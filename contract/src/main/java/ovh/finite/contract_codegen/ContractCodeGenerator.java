package ovh.finite.contract_codegen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ovh.finite.contract_ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractCodeGenerator implements Opcodes {
    private ClassWriter cw;
    private MethodVisitor mv;
    private Map<String, Integer> variables = new HashMap<>();
    private Map<String, Class<?>> varTypes = new HashMap<>();
    private Map<String, String> functionDescriptors = new HashMap<>();
    private String filePath;
    private int varIndex = 0;

    public ContractCodeGenerator(String filePath) {
        this.filePath = filePath;
    }

    private String getDescriptor(String type) {
        if (type == null) return "V";
        switch (type) {
            case "Int": return "I";
            case "Bool": return "Z";
            default: return "Ljava/lang/Object;"; // fallback
        }
    }

    private String getBoxedClass(String type) {
        switch (type) {
            case "Int": return "java/lang/Integer";
            case "Bool": return "java/lang/Boolean";
            default: return "java/lang/Object";
        }
    }

    private String getUnboxMethod(String type) {
        switch (type) {
            case "Int": return "intValue";
            case "Bool": return "booleanValue";
            default: return null;
        }
    }

    private String getUnboxDescriptor(String type) {
        switch (type) {
            case "Int": return "()I";
            case "Bool": return "()Z";
            default: return null;
        }
    }

    public byte[] generate(List<ContractStatement> statements) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC, "Main", null, "java/lang/Object", null);
        cw.visitSource(filePath, null);

        // Generate main method
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();

        varIndex = 1; // local 0 is args array
        variables.clear();

        for (ContractStatement stmt : statements) {
            generateStatement(stmt);
        }

        mv.visitInsn(RETURN);
        // With COMPUTE_FRAMES, provide reasonable upper bounds for stack and locals
        // Stack: assume max 10 for expressions, Locals: assume 50 for variables
        mv.visitMaxs(10, 50);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateStatement(ContractStatement stmt) {
        if (stmt instanceof ContractDecl) {
            generateContractDecl((ContractDecl) stmt);
        } else if (stmt instanceof FunctionDecl) {
            generateFunctionDecl((FunctionDecl) stmt);
        } else if (stmt instanceof VarDecl) {
            generateVarDecl((VarDecl) stmt);
        } else if (stmt instanceof IfStmt) {
            generateIfStmt((IfStmt) stmt);
        } else if (stmt instanceof WhileStmt) {
            generateWhileStmt((WhileStmt) stmt);
        } else if (stmt instanceof ExprStatement) {
            generateExpression(((ExprStatement) stmt).expression);
            // Don't pop, assume expressions in statements are void
        }
        // Others TODO
    }

    private void generateContractDecl(ContractDecl decl) {
        // For now, just generate the members
        for (ContractStatement member : decl.members) {
            generateStatement(member);
        }
    }

    private void generateFunctionDecl(FunctionDecl decl) {
        // Build descriptor
        StringBuilder desc = new StringBuilder("(");
        for (String type : decl.paramTypes) {
            desc.append(getDescriptor(type));
        }
        desc.append(")").append(getDescriptor(decl.returnType));

        functionDescriptors.put(decl.name, desc.toString());

        // Check for DllImport attribute
        Attribute dllImportAttr = decl.attributes.stream().filter(attr -> "DllImport".equals(attr.name)).findFirst().orElse(null);
        if (dllImportAttr != null) {
            // Generate JNA call
            String dllName = dllImportAttr.parameter != null ? dllImportAttr.parameter.replace("\"", "") : "unknown";
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, decl.name, desc.toString(), null, null);
            mv.visitCode();
            // com.sun.jna.NativeLibrary.getInstance(dllName).getFunction(decl.name).invoke(args);
            mv.visitLdcInsn(dllName);
            mv.visitMethodInsn(INVOKESTATIC, "com/sun/jna/NativeLibrary", "getInstance", "(Ljava/lang/String;)Lcom/sun/jna/NativeLibrary;", false);
            mv.visitLdcInsn(decl.name);
            mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/NativeLibrary", "getFunction", "(Ljava/lang/String;)Lcom/sun/jna/Function;", false);
            // Create Object[] args
            int paramCount = decl.paramTypes.size();
            mv.visitIntInsn(BIPUSH, paramCount);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            for (int i = 0; i < paramCount; i++) {
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i);
                String type = decl.paramTypes.get(i);
                if ("Int".equals(type)) {
                    mv.visitVarInsn(ILOAD, i);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                } else if ("Bool".equals(type)) {
                    mv.visitVarInsn(ILOAD, i);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                } else {
                    // Assume reference type
                    mv.visitVarInsn(ALOAD, i);
                }
                mv.visitInsn(AASTORE);
            }
            // Invoke the function
            if (decl.returnType == null) {
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invokeVoid", "([Ljava/lang/Object;)V", false);
                mv.visitInsn(RETURN);
            } else if ("Int".equals(decl.returnType)) {
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invokeInt", "([Ljava/lang/Object;)I", false);
                mv.visitInsn(IRETURN);
            } else if ("Bool".equals(decl.returnType)) {
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invokeInt", "([Ljava/lang/Object;)I", false);
                // Convert int to boolean (non-zero = true)
                Label falseLabel = new Label();
                mv.visitJumpInsn(IFEQ, falseLabel);
                mv.visitInsn(ICONST_1);
                Label endLabel = new Label();
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(falseLabel);
                mv.visitInsn(ICONST_0);
                mv.visitLabel(endLabel);
                mv.visitInsn(IRETURN);
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/jna/Function", "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
                mv.visitInsn(ARETURN);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } else {
            // Normal function
            int access = ACC_PUBLIC | ACC_STATIC;
            MethodVisitor funcMv = cw.visitMethod(access, decl.name, desc.toString(), null, null);
            funcMv.visitCode();

            // Set up variables
            Map<String, Integer> oldVariables = variables;
            variables = new HashMap<>();
            int localIndex = 0;
            for (int i = 0; i < decl.paramNames.size(); i++) {
                variables.put(decl.paramNames.get(i), localIndex);
                localIndex += 1; // assume 1 slot
            }
            varIndex = localIndex;

            for (ContractStatement stmt : decl.body) {
                generateStatement(stmt);
            }

            variables = oldVariables;

            if (decl.returnType != null) {
                // Assume return 0 or false
                if ("Int".equals(decl.returnType)) {
                    funcMv.visitInsn(ICONST_0);
                    funcMv.visitInsn(IRETURN);
                } else if ("Bool".equals(decl.returnType)) {
                    funcMv.visitInsn(ICONST_0);
                    funcMv.visitInsn(IRETURN);
                }
            } else {
                funcMv.visitInsn(RETURN);
            }
            funcMv.visitMaxs(0, 0);
            funcMv.visitEnd();
        }
    }

    private void generateVarDecl(VarDecl var) {
        if (var.initializer != null) {
            generateExpression(var.initializer);
        } else {
            mv.visitInsn(ICONST_0); // default 0
        }
        int index;
        if (variables.containsKey(var.name)) {
            index = variables.get(var.name);
        } else {
            index = varIndex++;
            variables.put(var.name, index);
        }
        // Assume int for now
        mv.visitVarInsn(ISTORE, index);
        varTypes.put(var.name, Integer.class);
    }

    private void generateIfStmt(IfStmt stmt) {
        Label elseLabel = new Label();
        Label endLabel = new Label();

        generateExpression(stmt.condition);
        mv.visitJumpInsn(IFEQ, elseLabel);

        for (ContractStatement s : stmt.thenBranch) {
            generateStatement(s);
        }
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(elseLabel);
        if (stmt.elseBranch != null) {
            for (ContractStatement s : stmt.elseBranch) {
                generateStatement(s);
            }
        }
        mv.visitLabel(endLabel);
    }

    private void generateWhileStmt(WhileStmt stmt) {
        Label loopLabel = new Label();
        Label endLabel = new Label();
        mv.visitLabel(loopLabel);
        generateExpression(stmt.condition);
        mv.visitJumpInsn(IFEQ, endLabel);
        for (ContractStatement bodyStmt : stmt.body) {
            generateStatement(bodyStmt);
        }
        mv.visitJumpInsn(GOTO, loopLabel);
        mv.visitLabel(endLabel);
    }

    private void generateExpression(ContractExpression expr) {
        if (expr instanceof Literal) {
            Literal lit = (Literal) expr;
            if (lit.value instanceof Integer) {
                int val = (Integer) lit.value;
                if (val >= -1 && val <= 5) {
                    mv.visitInsn(ICONST_0 + val);
                } else {
                    mv.visitLdcInsn(val);
                }
            } else if (lit.value instanceof Boolean) {
                mv.visitInsn(((Boolean) lit.value) ? ICONST_1 : ICONST_0);
            } else if (lit.value instanceof String) {
                mv.visitLdcInsn((String) lit.value);
            }
        } else if (expr instanceof Variable) {
            Variable var = (Variable) expr;
            Integer index = variables.get(var.name);
            if (index != null) {
                mv.visitVarInsn(ILOAD, index);
            }
        } else if (expr instanceof BinaryOp) {
            BinaryOp bin = (BinaryOp) expr;
            generateExpression(bin.left);
            generateExpression(bin.right);
            switch (bin.operator) {
                case "+": mv.visitInsn(IADD); break;
                case "-": mv.visitInsn(ISUB); break;
                case "*": mv.visitInsn(IMUL); break;
                case "/": mv.visitInsn(IDIV); break;
                case "==": {
                    Label trueLabel = new Label();
                    Label endLabel = new Label();
                    mv.visitJumpInsn(IF_ICMPEQ, trueLabel);
                    mv.visitInsn(ICONST_0);
                    mv.visitJumpInsn(GOTO, endLabel);
                    mv.visitLabel(trueLabel);
                    mv.visitInsn(ICONST_1);
                    mv.visitLabel(endLabel);
                    break;
                }
                case "!=": {
                    Label trueLabel = new Label();
                    Label endLabel = new Label();
                    mv.visitJumpInsn(IF_ICMPNE, trueLabel);
                    mv.visitInsn(ICONST_0);
                    mv.visitJumpInsn(GOTO, endLabel);
                    mv.visitLabel(trueLabel);
                    mv.visitInsn(ICONST_1);
                    mv.visitLabel(endLabel);
                    break;
                }
                case "<": {
                    Label trueLabel = new Label();
                    Label endLabel = new Label();
                    mv.visitJumpInsn(IF_ICMPLT, trueLabel);
                    mv.visitInsn(ICONST_0);
                    mv.visitJumpInsn(GOTO, endLabel);
                    mv.visitLabel(trueLabel);
                    mv.visitInsn(ICONST_1);
                    mv.visitLabel(endLabel);
                    break;
                }
                // Add more as needed
            }
        } else if (expr instanceof Unary) {
            Unary un = (Unary) expr;
            generateExpression(un.operand);
            if ("!".equals(un.operator)) {
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IXOR);
            } else if ("-".equals(un.operator)) {
                mv.visitInsn(INEG);
            }
        } else if (expr instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expr;
            // For now, assume no args, and ()V or ()I etc.
            // But to make correct, need to know the return type.
            // For simplicity, assume all return int/bool
            for (ContractExpression arg : call.arguments) {
                generateExpression(arg);
            }
            // Assume ()I for now, but need to get descriptor
            // For dllimport, the method is generated with correct desc, so INVOKESTATIC "Main", call.name, "()I" or whatever
            // But to know, perhaps store the descriptors.
            // For now, hardcode for known
            String desc = functionDescriptors.get(call.name);
            if (desc == null) desc = "()V";
            mv.visitMethodInsn(INVOKESTATIC, "Main", call.name, desc, false);
            // If void, but since expression, perhaps push 0 if needed, but for now assume callers handle
        }
    }
}

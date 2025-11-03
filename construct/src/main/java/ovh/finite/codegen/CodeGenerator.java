package ovh.finite.codegen;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ovh.finite.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator implements Opcodes {
    private ClassWriter cw;
    private MethodVisitor mv;
    private Map<String, Integer> variables = new HashMap<>();
    private Map<String, Class<?>> varTypes = new HashMap<>();
    private int varIndex = 0;

    public byte[] generate(List<Statement> statements) {
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC, "Main", null, "java/lang/Object", null);

        // Generate main method
        mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();

        varIndex = 1; // local 0 is args array
        variables.clear();

        for (Statement stmt : statements) {
            generateStatement(stmt);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0); // COMPUTE_FRAMES
        mv.visitEnd();

        // Generate functions as static methods
        for (Statement stmt : statements) {
            if (stmt instanceof FunctionDefinition) {
                generateFunction((FunctionDefinition) stmt);
            }
        }

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateStatement(Statement stmt) {
        if (stmt instanceof LetStatement) {
            generateLet((LetStatement) stmt);
        } else if (stmt instanceof WhileStatement) {
            generateWhile((WhileStatement) stmt);
        } else if (stmt instanceof ForStatement) {
            generateFor((ForStatement) stmt);
        } else if (stmt instanceof ExpressionStatement) {
            generateExpression(((ExpressionStatement) stmt).expression);
            // Pop result if not used, but not for void calls like dump
            Expression expr = ((ExpressionStatement) stmt).expression;
            if (!(expr instanceof FunctionCall && ((FunctionCall) expr).function instanceof Variable && ((Variable) ((FunctionCall) expr).function).name.equals("dump"))) {
                mv.visitInsn(POP);
            }
        }
        // Others TODO
    }

    private void generateWhile(WhileStatement stmt) {
        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();

        mv.visitLabel(start);
        generateExpression(stmt.condition);
        mv.visitJumpInsn(IFEQ, end); // if condition == 0, jump to end

        for (Statement s : stmt.body) {
            generateStatement(s);
        }

        mv.visitJumpInsn(GOTO, start);
        mv.visitLabel(end);
    }

    private void generateLet(LetStatement stmt) {
        generateExpression(stmt.value);
        int index;
        if (variables.containsKey(stmt.name)) {
            index = variables.get(stmt.name);
        } else {
            index = varIndex++;
            variables.put(stmt.name, index);
        }
        // Determine type
        Class<?> type = Integer.class; // default
        if (stmt.value instanceof Literal && ((Literal) stmt.value).value instanceof String) {
            type = String.class;
            mv.visitVarInsn(ASTORE, index);
        } else if (stmt.value instanceof MatchExpression) {
            type = String.class; // Assume for now
            mv.visitVarInsn(ASTORE, index);
        } else {
            mv.visitVarInsn(ISTORE, index);
        }
        varTypes.put(stmt.name, type);
    }

    private void generateFunction(FunctionDefinition func) {
        // Assume (II)I for now
        MethodVisitor funcMv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, func.name, "(II)I", null, null);
        funcMv.visitCode();

        // Set up variables for params
        Map<String, Integer> funcVariables = new HashMap<>();
        int localIndex = 0;
        for (String param : func.parameters) {
            funcVariables.put(param, localIndex++);
        }

        // Temporarily set variables to funcVariables and mv to funcMv
        Map<String, Integer> oldVariables = variables;
        MethodVisitor oldMv = mv;
        variables = funcVariables;
        mv = funcMv;
        varIndex = localIndex;

        generateExpression(func.body);

        variables = oldVariables;
        mv = oldMv;

        funcMv.visitInsn(IRETURN);
        funcMv.visitMaxs(0, 0); // COMPUTE_FRAMES
        funcMv.visitEnd();
    }

    private void generateExpression(Expression expr) {
        if (expr instanceof Literal) {
            Object value = ((Literal) expr).value;
            if (value instanceof Integer) {
                mv.visitLdcInsn(value);
            } else if (value instanceof String) {
                mv.visitLdcInsn(value);
            } // etc.
        } else if (expr instanceof Variable) {
            String name = ((Variable) expr).name;
            Integer index = variables.get(name);
            if (index != null) {
                Class<?> type = varTypes.get(name);
                if (type == String.class) {
                    mv.visitVarInsn(ALOAD, index);
                } else {
                    mv.visitVarInsn(ILOAD, index);
                }
            }
        } else if (expr instanceof BinaryOp) {
            generateBinaryOp((BinaryOp) expr);
        } else if (expr instanceof FunctionCall) {
            generateFunctionCall((FunctionCall) expr);
        } else if (expr instanceof MatchExpression) {
            generateMatch((MatchExpression) expr);
        } else if (expr instanceof ListLiteral) {
            generateList((ListLiteral) expr);
        }
        // TODO: others
    }

    private void generateBinaryOp(BinaryOp op) {
        generateExpression(op.left);
        generateExpression(op.right);
        switch (op.operator) {
            case "+": mv.visitInsn(IADD); break;
            case "-": mv.visitInsn(ISUB); break;
            case "*": mv.visitInsn(IMUL); break;
            case "/": mv.visitInsn(IDIV); break;
            case "==": {
                org.objectweb.asm.Label trueLabel = new org.objectweb.asm.Label();
                org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
                mv.visitJumpInsn(IF_ICMPEQ, trueLabel);
                mv.visitInsn(ICONST_0);
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(trueLabel);
                mv.visitInsn(ICONST_1);
                mv.visitLabel(endLabel);
                break;
            }
            case "!=": {
                org.objectweb.asm.Label trueLabel = new org.objectweb.asm.Label();
                org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
                mv.visitJumpInsn(IF_ICMPNE, trueLabel);
                mv.visitInsn(ICONST_0);
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(trueLabel);
                mv.visitInsn(ICONST_1);
                mv.visitLabel(endLabel);
                break;
            }
            case ">": {
                org.objectweb.asm.Label trueLabel = new org.objectweb.asm.Label();
                org.objectweb.asm.Label endLabel = new org.objectweb.asm.Label();
                mv.visitJumpInsn(IF_ICMPGT, trueLabel);
                mv.visitInsn(ICONST_0);
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(trueLabel);
                mv.visitInsn(ICONST_1);
                mv.visitLabel(endLabel);
                break;
            }
        }
    }

    private void generateFunctionCall(FunctionCall call) {
        for (Expression arg : call.arguments) {
            generateExpression(arg);
        }
        if (call.function instanceof Variable) {
            String name = ((Variable) call.function).name;
            if (name.equals("dump")) {
                // If the argument is a string literal or string variable, don't box, else box to Integer
                boolean isString = false;
                Expression arg = call.arguments.get(0);
                if (arg instanceof Literal && ((Literal) arg).value instanceof String) {
                    isString = true;
                } else if (arg instanceof Variable) {
                    String varName = ((Variable) arg).name;
                    isString = varTypes.get(varName) == String.class;
                }
                if (!isString) {
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                }
                // For println we need: getstatic java/lang/System out Ljava/io/PrintStream; <push value> invokevirtual println(Ljava/lang/Object;)V
                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
            } else {
                mv.visitMethodInsn(INVOKESTATIC, "Main", name, "(II)I", false); // Assume
            }
        }
    }

    private void generateMatch(MatchExpression match) {
        // Generate value once
        generateExpression(match.value);
        int valueIndex = varIndex++;
        mv.visitVarInsn(ISTORE, valueIndex); // Assume int

        Label endLabel = new Label();
        for (int i = 0; i < match.cases.size(); i++) {
            MatchExpression.MatchCase case_ = match.cases.get(i);
            if (isWildcard(case_.pattern)) {
                // Else case
                generateExpression(case_.body);
                mv.visitJumpInsn(GOTO, endLabel);
            } else {
                Label nextLabel = new Label();
                mv.visitVarInsn(ILOAD, valueIndex);
                generateExpression(case_.pattern);
                mv.visitJumpInsn(IF_ICMPNE, nextLabel);
                // Match
                generateExpression(case_.body);
                mv.visitJumpInsn(GOTO, endLabel);
                mv.visitLabel(nextLabel);
            }
        }
        // If no match and no wildcard, undefined, for now assume has wildcard
        mv.visitLabel(endLabel);
    }

    private void generateList(ListLiteral list) {
        int size = list.elements.size();
        mv.visitLdcInsn(size);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        for (int i = 0; i < size; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            generateExpression(list.elements.get(i));
            // Box if int
            if (list.elements.get(i) instanceof Literal && ((Literal) list.elements.get(i)).value instanceof Integer) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (list.elements.get(i) instanceof Variable) {
                String varName = ((Variable) list.elements.get(i)).name;
                if (varTypes.get(varName) == Integer.class) {
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                }
            }
            mv.visitInsn(AASTORE);
        }
    }

    private void generateFor(ForStatement forStmt) {
        // Assume iterable is ListLiteral
        generateExpression(forStmt.iterable);
        int arrayIndex = varIndex++;
        mv.visitVarInsn(ASTORE, arrayIndex);

        int indexIndex = varIndex++;
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, indexIndex);

        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitLabel(loopStart);

        // Load index, load length
        mv.visitVarInsn(ILOAD, indexIndex);
        mv.visitVarInsn(ALOAD, arrayIndex);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);

        // Load array[index] into variable
        mv.visitVarInsn(ALOAD, arrayIndex);
        mv.visitVarInsn(ILOAD, indexIndex);
        mv.visitInsn(AALOAD);
        // Assume int, unbox
        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);

        Integer varIdx = variables.get(forStmt.variable);
        if (varIdx == null) {
            varIdx = this.varIndex++;
            variables.put(forStmt.variable, varIdx);
        }
        mv.visitVarInsn(ISTORE, varIdx);
        varTypes.put(forStmt.variable, Integer.class);

        // Generate body
        for (Statement stmt : forStmt.body) {
            generateStatement(stmt);
        }

        // index++
        mv.visitIincInsn(indexIndex, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        mv.visitLabel(loopEnd);
    }

    private boolean isWildcard(Expression pattern) {
        return pattern instanceof Variable && ((Variable) pattern).name.equals("_");
    }
}
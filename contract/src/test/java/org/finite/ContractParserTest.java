package org.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.DiagnosticReporter;
import ovh.finite.contract_ast.*;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;

public class ContractParserTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.contract");
    }

    private List<ContractStatement> parse(String source) {
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();
        ContractParser parser = new ContractParser(tokens, reporter);
        return parser.parse();
    }

    @Test
    public void testParseSimpleFunction() {
        String source = "fn myFunc(x: Int) -> Int {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof FunctionDecl);
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("myFunc", func.name);
        assertEquals(1, func.paramTypes.size());
        assertEquals("Int", func.paramTypes.get(0));
        assertEquals("Int", func.returnType);
    }

    @Test
    public void testParseFunctionWithMultipleParams() {
        String source = "fn add(a: Int, b: Int) -> Int {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("add", func.name);
        assertEquals(2, func.paramTypes.size());
        assertEquals("Int", func.paramTypes.get(0));
        assertEquals("Int", func.paramTypes.get(1));
        assertEquals("Int", func.returnType);
    }

    @Test
    public void testParseFunctionWithoutReturn() {
        String source = "fn doNothing() {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("doNothing", func.name);
        assertEquals(0, func.paramTypes.size());
        assertNull("Return type should be null", func.returnType);
    }

    @Test
    public void testParseDllImportAttribute() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("InitWindow", func.name);
        assertEquals(1, func.attributes.size());
        Attribute attr = func.attributes.get(0);
        assertEquals("DllImport", attr.name);
    }

    @Test
    public void testParseMultipleFunctions() {
        String source = "fn first() {}\n" +
                "fn second(x: Int) {}\n" +
                "fn third() -> Bool {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(3, stmts.size());
        assertEquals("first", ((FunctionDecl) stmts.get(0)).name);
        assertEquals("second", ((FunctionDecl) stmts.get(1)).name);
        assertEquals("third", ((FunctionDecl) stmts.get(2)).name);
    }

    @Test
    public void testParseContractProgram() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof ContractDecl);
        ContractDecl contract = (ContractDecl) stmts.get(0);
        assertEquals("Program", contract.name);
        assertEquals(1, contract.members.size());
        assertTrue(contract.members.get(0) instanceof FunctionDecl);
    }

    @Test
    public void testParseVariableDeclaration() {
        String source = "fn test() {\n" +
                "  var x = 42\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        assertTrue(func.body.get(0) instanceof VarDecl);
        VarDecl var = (VarDecl) func.body.get(0);
        assertEquals("x", var.name);
    }

    @Test
    public void testParseWhileLoop() {
        String source = "fn test() {\n" +
                "  while (true) {\n" +
                "    var x = 1\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        assertTrue(func.body.get(0) instanceof WhileStmt);
    }

    @Test
    public void testParseIfStatement() {
        String source = "fn test() {\n" +
                "  if (x > 0) {\n" +
                "    var y = 1\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        assertTrue(func.body.get(0) instanceof IfStmt);
    }

    @Test
    public void testParseExpressionStatement() {
        String source = "fn test() {\n" +
                "  myFunc(42)\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        assertTrue(func.body.get(0) instanceof ExprStatement);
    }

    @Test
    public void testParseStringType() {
        String source = "fn getName() -> String {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("String", func.returnType);
    }

    @Test
    public void testParseBoolType() {
        String source = "fn isTrue() -> Bool {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("Bool", func.returnType);
    }

    @Test
    public void testParseFloatType() {
        String source = "fn getRadius() -> Float {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals("Float", func.returnType);
    }

    @Test
    public void testParseWithBlankLines() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "// This is a comment\n" +
                "fn myFunc() {}";
        List<ContractStatement> stmts = parse(source);

        // Should parse all three functions despite blank lines and comments
        assertEquals(3, stmts.size());
        assertEquals("InitWindow", ((FunctionDecl) stmts.get(0)).name);
        assertEquals("CloseWindow", ((FunctionDecl) stmts.get(1)).name);
        assertEquals("myFunc", ((FunctionDecl) stmts.get(2)).name);
    }

    @Test
    public void testParseWithComments() {
        String source = "fn test() {\n" +
                "    // Single line comment\n" +
                "    var x = 42\n" +
                "    /* Multi-line\n" +
                "       comment */\n" +
                "    var y = 100\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        // Should have parsed both variable declarations despite comments
        assertEquals(2, func.body.size());
    }

    @Test
    public void testParseCompleteContract() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    var x = 42\n" +
                "    InitWindow(800, 600, \"Test\")\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(3, stmts.size());
        assertEquals("InitWindow", ((FunctionDecl) stmts.get(0)).name);
        assertEquals("CloseWindow", ((FunctionDecl) stmts.get(1)).name);
        assertTrue(stmts.get(2) instanceof ContractDecl);
    }

    @Test
    public void testParseMultipleAttributes() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "[Deprecated]\n" +
                "fn oldFunc() {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(2, func.attributes.size());
    }

    @Test
    public void testParseNestedBlocks() {
        String source = "fn test() {\n" +
                "  if (x > 0) {\n" +
                "    while (true) {\n" +
                "      var y = 1\n" +
                "    }\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        // Should parse without errors
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testParseStringLiteral() {
        String source = "fn test() {\n" +
                "  var s = \"Hello, world!\"\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        FunctionDecl func = (FunctionDecl) stmts.get(0);
        assertEquals(1, func.body.size());
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testParseFloatLiteral() {
        String source = "fn drawCircle(radius: Float) {\n" +
                "  DrawCircle(400, 300, 50.0, WHITE)\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testParseHexLiteral() {
        String source = "fn test() {\n" +
                "  var color = 0xFFFFFF\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testParseUnaryExpression() {
        String source = "fn test() {\n" +
                "  if (!done) {\n" +
                "    var x = 1\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testParseBinaryExpression() {
        String source = "fn test() {\n" +
                "  var result = x + y\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertFalse("Should not have errors", reporter.hasErrors());
    }
}

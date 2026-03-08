package ovh.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.ast.Statement;
import ovh.finite.codegen.CodeGenerator;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;

public class CodeGeneratorTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.construct");
    }

    private byte[] compile(String source) {
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens, reporter);
        List<Statement> stmts = parser.parse();
        CodeGenerator generator = new CodeGenerator();
        return generator.generate(stmts);
    }

    @Test
    public void testGenerateEmptyProgram() {
        byte[] bytecode = compile("");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateIntLetStatement() {
        byte[] bytecode = compile("let x = 42");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateStringLetStatement() {
        byte[] bytecode = compile("let msg = \"hello\"");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateArithmetic() {
        byte[] bytecode = compile("let result = 3 + 4");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateSubtraction() {
        byte[] bytecode = compile("let result = 10 - 3");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateMultiplication() {
        byte[] bytecode = compile("let result = 4 * 5");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateFunctionDefinition() {
        byte[] bytecode = compile("fn add(x, y) = x + y");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateMultipleLetStatements() {
        String source = "let x = 1\nlet y = 2\nlet z = 3";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateMatchExpression() {
        String source = "let x = 1\nlet result = match x:\n| 1 -> 10\n| _ -> 0";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateListLiteral() {
        byte[] bytecode = compile("let nums = [1, 2, 3]");
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateForStatement() {
        String source = "for i in [1, 2, 3] do\nend";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateComplexProgram() {
        String source =
            "fn add(x, y) = x + y\n" +
            "let a = 5\n" +
            "let b = add(a, a)";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testGeneratedBytecodeStartsWithCafeBabe() {
        byte[] bytecode = compile("let x = 1");
        assertNotNull(bytecode);
        assertTrue("Bytecode should be at least 4 bytes", bytecode.length >= 4);
        // Java class file magic number: 0xCAFEBABE
        assertEquals("Magic byte 0", (byte) 0xCA, bytecode[0]);
        assertEquals("Magic byte 1", (byte) 0xFE, bytecode[1]);
        assertEquals("Magic byte 2", (byte) 0xBA, bytecode[2]);
        assertEquals("Magic byte 3", (byte) 0xBE, bytecode[3]);
    }
}

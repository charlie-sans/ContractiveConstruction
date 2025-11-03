package org.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.DiagnosticReporter;
import ovh.finite.contract_ast.*;
import ovh.finite.contract_codegen.ContractCodeGenerator;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;

public class ContractCodeGeneratorTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.contract");
    }

    private byte[] compile(String source) {
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();
        ContractParser parser = new ContractParser(tokens, reporter);
        List<ContractStatement> stmts = parser.parse();
        ContractCodeGenerator generator = new ContractCodeGenerator("test.contract");
        return generator.generate(stmts);
    }

    @Test
    public void testGenerateSimpleContract() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "    var x = 42\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
    }

    @Test
    public void testGenerateDllImportFunction() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateMultipleDllImportFunctions() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn DrawCircle(centerX: Int, centerY: Int, radius: Float, color: Int) {}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateContractWithDllImportCalls() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    InitWindow(800, 600, \"Hello\")\n" +
                "    CloseWindow()\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateStringParameter() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn DrawText(text: String, x: Int, y: Int, fontSize: Int, color: Int) {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    DrawText(\"Hello\", 100, 200, 20, 0xFFFFFF)\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateFloatParameter() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn DrawCircle(centerX: Int, centerY: Int, radius: Float, color: Int) {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    DrawCircle(400, 300, 50.0, 0xFFFFFF)\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateBoolReturnType() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn WindowShouldClose() -> Bool {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    while (!WindowShouldClose()) {\n" +
                "      var x = 1\n" +
                "    }\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateVariableDeclaration() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "    var x = 42\n" +
                "    var y = 100\n" +
                "    var z = 0xFFFFFF\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateWhileLoop() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "    var i = 0\n" +
                "    while (i < 10) {\n" +
                "      i = i + 1\n" +
                "    }\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateIfStatement() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "    var x = 42\n" +
                "    if (x > 0) {\n" +
                "      var y = 1\n" +
                "    }\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateBinaryOperations() {
        String source = "Contract Program {\n" +
                "  fn main() {\n" +
                "    var a = 10\n" +
                "    var b = 20\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
    }

    @Test
    public void testGenerateComplexContract() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn WindowShouldClose() -> Bool {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn BeginDrawing() {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn EndDrawing() {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn ClearBackground(color: Int) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn DrawCircle(centerX: Int, centerY: Int, radius: Float, color: Int) {}\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn DrawText(text: String, x: Int, y: Int, fontSize: Int, color: Int) {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    var RAYWHITE = 0xFFFFFF\n" +
                "    var BLACK = 0x000000\n" +
                "    InitWindow(800, 600, \"Hello, world!\")\n" +
                "    while (!WindowShouldClose()) {\n" +
                "      BeginDrawing()\n" +
                "      ClearBackground(BLACK)\n" +
                "      DrawCircle(400, 300, 50.0, RAYWHITE)\n" +
                "      DrawText(\"Hello, world!\", 350, 280, 20, RAYWHITE)\n" +
                "      EndDrawing()\n" +
                "    }\n" +
                "    CloseWindow()\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateWithBlankLines() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    InitWindow(800, 600, \"Test\")\n" +
                "    CloseWindow()\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }

    @Test
    public void testGenerateWithComments() {
        String source = "// Initialize raylib window\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "/* Close the window when done */\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    // Open a window\n" +
                "    InitWindow(800, 600, \"Test\")\n" +
                "    // Close it\n" +
                "    CloseWindow()\n" +
                "  }\n" +
                "}";
        byte[] bytecode = compile(source);
        assertNotNull("Should generate bytecode", bytecode);
        assertTrue("Bytecode should not be empty", bytecode.length > 0);
        assertFalse("Should not have errors", reporter.hasErrors());
    }
}

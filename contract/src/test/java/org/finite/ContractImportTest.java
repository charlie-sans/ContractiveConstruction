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

public class ContractImportTest {
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
    public void testParseImportStatement() {
        String source = "import [\"utils.ct\"]";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        ImportStatement importStmt = (ImportStatement) stmts.get(0);
        assertEquals("utils.ct", importStmt.filePath);
    }

    @Test
    public void testParseMultipleImports() {
        String source = "import [\"utils.ct\"]\n" +
                "import [\"helpers.ct\"]";
        List<ContractStatement> stmts = parse(source);

        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof ImportStatement);
        assertEquals("utils.ct", ((ImportStatement) stmts.get(0)).filePath);
        assertEquals("helpers.ct", ((ImportStatement) stmts.get(1)).filePath);
    }

    @Test
    public void testParseImportWithNestedPath() {
        String source = "import [\"lib/utils.ct\"]";
        List<ContractStatement> stmts = parse(source);

        assertEquals(1, stmts.size());
        ImportStatement importStmt = (ImportStatement) stmts.get(0);
        assertEquals("lib/utils.ct", importStmt.filePath);
    }

    @Test
    public void testParseImportBeforeFunctions() {
        String source = "import [\"utils.ct\"]\n" +
                "\n" +
                "fn myFunc() {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof FunctionDecl);
    }

    @Test
    public void testParseImportBeforeContract() {
        String source = "import [\"utils.ct\"]\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {}\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof ContractDecl);
    }

    @Test
    public void testParseImportWithComments() {
        String source = "// Import utilities\n" +
                "import [\"utils.ct\"]\n" +
                "\n" +
                "// Main functions\n" +
                "fn test() {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof FunctionDecl);
    }

    @Test
    public void testParseImportWithBlankLines() {
        String source = "import [\"utils.ct\"]\n" +
                "\n" +
                "\n" +
                "import [\"helpers.ct\"]\n" +
                "\n" +
                "[DllImport(\"lib.so\")]\n" +
                "fn test() {}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(3, stmts.size());
        assertEquals("utils.ct", ((ImportStatement) stmts.get(0)).filePath);
        assertEquals("helpers.ct", ((ImportStatement) stmts.get(1)).filePath);
        assertTrue(stmts.get(2) instanceof FunctionDecl);
    }

    @Test
    public void testParseComplexImportScenario() {
        String source = "import [\"lib/drawing.ct\"]\n" +
                "import [\"lib/colors.ct\"]\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "Contract Program {\n" +
                "  fn main() {\n" +
                "    var x = 42\n" +
                "  }\n" +
                "}";
        List<ContractStatement> stmts = parse(source);

        assertEquals(4, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof ImportStatement);
        assertTrue(stmts.get(2) instanceof FunctionDecl);
        assertTrue(stmts.get(3) instanceof ContractDecl);
        assertFalse("Should not have errors", reporter.hasErrors());
    }
}

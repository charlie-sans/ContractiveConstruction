package org.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import ovh.finite.DiagnosticReporter;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;
import ovh.finite.ast.Statement;
import ovh.finite.ast.ImportStatement;

public class ConstructImportTest {
    @Test
    public void testParseImportList() {
        DiagnosticReporter reporter = new DiagnosticReporter("", "test.construct");
        String source = "import [\"a.construct\", \"b.construct\"]";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens, reporter);
        List<Statement> stmts = parser.parse();

        assertEquals(2, stmts.size());
        assertTrue(stmts.get(0) instanceof ImportStatement);
        assertTrue(stmts.get(1) instanceof ImportStatement);
        assertEquals("a.construct", ((ImportStatement) stmts.get(0)).filePath);
        assertEquals("b.construct", ((ImportStatement) stmts.get(1)).filePath);
    }
}

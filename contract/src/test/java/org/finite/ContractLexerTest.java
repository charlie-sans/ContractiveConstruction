package org.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.DiagnosticReporter;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_lexer.ContractTokenType;

public class ContractLexerTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.contract");
    }

    @Test
    public void testLexerBasicKeywords() {
        String source = "Contract Program { fn main() {} }";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertTrue(tokens.size() > 0);
        assertEquals(ContractTokenType.CONTRACT, tokens.get(0).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals(ContractTokenType.LBRACE, tokens.get(2).type);
        assertEquals(ContractTokenType.FN, tokens.get(3).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(4).type);
        assertEquals(ContractTokenType.LPAREN, tokens.get(5).type);
        assertEquals(ContractTokenType.RPAREN, tokens.get(6).type);
        assertEquals(ContractTokenType.LBRACE, tokens.get(7).type);
        assertEquals(ContractTokenType.RBRACE, tokens.get(8).type);
        assertEquals(ContractTokenType.RBRACE, tokens.get(9).type);
        assertEquals(ContractTokenType.EOF, tokens.get(10).type);
    }

    @Test
    public void testLexerDllImportAttribute() {
        String source = "[DllImport(\"libraylib.so\")] fn DrawCircle(x: Int) {}";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.LBRACKET, tokens.get(0).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("DllImport", tokens.get(1).lexeme);
        assertEquals(ContractTokenType.LPAREN, tokens.get(2).type);
        assertEquals(ContractTokenType.STRING, tokens.get(3).type);
        assertEquals(ContractTokenType.RPAREN, tokens.get(4).type);
        assertEquals(ContractTokenType.RBRACKET, tokens.get(5).type);
    }

    @Test
    public void testLexerStringLiteral() {
        String source = "\"Hello, world!\"";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.STRING, tokens.get(0).type);
        assertEquals("Hello, world!", tokens.get(0).literal);
    }

    @Test
    public void testLexerIntegerLiteral() {
        String source = "42 0xFF";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.INT, tokens.get(0).type);
        assertEquals(42, tokens.get(0).literal);

        assertEquals(ContractTokenType.INT, tokens.get(1).type);
        assertEquals(0xFF, tokens.get(1).literal);
    }

    @Test
    public void testLexerFloatLiteral() {
        String source = "3.14 50.0 0.1";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.FLOAT, tokens.get(0).type);
        assertEquals(3.14, (double) tokens.get(0).literal, 0.001);

        assertEquals(ContractTokenType.FLOAT, tokens.get(1).type);
        assertEquals(50.0, (double) tokens.get(1).literal, 0.001);

        assertEquals(ContractTokenType.FLOAT, tokens.get(2).type);
        assertEquals(0.1, (double) tokens.get(2).literal, 0.001);
    }

    @Test
    public void testLexerSingleLineComments() {
        String source = "fn main() {\n" +
                "    // This is a comment\n" +
                "    var x = 42\n" +
                "    // Another comment\n" +
                "}";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        // Comments should be skipped, so no comment tokens
        boolean hasComment = tokens.stream().anyMatch(t -> t.type == ContractTokenType.SLASH);
        assertFalse("Comments should be skipped", hasComment);

        // Should have fn, main, etc.
        assertEquals(ContractTokenType.FN, tokens.get(0).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("main", tokens.get(1).lexeme);
    }

    @Test
    public void testLexerMultiLineComments() {
        String source = "fn main() {\n" +
                "    /* This is a\n" +
                "       multi-line comment */\n" +
                "    var x = 42\n" +
                "}";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        // Multi-line comments should be skipped
        boolean hasComment = tokens.stream().anyMatch(t -> t.type == ContractTokenType.SLASH);
        assertFalse("Multi-line comments should be skipped", hasComment);

        // Should have fn, main, etc.
        assertEquals(ContractTokenType.FN, tokens.get(0).type);
    }

    @Test
    public void testLexerBlankLines() {
        String source = "[DllImport(\"libraylib.so\")]\n" +
                "fn InitWindow(width: Int, height: Int, title: String) {}\n" +
                "\n" +
                "\n" +
                "[DllImport(\"libraylib.so\")]\n" +
                "fn CloseWindow() {}";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertTrue("Should have tokens", tokens.size() > 0);
        // First function
        assertEquals(ContractTokenType.LBRACKET, tokens.get(0).type);
        // Blank lines should be handled (they don't produce tokens)
        // Second function should be parseable
        boolean hasTwoFunctions = false;
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).type == ContractTokenType.RBRACE &&
                tokens.get(i + 1).type == ContractTokenType.LBRACKET) {
                hasTwoFunctions = true;
                break;
            }
        }
        assertTrue("Should have two functions despite blank lines", hasTwoFunctions);
    }

    @Test
    public void testLexerOperators() {
        String source = "+ - * / = == != < > ! ( ) [ ] { } , . : ;";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        ContractTokenType[] expected = {
            ContractTokenType.PLUS,
            ContractTokenType.MINUS,
            ContractTokenType.STAR,
            ContractTokenType.SLASH,
            ContractTokenType.EQUAL,
            ContractTokenType.EQUAL_EQUAL,
            ContractTokenType.BANG_EQUAL,
            ContractTokenType.LESS,
            ContractTokenType.GREATER,
            ContractTokenType.BANG,
            ContractTokenType.LPAREN,
            ContractTokenType.RPAREN,
            ContractTokenType.LBRACKET,
            ContractTokenType.RBRACKET,
            ContractTokenType.LBRACE,
            ContractTokenType.RBRACE,
            ContractTokenType.COMMA,
            ContractTokenType.DOT,
            ContractTokenType.COLON,
            ContractTokenType.SEMICOLON,
            ContractTokenType.EOF
        };

        for (int i = 0; i < expected.length; i++) {
            assertEquals("Token at index " + i, expected[i], tokens.get(i).type);
        }
    }

    @Test
    public void testLexerVariableDeclaration() {
        String source = "var x = 42";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.VAR, tokens.get(0).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("x", tokens.get(1).lexeme);
        assertEquals(ContractTokenType.EQUAL, tokens.get(2).type);
        assertEquals(ContractTokenType.INT, tokens.get(3).type);
    }

    @Test
    public void testLexerWhileLoop() {
        String source = "while (!done) { x = x + 1 }";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        assertEquals(ContractTokenType.WHILE, tokens.get(0).type);
        assertEquals(ContractTokenType.LPAREN, tokens.get(1).type);
        assertEquals(ContractTokenType.BANG, tokens.get(2).type);
        assertEquals(ContractTokenType.IDENTIFIER, tokens.get(3).type);
        assertEquals("done", tokens.get(3).lexeme);
    }

    @Test
    public void testLexerLineTracking() {
        String source = "fn main() {\n" +
                "    var x = 42\n" +
                "    // comment on line 3\n" +
                "    var y = 100\n" +
                "}";
        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();

        // Find y token and check it's on line 4
        ContractToken yToken = tokens.stream()
            .filter(t -> "y".equals(t.lexeme))
            .findFirst()
            .orElse(null);
        assertNotNull("Should find y token", yToken);
        assertEquals("y should be on line 4", 4, yToken.line);
    }
}

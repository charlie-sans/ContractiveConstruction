package ovh.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.lexer.TokenType;

public class LexerTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.construct");
    }

    @Test
    public void testLexerBasicKeywords() {
        String source = "let fn type import if then else match for in do end while";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        TokenType[] expected = {
            TokenType.LET, TokenType.FN, TokenType.TYPE, TokenType.IMPORT,
            TokenType.IF, TokenType.THEN, TokenType.ELSE, TokenType.MATCH,
            TokenType.FOR, TokenType.IN, TokenType.DO, TokenType.END,
            TokenType.WHILE, TokenType.EOF
        };

        assertEquals(expected.length, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Token at index " + i, expected[i], tokens.get(i).type);
        }
    }

    @Test
    public void testLexerBooleanLiterals() {
        String source = "true false";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.TRUE, tokens.get(0).type);
        assertEquals(TokenType.FALSE, tokens.get(1).type);
        assertEquals(TokenType.EOF, tokens.get(2).type);
    }

    @Test
    public void testLexerIntLiteral() {
        String source = "42";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.INT, tokens.get(0).type);
        assertEquals(42, tokens.get(0).literal);
    }

    @Test
    public void testLexerFloatLiteral() {
        String source = "3.14 50.0";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.FLOAT, tokens.get(0).type);
        assertEquals(3.14, (double) tokens.get(0).literal, 0.001);

        assertEquals(TokenType.FLOAT, tokens.get(1).type);
        assertEquals(50.0, (double) tokens.get(1).literal, 0.001);
    }

    @Test
    public void testLexerStringLiteral() {
        String source = "\"hello world\"";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.STRING, tokens.get(0).type);
        assertEquals("hello world", tokens.get(0).literal);
    }

    @Test
    public void testLexerIdentifier() {
        String source = "myVar someFunction x";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.IDENTIFIER, tokens.get(0).type);
        assertEquals("myVar", tokens.get(0).lexeme);

        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("someFunction", tokens.get(1).lexeme);

        assertEquals(TokenType.IDENTIFIER, tokens.get(2).type);
        assertEquals("x", tokens.get(2).lexeme);
    }

    @Test
    public void testLexerOperators() {
        String source = "+ - * / % = == != < <= > >= -> | _";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        TokenType[] expected = {
            TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
            TokenType.PERCENT, TokenType.EQUAL, TokenType.EQUAL_EQUAL,
            TokenType.BANG_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL,
            TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.ARROW,
            TokenType.PIPE, TokenType.UNDERSCORE, TokenType.EOF
        };

        assertEquals(expected.length, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Token at index " + i, expected[i], tokens.get(i).type);
        }
    }

    @Test
    public void testLexerDelimiters() {
        String source = "( ) { } [ ] , . :";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        TokenType[] expected = {
            TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
            TokenType.LBRACKET, TokenType.RBRACKET, TokenType.COMMA,
            TokenType.DOT, TokenType.COLON, TokenType.EOF
        };

        assertEquals(expected.length, tokens.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Token at index " + i, expected[i], tokens.get(i).type);
        }
    }

    @Test
    public void testLexerHashComment() {
        String source = "let x = 42 # this is a comment\nlet y = 10";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        // Comments should be skipped
        assertEquals(TokenType.LET, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("x", tokens.get(1).lexeme);
        assertEquals(TokenType.EQUAL, tokens.get(2).type);
        assertEquals(TokenType.INT, tokens.get(3).type);
        // After comment is skipped, next meaningful token is 'let'
        assertEquals(TokenType.LET, tokens.get(4).type);
        assertFalse("Comments should not produce tokens", reporter.hasErrors());
    }

    @Test
    public void testLexerLineTracking() {
        String source = "let x = 1\nlet y = 2\nlet z = 3";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        // Find 'z' token and check it's on line 3
        Token zToken = tokens.stream()
            .filter(t -> "z".equals(t.lexeme))
            .findFirst()
            .orElse(null);
        assertNotNull("Should find z token", zToken);
        assertEquals("z should be on line 3", 3, zToken.line);
    }

    @Test
    public void testLexerArrowToken() {
        String source = "fn add(x, y) = x + y";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.FN, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("add", tokens.get(1).lexeme);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testLexerUnterminatedString() {
        String source = "\"unterminated";
        Lexer lexer = new Lexer(source, reporter);
        lexer.scanTokens();

        assertTrue("Should report error for unterminated string", reporter.hasErrors());
    }

    @Test
    public void testLexerUnexpectedCharacter() {
        String source = "let x = @";
        Lexer lexer = new Lexer(source, reporter);
        lexer.scanTokens();

        assertTrue("Should report error for unexpected character", reporter.hasErrors());
    }

    @Test
    public void testLexerEofToken() {
        String source = "";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type);
    }

    @Test
    public void testLexerLetStatement() {
        String source = "let x = 42";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.LET, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("x", tokens.get(1).lexeme);
        assertEquals(TokenType.EQUAL, tokens.get(2).type);
        assertEquals(TokenType.INT, tokens.get(3).type);
        assertEquals(42, tokens.get(3).literal);
    }

    @Test
    public void testLexerMatchKeyword() {
        String source = "match x: | 1 -> 2";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.MATCH, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals(TokenType.COLON, tokens.get(2).type);
        assertEquals(TokenType.PIPE, tokens.get(3).type);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testLexerForLoop() {
        String source = "for x in [1, 2, 3] do\nend";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.FOR, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertEquals("x", tokens.get(1).lexeme);
        assertEquals(TokenType.IN, tokens.get(2).type);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testLexerWhileLoop() {
        String source = "while x == 1 do\nend";
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();

        assertEquals(TokenType.WHILE, tokens.get(0).type);
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).type);
        assertFalse("Should have no errors", reporter.hasErrors());
    }
}

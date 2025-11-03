package ovh.finite.lexer;

import ovh.finite.Diagnostic;
import ovh.finite.DiagnosticReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final DiagnosticReporter reporter;
    private final boolean debug;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startColumn = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("let", TokenType.LET);
        keywords.put("fn", TokenType.FN);
        keywords.put("type", TokenType.TYPE);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);
        keywords.put("match", TokenType.MATCH);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("do", TokenType.DO);
        keywords.put("end", TokenType.END);
        keywords.put("while", TokenType.WHILE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
    }

    public Lexer(String source, DiagnosticReporter reporter) {
        this(source, reporter, false);
    }

    public Lexer(String source, DiagnosticReporter reporter, boolean debug) {
        this.source = source;
        this.reporter = reporter;
        this.debug = debug;
    }

    public List<Token> scanTokens() {
        if (debug) {
            System.out.println("=== LEXER DEBUG ===");
        }
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        if (debug) {
            System.out.println("=== LEXER COMPLETE ===");
            for (Token token : tokens) {
                System.out.println(token);
            }
        }
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '[': addToken(TokenType.LBRACKET); break;
            case ']': addToken(TokenType.RBRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ':': addToken(TokenType.COLON); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-':
                if (match('>')) addToken(TokenType.ARROW);
                else addToken(TokenType.MINUS);
                break;
            case '*': addToken(TokenType.STAR); break;
            case '/': addToken(TokenType.SLASH); break;
            case '%': addToken(TokenType.PERCENT); break;
            case '=':
                if (match('=')) addToken(TokenType.EQUAL_EQUAL);
                else addToken(TokenType.EQUAL);
                break;
            case '!':
                if (match('=')) addToken(TokenType.BANG_EQUAL);
                else {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Unexpected '!'", null, line, column, "E001", "Did you mean '!='?"));
                }
                break;
            case '<':
                if (match('=')) addToken(TokenType.LESS_EQUAL);
                else addToken(TokenType.LESS);
                break;
            case '>':
                if (match('=')) addToken(TokenType.GREATER_EQUAL);
                else addToken(TokenType.GREATER);
                break;
            case '|': addToken(TokenType.PIPE); break;
            case '_': addToken(TokenType.UNDERSCORE); break;
            case '"': string(); break;
            case '#':
                while (peek() != '\n' && !isAtEnd()) advance();
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                column = 1;
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Unexpected character: '" + c + "'", null, line, column, "E002", null));
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) advance();
            addToken(TokenType.FLOAT, Double.parseDouble(source.substring(start, current)));
        } else {
            addToken(TokenType.INT, Integer.parseInt(source.substring(start, current)));
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Unterminated string", null, line, column, "E003", "Add closing quote"));
            return;
        }

        advance(); // closing "

        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (current >= source.length()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        column++;
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        Token token = new Token(type, text, literal, line, startColumn);
        tokens.add(token);
        if (debug) {
            System.out.println("Token: " + token);
        }
    }
}
package ovh.finite.contract_lexer;

import ovh.finite.Diagnostic;
import ovh.finite.DiagnosticReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractLexer {
    private final String source;
    private final List<ContractToken> tokens = new ArrayList<>();
    private final DiagnosticReporter reporter;
    private final boolean debug;
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int startColumn = 1;

    private static final Map<String, ContractTokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("Contract", ContractTokenType.CONTRACT);
        keywords.put("fn", ContractTokenType.FN);
        keywords.put("var", ContractTokenType.VAR);
        keywords.put("if", ContractTokenType.IF);
        keywords.put("else", ContractTokenType.ELSE);
        keywords.put("while", ContractTokenType.WHILE);
        keywords.put("switch", ContractTokenType.SWITCH);
        keywords.put("case", ContractTokenType.CASE);
    }

    public ContractLexer(String source, DiagnosticReporter reporter) {
        this(source, reporter, false);
    }

    public ContractLexer(String source, DiagnosticReporter reporter, boolean debug) {
        this.source = source;
        this.reporter = reporter;
        this.debug = debug;
    }

    public List<ContractToken> scanTokens() {
        if (debug) {
            System.out.println("=== CONTRACT LEXER DEBUG ===");
        }
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }

        tokens.add(new ContractToken(ContractTokenType.EOF, "", null, line, column));
        if (debug) {
            System.out.println("=== CONTRACT LEXER COMPLETE ===");
            for (ContractToken token : tokens) {
                System.out.println(token);
            }
        }
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(ContractTokenType.LPAREN); break;
            case ')': addToken(ContractTokenType.RPAREN); break;
            case '{': addToken(ContractTokenType.LBRACE); break;
            case '}': addToken(ContractTokenType.RBRACE); break;
            case '[': addToken(ContractTokenType.LBRACKET); break;
            case ']': addToken(ContractTokenType.RBRACKET); break;
            case ',': addToken(ContractTokenType.COMMA); break;
            case '.': addToken(ContractTokenType.DOT); break;
            case ':': addToken(ContractTokenType.COLON); break;
            case ';': addToken(ContractTokenType.SEMICOLON); break;
            case '+': addToken(ContractTokenType.PLUS); break;
            case '-': addToken(ContractTokenType.MINUS); break;
            case '*': addToken(ContractTokenType.STAR); break;
            case '/':
                // Check for comments
                if (match('/')) {
                    // Single-line comment - skip until end of line
                    while (peek() != '\n' && !isAtEnd()) {
                        advance();
                    }
                } else if (match('*')) {
                    // Multi-line comment - skip until */
                    while (!isAtEnd()) {
                        if (peek() == '*' && peekNext() == '/') {
                            advance(); // consume *
                            advance(); // consume /
                            break;
                        }
                        if (peek() == '\n') {
                            line++;
                            column = 1;
                        }
                        advance();
                    }
                } else {
                    addToken(ContractTokenType.SLASH);
                }
                break;
            case '=':
                if (match('=')) addToken(ContractTokenType.EQUAL_EQUAL);
                else addToken(ContractTokenType.EQUAL);
                break;
            case '!':
                if (match('=')) addToken(ContractTokenType.BANG_EQUAL);
                else addToken(ContractTokenType.BANG);
                break;
            case '<': addToken(ContractTokenType.LESS); break;
            case '>': addToken(ContractTokenType.GREATER); break;
            case '"': string(); break;
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
        ContractTokenType type = keywords.get(text);
        if (type == null) type = ContractTokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        if (peek() == 'x') {
            advance(); // x
            while (isHexDigit(peek())) advance();
            addToken(ContractTokenType.INT, Integer.parseInt(source.substring(start + 2, current), 16));
        } else {
            while (isDigit(peek())) advance();

            if (peek() == '.' && isDigit(peekNext())) {
                advance();
                while (isDigit(peek())) advance();
                addToken(ContractTokenType.FLOAT, Double.parseDouble(source.substring(start, current)));
            } else {
                addToken(ContractTokenType.INT, Integer.parseInt(source.substring(start, current)));
            }
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 1;
            }
            advance();
        }

        if (isAtEnd()) {
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Unterminated string", null, line, column, "E003", "Add closing quote"));
            return;
        }

        advance(); // closing "

        String value = source.substring(start + 1, current - 1);
        addToken(ContractTokenType.STRING, value);
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

    private boolean isHexDigit(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
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

    private void addToken(ContractTokenType type) {
        addToken(type, null);
    }

    private void addToken(ContractTokenType type, Object literal) {
        String text = source.substring(start, current);
        ContractToken token = new ContractToken(type, text, literal, line, startColumn);
        tokens.add(token);
        if (debug) {
            System.out.println("ContractToken: " + token);
        }
    }
}
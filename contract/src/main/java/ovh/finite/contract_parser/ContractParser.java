package ovh.finite.contract_parser;

import ovh.finite.Diagnostic;
import ovh.finite.DiagnosticReporter;
import ovh.finite.contract_ast.*;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_lexer.ContractTokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractParser {
    private final List<ContractToken> tokens;
    private final DiagnosticReporter reporter;
    private final boolean debug;
    private int current = 0;

    public ContractParser(List<ContractToken> tokens, DiagnosticReporter reporter) {
        this(tokens, reporter, false);
    }

    public ContractParser(List<ContractToken> tokens, DiagnosticReporter reporter, boolean debug) {
        this.tokens = tokens;
        this.reporter = reporter;
        this.debug = debug;
    }

    public List<ContractStatement> parse() {
        if (debug) {
            System.out.println("=== CONTRACT PARSER DEBUG ===");
        }
        List<ContractStatement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            if (check(ContractTokenType.CONTRACT) || check(ContractTokenType.FN) || check(ContractTokenType.LBRACKET)) {
                ContractStatement stmt = parseTopLevel();
                if (stmt != null) {
                    statements.add(stmt);
                    if (reporter.hasReachedMaxErrors()) break;
                    if (debug) {
                        System.out.println("Parsed contract statement: " + stmt);
                    }
                } else {
                    synchronize();
                }
            } else {
                synchronize();
            }
        }
        if (debug) {
            System.out.println("=== CONTRACT PARSER COMPLETE ===");
        }
        return statements;
    }

    private List<Attribute> parseAttributes() {
        List<Attribute> attributes = new ArrayList<>();
        while (match(ContractTokenType.LBRACKET)) {
            String name = consume(ContractTokenType.IDENTIFIER, "Expected attribute name").lexeme;
            
            Map<String, Object> namedParams = new HashMap<>();
            List<Object> positionalParams = new ArrayList<>();
            
            // Parse attribute parameters: [Name] or [Name(params)]
            if (match(ContractTokenType.LPAREN)) {
                if (!check(ContractTokenType.RPAREN)) {
                    parseAttributeParameters(namedParams, positionalParams);
                }
                consume(ContractTokenType.RPAREN, "Expected ')' after parameters");
            }
            
            consume(ContractTokenType.RBRACKET, "Expected ']' after attribute");
            
            // Create attribute with both named and positional params
            // For backward compat, if only one positional string param, also set legacy parameter field
            String legacyParam = null;
            if (positionalParams.size() == 1 && positionalParams.get(0) instanceof String && namedParams.isEmpty()) {
                legacyParam = (String) positionalParams.get(0);
            }
            attributes.add(new Attribute(name, namedParams, positionalParams, legacyParam));
        }
        return attributes;
    }

    private void parseAttributeParameters(Map<String, Object> namedParams, List<Object> positionalParams) {
        do {
            Object value = parseAttributeValue();
            
            // Check if this is a named parameter (key: value)
            if (check(ContractTokenType.COLON)) {
                if (value instanceof String) {
                    String key = (String) value;
                    advance(); // consume ':'
                    Object paramValue = parseAttributeValue();
                    namedParams.put(key, paramValue);
                } else {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected identifier before ':' in named parameter", null, peek().line, peek().column, "E006", null));
                }
            } else {
                // Positional parameter
                positionalParams.add(value);
            }
        } while (match(ContractTokenType.COMMA));
    }

    private Object parseAttributeValue() {
        if (match(ContractTokenType.STRING)) {
            return previous().literal;
        } else if (match(ContractTokenType.INT)) {
            return previous().literal;
        } else if (match(ContractTokenType.FLOAT)) {
            return previous().literal;
        } else if (match(ContractTokenType.IDENTIFIER)) {
            // Could be a key for named param or a reference
            return previous().lexeme;
        } else {
            ContractToken token = peek();
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected attribute parameter value", null, token.line, token.column, "E006", null));
            advance();
            return null;
        }
    }

    private ContractStatement parseTopLevel() {
        List<Attribute> attributes = parseAttributes();
        if (match(ContractTokenType.CONTRACT)) {
            return parseContractDecl(attributes);
        } else if (match(ContractTokenType.FN)) {
            return parseFunctionDecl(attributes);
        } else {
            ContractToken token = peek();
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected top-level declaration", null, token.line, token.column, "E006", null));
            return null;
        }
    }

    private ContractDecl parseContractDecl(List<Attribute> attributes) {
        String name = consume(ContractTokenType.IDENTIFIER, "Expected contract name").lexeme;
        consume(ContractTokenType.LBRACE, "Expected '{' after contract name");
        List<ContractStatement> members = new ArrayList<>();
        while (!check(ContractTokenType.RBRACE) && !isAtEnd()) {
            if (check(ContractTokenType.FN)) {
                ContractStatement stmt = parseTopLevel();
                if (stmt != null) {
                    members.add(stmt);
                } else {
                    synchronizeMembers();
                }
            } else {
                synchronizeMembers();
            }
        }
        consume(ContractTokenType.RBRACE, "Expected '}' after contract body");
        return new ContractDecl(attributes, name, members);
    }

    private FunctionDecl parseFunctionDecl(List<Attribute> attributes) {
        String name = consume(ContractTokenType.IDENTIFIER, "Expected function name").lexeme;
        consume(ContractTokenType.LPAREN, "Expected '(' after function name");
        List<String> paramNames = new ArrayList<>();
        List<String> paramTypes = new ArrayList<>();
        if (!check(ContractTokenType.RPAREN)) {
            do {
                paramNames.add(consume(ContractTokenType.IDENTIFIER, "Expected parameter name").lexeme);
                consume(ContractTokenType.COLON, "Expected ':' after parameter name");
                paramTypes.add(consume(ContractTokenType.IDENTIFIER, "Expected type").lexeme);
            } while (match(ContractTokenType.COMMA));
        }
        consume(ContractTokenType.RPAREN, "Expected ')' after parameters");
        String returnType = null;
        if (match(ContractTokenType.MINUS) && match(ContractTokenType.GREATER)) {
            returnType = consume(ContractTokenType.IDENTIFIER, "Expected return type").lexeme;
        }
        consume(ContractTokenType.LBRACE, "Expected '{' before function body");
        List<ContractStatement> body = parseBlock();
        consume(ContractTokenType.RBRACE, "Expected '}' after function body");
        return new FunctionDecl(attributes, name, paramNames, paramTypes, returnType, body);
    }

    private List<ContractStatement> parseBlock() {
        List<ContractStatement> statements = new ArrayList<>();
        int statementCount = 0;
        while (!check(ContractTokenType.RBRACE) && !isAtEnd()) {
            if (++statementCount > 50) break;
            if (reporter.hasReachedMaxErrors()) break;
            ContractStatement stmt = parseStatement();
            if (stmt != null) {
                statements.add(stmt);
            } else {
                advance();
            }
        }
        return statements;
    }

    private ContractStatement parseStatement() {
        if (match(ContractTokenType.VAR)) {
            return parseVarDecl();
        } else if (match(ContractTokenType.IF)) {
            return parseIfStmt();
        } else if (match(ContractTokenType.WHILE)) {
            return parseWhileStmt();
        } else if (match(ContractTokenType.SWITCH)) {
            return parseSwitchStmt();
        } else {
            ContractExpression expr = parseExpression();
            if (expr == null) {
                return null;
            }
            // Optional semicolon
            match(ContractTokenType.SEMICOLON);
            return new ExprStatement(expr);
        }
    }

    private VarDecl parseVarDecl() {
        List<Attribute> attributes = parseAttributes();
        String name = consume(ContractTokenType.IDENTIFIER, "Expected variable name").lexeme;
        ContractExpression initializer = null;
        if (match(ContractTokenType.EQUAL)) {
            initializer = parseExpression();
        }
        // Optional semicolon
        match(ContractTokenType.SEMICOLON);
        return new VarDecl(attributes, name, initializer);
    }

    private IfStmt parseIfStmt() {
        consume(ContractTokenType.LPAREN, "Expected '(' after 'if'");
        ContractExpression condition = parseExpression();
        if (condition == null) return null;
        consume(ContractTokenType.RPAREN, "Expected ')' after condition");
        consume(ContractTokenType.LBRACE, "Expected '{' after condition");
        List<ContractStatement> thenBranch = parseBlock();
        consume(ContractTokenType.RBRACE, "Expected '}' after then branch");
        List<ContractStatement> elseBranch = null;
        if (match(ContractTokenType.ELSE)) {
            if (match(ContractTokenType.COLON)) {
                // else: { ... }
                consume(ContractTokenType.LBRACE, "Expected '{' after 'else:'");
                elseBranch = parseBlock();
                consume(ContractTokenType.RBRACE, "Expected '}' after else branch");
            } else {
                // else { ... }
                consume(ContractTokenType.LBRACE, "Expected '{' after 'else'");
                elseBranch = parseBlock();
                consume(ContractTokenType.RBRACE, "Expected '}' after else branch");
            }
        }
        return new IfStmt(condition, thenBranch, elseBranch);
    }

    private WhileStmt parseWhileStmt() {
        consume(ContractTokenType.LPAREN, "Expected '(' after 'while'");
        ContractExpression condition = parseExpression();
        if (condition == null) return null;
        consume(ContractTokenType.RPAREN, "Expected ')' after condition");
        consume(ContractTokenType.LBRACE, "Expected '{' after condition");
        List<ContractStatement> body = parseBlock();
        consume(ContractTokenType.RBRACE, "Expected '}' after while body");
        return new WhileStmt(condition, body);
    }

    private ContractStatement parseSwitchStmt() {
        // For simplicity, let's skip switch for now
        ContractToken token = peek();
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Switch not implemented yet", null, token.line, token.column, "E007", null));
        return null;
    }

    private ContractExpression parseExpression() {
        return parseAssignment();
    }

    private ContractExpression parseAssignment() {
        ContractExpression expr = parseEquality();
        if (expr == null) return null;
        if (match(ContractTokenType.EQUAL)) {
            ContractExpression value = parseAssignment();
            if (value == null) return null;
            if (expr instanceof Variable) {
                return new Assignment(((Variable) expr).name, value);
            } else {
                ContractToken token = peek();
                reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Invalid assignment target", null, token.line, token.column, "E008", null));
                return null;
            }
        }
        return expr;
    }

    private ContractExpression parseEquality() {
        ContractExpression expr = parseRelational();
        if (expr == null) return null;
        while (match(ContractTokenType.EQUAL_EQUAL)) {
            String operator = previous().lexeme;
            ContractExpression right = parseRelational();
            if (right == null) return null;
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseRelational() {
        ContractExpression expr = parseAdditive();
        if (expr == null) return null;
        while (match(ContractTokenType.LESS, ContractTokenType.GREATER)) {
            String operator = previous().lexeme;
            ContractExpression right = parseAdditive();
            if (right == null) return null;
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseAdditive() {
        ContractExpression expr = parseTerm();
        if (expr == null) return null;
        while (match(ContractTokenType.PLUS, ContractTokenType.MINUS)) {
            String operator = previous().lexeme;
            ContractExpression right = parseTerm();
            if (right == null) return null;
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseTerm() {
        ContractExpression expr = parseUnary();
        if (expr == null) return null;
        return expr;
    }

    private ContractExpression parseUnary() {
        if (match(ContractTokenType.BANG, ContractTokenType.MINUS)) {
            String operator = previous().lexeme;
            ContractExpression operand = parseUnary();
            if (operand == null) return null;
            return new Unary(operator, operand);
        }
        ContractExpression expr = parsePrimary();
        if (expr == null) return null;
        return expr;
    }

    private static final int MAX_PRIMARY_DEPTH = 100;
    private int primaryDepth = 0;

    private ContractExpression parsePrimary() {
        primaryDepth++;
        if (primaryDepth > MAX_PRIMARY_DEPTH) {
            ContractToken token = peek();
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Maximum expression depth exceeded", null, token.line, token.column, "EDEPTH", null));
            primaryDepth--;
            return null;
        }
        ContractExpression result = null;
        boolean done = false;
        while (!done) {
            if (match(ContractTokenType.INT)) {
                result = new Literal(previous().literal);
                done = true;
            } else if (match(ContractTokenType.STRING)) {
                result = new Literal(previous().literal);
                done = true;
            } else if (match(ContractTokenType.FLOAT)) {
                result = new Literal(previous().literal);
                done = true;
            } else if (match(ContractTokenType.IDENTIFIER)) {
                String name = previous().lexeme;
                while (match(ContractTokenType.DOT)) {
                    consume(ContractTokenType.IDENTIFIER, "Expected identifier after '.'");
                    name += "." + previous().lexeme;
                }
                if (match(ContractTokenType.LPAREN)) {
                    List<ContractExpression> args = new ArrayList<>();
                    if (!check(ContractTokenType.RPAREN)) {
                        do {
                            args.add(parseExpression());
                        } while (match(ContractTokenType.COMMA));
                    }
                    consume(ContractTokenType.RPAREN, "Expected ')' after arguments");
                    result = new FunctionCall(name, args);
                } else {
                    result = new Variable(name);
                }
                done = true;
            } else {
                ContractToken token = peek();
                reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected expression", null, token.line, token.column, "E005", null));
                advance();
                result = null;
                done = true;
            }
        }
        primaryDepth--;
        return result;
    }

    private boolean match(ContractTokenType... types) {
        for (ContractTokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(ContractTokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private ContractToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == ContractTokenType.EOF;
    }

    private ContractToken peek() {
        return tokens.get(current);
    }

    private ContractToken previous() {
        return tokens.get(current - 1);
    }

    private ContractToken consume(ContractTokenType type, String message) {
        if (check(type)) return advance();
        ContractToken token = peek();
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, message, null, token.line, token.column, "E004", null));
        advance(); // skip the bad token
        return null;
    }

    private void synchronize() {
        advance(); // skip the erroneous token
        while (!isAtEnd()) {
            if (previous().type == ContractTokenType.SEMICOLON) return;
            if (check(ContractTokenType.CONTRACT) || check(ContractTokenType.FN) || check(ContractTokenType.LBRACKET)) return;
            advance();
        }
    }

    private void synchronizeMembers() {
        advance(); // skip the erroneous token
        while (!isAtEnd()) {
            if (previous().type == ContractTokenType.SEMICOLON) return;
            if (check(ContractTokenType.FN) || check(ContractTokenType.RBRACE)) return;
            advance();
        }
    }
}

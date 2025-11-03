package ovh.finite.contract_parser;

import ovh.finite.Diagnostic;
import ovh.finite.DiagnosticReporter;
import ovh.finite.contract_ast.*;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_lexer.ContractTokenType;

import java.util.ArrayList;
import java.util.List;

public class ContractParser {
    private final List<ContractToken> tokens;
    private final DiagnosticReporter reporter;
    private int current = 0;

    public ContractParser(List<ContractToken> tokens, DiagnosticReporter reporter) {
        this.tokens = tokens;
        this.reporter = reporter;
    }

    public List<ContractStatement> parse() {
        List<ContractStatement> statements = new ArrayList<>();
        while (!isAtEnd() && (check(ContractTokenType.CONTRACT) || check(ContractTokenType.FN) || check(ContractTokenType.LBRACKET))) {
            statements.add(parseTopLevel());
        }
        return statements;
    }

    private List<Attribute> parseAttributes() {
        List<Attribute> attributes = new ArrayList<>();
        while (match(ContractTokenType.LBRACKET)) {
            String name = consume(ContractTokenType.IDENTIFIER, "Expected attribute name").lexeme;
            String param = null;
            if (match(ContractTokenType.LPAREN)) {
                param = consume(ContractTokenType.STRING, "Expected string parameter").literal.toString();
                consume(ContractTokenType.RPAREN, "Expected ')' after parameter");
            }
            consume(ContractTokenType.RBRACKET, "Expected ']' after attribute");
            attributes.add(new Attribute(name, param));
        }
        return attributes;
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
            members.add(parseTopLevel());
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
        while (!check(ContractTokenType.RBRACE) && !isAtEnd()) {
            statements.add(parseStatement());
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
        if (match(ContractTokenType.EQUAL)) {
            ContractExpression value = parseAssignment();
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
        while (match(ContractTokenType.EQUAL_EQUAL)) {
            String operator = previous().lexeme;
            ContractExpression right = parseRelational();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseRelational() {
        ContractExpression expr = parseAdditive();
        while (match(ContractTokenType.LESS, ContractTokenType.GREATER)) {
            String operator = previous().lexeme;
            ContractExpression right = parseAdditive();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseAdditive() {
        ContractExpression expr = parseTerm();
        while (match(ContractTokenType.PLUS, ContractTokenType.MINUS)) {
            String operator = previous().lexeme;
            ContractExpression right = parseTerm();
            expr = new BinaryOp(expr, operator, right);
        }
        return expr;
    }

    private ContractExpression parseTerm() {
        return parseUnary();
    }

    private ContractExpression parseUnary() {
        if (match(ContractTokenType.BANG, ContractTokenType.MINUS)) {
            String operator = previous().lexeme;
            ContractExpression operand = parseUnary();
            return new Unary(operator, operand);
        }
        return parsePrimary();
    }

    private ContractExpression parsePrimary() {
        if (match(ContractTokenType.INT)) {
            return new Literal(Integer.parseInt(previous().literal.toString()));
        } else if (match(ContractTokenType.STRING)) {
            return new Literal(previous().literal);
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
                return new FunctionCall(name, args);
            } else {
                return new Variable(name);
            }
        } else {
            ContractToken token = peek();
            reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected expression", null, token.line, token.column, "E005", null));
            return null;
        }
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
}

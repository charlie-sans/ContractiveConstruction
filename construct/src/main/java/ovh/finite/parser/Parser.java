package ovh.finite.parser;

import ovh.finite.Diagnostic;
import ovh.finite.DiagnosticReporter;
import ovh.finite.ast.*;
import ovh.finite.lexer.Token;
import ovh.finite.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> tokens;
    private final DiagnosticReporter reporter;
    private int current = 0;

    public Parser(List<Token> tokens, DiagnosticReporter reporter) {
        this.tokens = tokens;
        this.reporter = reporter;
    }

    public List<Statement> parse() {
        List<Statement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private Statement parseStatement() {
        if (match(TokenType.LET)) {
            return parseLetStatement();
        } else if (match(TokenType.FN)) {
            return parseFunctionDefinition();
        } else if (match(TokenType.TYPE)) {
            return parseTypeAlias();
        } else if (match(TokenType.IMPORT)) {
            return parseImport();
        } else if (match(TokenType.WHILE)) {
            return parseWhileStatement();
        } else if (match(TokenType.FOR)) {
            return parseForStatement();
        } else {
            // For now, assume expression statement
            Expression expr = parseExpression();
            return new ExpressionStatement(expr);
        }
    }

    private LetStatement parseLetStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
        consume(TokenType.EQUAL, "Expected '='");
        Expression value = parseExpression();
        return new LetStatement(name.lexeme, value);
    }

    private FunctionDefinition parseFunctionDefinition() {
        Token name = consume(TokenType.IDENTIFIER, "Expected function name");
        consume(TokenType.LPAREN, "Expected '('");
        List<String> params = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                Token param = consume(TokenType.IDENTIFIER, "Expected parameter name");
                params.add(param.lexeme);
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')'");
        consume(TokenType.EQUAL, "Expected '='");
        Expression body = parseExpression();
        return new FunctionDefinition(name.lexeme, params, body);
    }

    private Statement parseTypeAlias() {
        // Placeholder
        consume(TokenType.IDENTIFIER, "Expected type name");
        consume(TokenType.EQUAL, "Expected '='");
        // Skip for now
        while (!isAtEnd() && !check(TokenType.LET) && !check(TokenType.FN) && !check(TokenType.TYPE) && !check(TokenType.IMPORT)) {
            advance();
        }
        return null; // TODO
    }

    private Statement parseImport() {
        consume(TokenType.IDENTIFIER, "Expected module name");
        return null; // TODO
    }

    private Expression parseExpression() {
        if (match(TokenType.MATCH)) {
            return parseMatchExpression();
        }
        return parseEquality();
    }

    private Expression parseEquality() {
        Expression expr = parseComparison();
        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expression right = parseComparison();
            expr = new BinaryOp(expr, operator.lexeme, right);
        }
        return expr;
    }

    private Expression parseComparison() {
        Expression expr = parseTerm();
        while (match(TokenType.LESS, TokenType.LESS_EQUAL, TokenType.GREATER, TokenType.GREATER_EQUAL)) {
            Token operator = previous();
            Expression right = parseTerm();
            expr = new BinaryOp(expr, operator.lexeme, right);
        }
        return expr;
    }

    private Expression parseTerm() {
        Expression expr = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expression right = parseFactor();
            expr = new BinaryOp(expr, operator.lexeme, right);
        }
        return expr;
    }

    private Expression parseFactor() {
        Expression expr = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            Expression right = parseUnary();
            expr = new BinaryOp(expr, operator.lexeme, right);
        }
        return expr;
    }

    private Expression parseUnary() {
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            Expression right = parseUnary();
            return new UnaryOp(operator.lexeme, right);
        }
        return parsePrimary();
    }

    private Expression parsePrimary() {
        if (match(TokenType.FALSE)) return new Literal(false);
        if (match(TokenType.TRUE)) return new Literal(true);
        if (match(TokenType.INT)) return new Literal(previous().literal);
        if (match(TokenType.FLOAT)) return new Literal(previous().literal);
        if (match(TokenType.STRING)) return new Literal(previous().literal);
        if (match(TokenType.UNDERSCORE)) return new Variable("_");
        if (match(TokenType.LBRACKET)) return parseListLiteral();
        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            if (match(TokenType.LPAREN)) {
                return parseFunctionCall(name);
            } else {
                return new Variable(name.lexeme);
            }
        }
        if (match(TokenType.LPAREN)) {
            Expression expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')'");
            return expr;
        }
        if (match(TokenType.IF)) {
            return parseIfExpression();
        }
        Token token = peek();
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Expected expression", null, token.line, token.column, "E005", null));
        return null;
    }

    private Expression parseFunctionCall(Token name) {
        List<Expression> args = new ArrayList<>();
        if (!check(TokenType.RPAREN)) {
            do {
                args.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RPAREN, "Expected ')'");
        return new FunctionCall(new Variable(name.lexeme), args);
    }

    private Expression parseListLiteral() {
        List<Expression> elements = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RBRACKET, "Expected ']'");
        return new ListLiteral(elements);
    }

    private Statement parseWhileStatement() {
        Expression condition = parseExpression();
        consume(TokenType.DO, "Expected 'do'");
        List<Statement> body = parseBlock();
        consume(TokenType.END, "Expected 'end'");
        return new WhileStatement(condition, body);
    }

    private Statement parseForStatement() {
        Token var = consume(TokenType.IDENTIFIER, "Expected variable name");
        consume(TokenType.IN, "Expected 'in'");
        Expression iterable = parseExpression();
        consume(TokenType.DO, "Expected 'do'");
        List<Statement> body = parseBlock();
        consume(TokenType.END, "Expected 'end'");
        return new ForStatement(var.lexeme, iterable, body);
    }

    private List<Statement> parseBlock() {
        List<Statement> statements = new ArrayList<>();
        while (!check(TokenType.END) && !isAtEnd()) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private Expression parseIfExpression() {
        Expression condition = parseExpression();
        consume(TokenType.THEN, "Expected 'then'");
        Expression thenBranch = parseExpression();
        consume(TokenType.ELSE, "Expected 'else'");
        Expression elseBranch = parseExpression();
        return new IfExpression(condition, thenBranch, elseBranch);
    }

    private Expression parseMatchExpression() {
        Expression value = parseExpression();
        consume(TokenType.COLON, "Expected ':'");
        List<MatchExpression.MatchCase> cases = new ArrayList<>();
        while (match(TokenType.PIPE)) {
            Expression pattern = parseExpression();
            consume(TokenType.ARROW, "Expected '->'");
            Expression body = parseExpression();
            cases.add(new MatchExpression.MatchCase(pattern, body));
        }
        return new MatchExpression(value, cases);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        Token token = peek();
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, message, null, token.line, token.column, "E004", null));
        advance(); // skip
        return null;
    }
}
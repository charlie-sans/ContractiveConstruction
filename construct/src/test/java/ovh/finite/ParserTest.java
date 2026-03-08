package ovh.finite;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ovh.finite.ast.*;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;

public class ParserTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("", "test.construct");
    }

    private List<Statement> parse(String source) {
        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens, reporter);
        return parser.parse();
    }

    @Test
    public void testParseLetStatement() {
        List<Statement> stmts = parse("let x = 42");

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof LetStatement);
        LetStatement let = (LetStatement) stmts.get(0);
        assertEquals("x", let.name);
        assertTrue(let.value instanceof Literal);
        assertEquals(42, ((Literal) let.value).value);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseLetStatementWithString() {
        List<Statement> stmts = parse("let msg = \"hello\"");

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof LetStatement);
        LetStatement let = (LetStatement) stmts.get(0);
        assertEquals("msg", let.name);
        assertTrue(let.value instanceof Literal);
        assertEquals("hello", ((Literal) let.value).value);
    }

    @Test
    public void testParseLetStatementWithBooleanTrue() {
        List<Statement> stmts = parse("let flag = true");

        assertEquals(1, stmts.size());
        LetStatement let = (LetStatement) stmts.get(0);
        assertTrue(let.value instanceof Literal);
        assertEquals(true, ((Literal) let.value).value);
    }

    @Test
    public void testParseLetStatementWithBooleanFalse() {
        List<Statement> stmts = parse("let flag = false");

        assertEquals(1, stmts.size());
        LetStatement let = (LetStatement) stmts.get(0);
        assertTrue(let.value instanceof Literal);
        assertEquals(false, ((Literal) let.value).value);
    }

    @Test
    public void testParseFunctionDefinition() {
        List<Statement> stmts = parse("fn add(x, y) = x + y");

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof FunctionDefinition);
        FunctionDefinition func = (FunctionDefinition) stmts.get(0);
        assertEquals("add", func.name);
        assertEquals(2, func.parameters.size());
        assertEquals("x", func.parameters.get(0));
        assertEquals("y", func.parameters.get(1));
        assertTrue(func.body instanceof BinaryOp);
        BinaryOp body = (BinaryOp) func.body;
        assertEquals("+", body.operator);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseFunctionDefinitionNoParams() {
        List<Statement> stmts = parse("fn answer() = 42");

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof FunctionDefinition);
        FunctionDefinition func = (FunctionDefinition) stmts.get(0);
        assertEquals("answer", func.name);
        assertEquals(0, func.parameters.size());
        assertTrue(func.body instanceof Literal);
    }

    @Test
    public void testParseBinaryOpAddition() {
        List<Statement> stmts = parse("1 + 2");

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof ExpressionStatement);
        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        BinaryOp op = (BinaryOp) expr;
        assertEquals("+", op.operator);
        assertEquals(1, ((Literal) op.left).value);
        assertEquals(2, ((Literal) op.right).value);
    }

    @Test
    public void testParseBinaryOpSubtraction() {
        List<Statement> stmts = parse("10 - 3");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertEquals("-", ((BinaryOp) expr).operator);
    }

    @Test
    public void testParseBinaryOpMultiplication() {
        List<Statement> stmts = parse("4 * 5");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertEquals("*", ((BinaryOp) expr).operator);
    }

    @Test
    public void testParseBinaryOpDivision() {
        List<Statement> stmts = parse("10 / 2");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertEquals("/", ((BinaryOp) expr).operator);
    }

    @Test
    public void testParseBinaryOpEquality() {
        List<Statement> stmts = parse("x == 1");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertEquals("==", ((BinaryOp) expr).operator);
    }

    @Test
    public void testParseBinaryOpComparison() {
        List<Statement> stmts = parse("x > 5");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertEquals(">", ((BinaryOp) expr).operator);
    }

    @Test
    public void testParseUnaryNegation() {
        List<Statement> stmts = parse("-42");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof UnaryOp);
        UnaryOp op = (UnaryOp) expr;
        assertEquals("-", op.operator);
        assertTrue(op.operand instanceof Literal);
    }

    @Test
    public void testParseVariable() {
        List<Statement> stmts = parse("myVar");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof Variable);
        assertEquals("myVar", ((Variable) expr).name);
    }

    @Test
    public void testParseIfExpression() {
        List<Statement> stmts = parse("if x == 1 then 10 else 20");

        assertEquals(1, stmts.size());
        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof IfExpression);
        IfExpression ifExpr = (IfExpression) expr;
        assertTrue(ifExpr.condition instanceof BinaryOp);
        assertTrue(ifExpr.thenBranch instanceof Literal);
        assertTrue(ifExpr.elseBranch instanceof Literal);
        assertEquals(10, ((Literal) ifExpr.thenBranch).value);
        assertEquals(20, ((Literal) ifExpr.elseBranch).value);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseMatchExpression() {
        String source = "match x:\n| 1 -> 10\n| _ -> 0";
        List<Statement> stmts = parse(source);

        assertEquals(1, stmts.size());
        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof MatchExpression);
        MatchExpression match = (MatchExpression) expr;
        assertTrue(match.value instanceof Variable);
        assertEquals("x", ((Variable) match.value).name);
        assertEquals(2, match.cases.size());
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseMatchExpressionWildcard() {
        String source = "match n:\n| 0 -> \"zero\"\n| _ -> \"other\"";
        List<Statement> stmts = parse(source);

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof MatchExpression);
        MatchExpression match = (MatchExpression) expr;
        assertEquals(2, match.cases.size());
        // Wildcard case: pattern should be Variable("_")
        MatchExpression.MatchCase wildcard = match.cases.get(1);
        assertTrue(wildcard.pattern instanceof Variable);
        assertEquals("_", ((Variable) wildcard.pattern).name);
    }

    @Test
    public void testParseListLiteral() {
        List<Statement> stmts = parse("[1, 2, 3]");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof ListLiteral);
        ListLiteral list = (ListLiteral) expr;
        assertEquals(3, list.elements.size());
        assertEquals(1, ((Literal) list.elements.get(0)).value);
        assertEquals(2, ((Literal) list.elements.get(1)).value);
        assertEquals(3, ((Literal) list.elements.get(2)).value);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseEmptyList() {
        List<Statement> stmts = parse("[]");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof ListLiteral);
        ListLiteral list = (ListLiteral) expr;
        assertEquals(0, list.elements.size());
    }

    @Test
    public void testParseFunctionCall() {
        List<Statement> stmts = parse("add(1, 2)");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof FunctionCall);
        FunctionCall call = (FunctionCall) expr;
        assertTrue(call.function instanceof Variable);
        assertEquals("add", ((Variable) call.function).name);
        assertEquals(2, call.arguments.size());
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseFunctionCallNoArgs() {
        List<Statement> stmts = parse("greet()");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof FunctionCall);
        FunctionCall call = (FunctionCall) expr;
        assertEquals(0, call.arguments.size());
    }

    @Test
    public void testParseWhileStatement() {
        String source = "while x == 1 do\nlet y = 2\nend";
        List<Statement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof WhileStatement);
        WhileStatement whileStmt = (WhileStatement) stmts.get(0);
        assertTrue(whileStmt.condition instanceof BinaryOp);
        assertEquals(1, whileStmt.body.size());
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseForStatement() {
        String source = "for x in [1, 2, 3] do\nend";
        List<Statement> stmts = parse(source);

        assertEquals(1, stmts.size());
        assertTrue(stmts.get(0) instanceof ForStatement);
        ForStatement forStmt = (ForStatement) stmts.get(0);
        assertEquals("x", forStmt.variable);
        assertTrue(forStmt.iterable instanceof ListLiteral);
        assertEquals(0, forStmt.body.size());
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseForStatementWithBody() {
        String source = "for item in [1, 2] do\nlet y = item\nend";
        List<Statement> stmts = parse(source);

        assertTrue(stmts.get(0) instanceof ForStatement);
        ForStatement forStmt = (ForStatement) stmts.get(0);
        assertEquals("item", forStmt.variable);
        assertEquals(1, forStmt.body.size());
        assertTrue(forStmt.body.get(0) instanceof LetStatement);
    }

    @Test
    public void testParseMultipleStatements() {
        String source = "let x = 1\nlet y = 2\nlet z = 3";
        List<Statement> stmts = parse(source);

        assertEquals(3, stmts.size());
        assertTrue(stmts.get(0) instanceof LetStatement);
        assertTrue(stmts.get(1) instanceof LetStatement);
        assertTrue(stmts.get(2) instanceof LetStatement);
        assertEquals("x", ((LetStatement) stmts.get(0)).name);
        assertEquals("y", ((LetStatement) stmts.get(1)).name);
        assertEquals("z", ((LetStatement) stmts.get(2)).name);
        assertFalse("Should have no errors", reporter.hasErrors());
    }

    @Test
    public void testParseParenthesizedExpression() {
        List<Statement> stmts = parse("(1 + 2)");

        Expression expr = ((ExpressionStatement) stmts.get(0)).expression;
        assertTrue(expr instanceof BinaryOp);
        assertFalse("Should have no errors", reporter.hasErrors());
    }
}

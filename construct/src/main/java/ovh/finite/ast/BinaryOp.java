package ovh.finite.ast;

public class BinaryOp extends Expression {
    public final Expression left;
    public final String operator;
    public final Expression right;

    public BinaryOp(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}
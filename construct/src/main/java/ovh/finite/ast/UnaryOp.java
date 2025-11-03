package ovh.finite.ast;

public class UnaryOp extends Expression {
    public final String operator;
    public final Expression operand;

    public UnaryOp(String operator, Expression operand) {
        this.operator = operator;
        this.operand = operand;
    }
}
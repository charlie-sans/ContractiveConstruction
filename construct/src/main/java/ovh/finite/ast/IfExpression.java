package ovh.finite.ast;

public class IfExpression extends Expression {
    public final Expression condition;
    public final Expression thenBranch;
    public final Expression elseBranch;

    public IfExpression(Expression condition, Expression thenBranch, Expression elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}
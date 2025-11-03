package ovh.finite.contract_ast;

public class ExprStatement extends ContractStatement {
    public final ContractExpression expression;

    public ExprStatement(ContractExpression expression) {
        this.expression = expression;
    }
}
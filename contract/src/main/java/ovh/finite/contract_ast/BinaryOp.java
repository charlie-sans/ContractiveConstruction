package ovh.finite.contract_ast;

public class BinaryOp extends ContractExpression {
    public final ContractExpression left;
    public final String operator;
    public final ContractExpression right;

    public BinaryOp(ContractExpression left, String operator, ContractExpression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}
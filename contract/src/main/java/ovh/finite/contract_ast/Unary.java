package ovh.finite.contract_ast;

public class Unary extends ContractExpression {
    public final String operator;
    public final ContractExpression operand;

    public Unary(String operator, ContractExpression operand) {
        this.operator = operator;
        this.operand = operand;
    }
}
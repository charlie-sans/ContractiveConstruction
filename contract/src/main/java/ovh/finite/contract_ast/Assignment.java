package ovh.finite.contract_ast;

public class Assignment extends ContractExpression {
    public final String name;
    public final ContractExpression value;

    public Assignment(String name, ContractExpression value) {
        this.name = name;
        this.value = value;
    }
}
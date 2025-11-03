package ovh.finite.contract_ast;

public class Variable extends ContractExpression {
    public final String name;

    public Variable(String name) {
        this.name = name;
    }
}
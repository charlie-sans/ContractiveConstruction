package ovh.finite.contract_ast;

public class Literal extends ContractExpression {
    public final Object value;

    public Literal(Object value) {
        this.value = value;
    }
}
package ovh.finite.contract_ast;

import java.util.List;

public class FunctionCall extends ContractExpression {
    public final String name;
    public final List<ContractExpression> arguments;

    public FunctionCall(String name, List<ContractExpression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}
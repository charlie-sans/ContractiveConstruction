package ovh.finite.ast;

import java.util.List;

public class FunctionDefinition extends Statement {
    public final String name;
    public final List<String> parameters;
    public final Expression body;

    public FunctionDefinition(String name, List<String> parameters, Expression body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}
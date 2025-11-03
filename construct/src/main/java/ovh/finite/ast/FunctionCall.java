package ovh.finite.ast;

import java.util.List;

public class FunctionCall extends Expression {
    public final Expression function;
    public final List<Expression> arguments;

    public FunctionCall(Expression function, List<Expression> arguments) {
        this.function = function;
        this.arguments = arguments;
    }
}
package ovh.finite.ast;

import java.util.List;

public class ForStatement extends Statement {
    public final String variable;
    public final Expression iterable;
    public final List<Statement> body;

    public ForStatement(String variable, Expression iterable, List<Statement> body) {
        this.variable = variable;
        this.iterable = iterable;
        this.body = body;
    }
}
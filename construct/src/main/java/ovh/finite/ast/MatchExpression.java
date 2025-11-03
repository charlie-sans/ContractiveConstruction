package ovh.finite.ast;

import java.util.List;

public class MatchExpression extends Expression {
    public final Expression value;
    public final List<MatchCase> cases;

    public MatchExpression(Expression value, List<MatchCase> cases) {
        this.value = value;
        this.cases = cases;
    }

    public static class MatchCase {
        public final Expression pattern;
        public final Expression body;

        public MatchCase(Expression pattern, Expression body) {
            this.pattern = pattern;
            this.body = body;
        }
    }
}
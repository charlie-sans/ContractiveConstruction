package ovh.finite.ast;

public class DumpStatement extends Statement {
	public final Expression expression;

	public DumpStatement(Expression expression) {
		this.expression = expression;
	}
}


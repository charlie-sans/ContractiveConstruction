package ovh.finite.ast;

public class LetStatement extends Statement {
    public final String name;
    public final Expression value;

    public LetStatement(String name, Expression value) {
        this.name = name;
        this.value = value;
    }
}
package ovh.finite.ast;

public class Variable extends Expression {
    public final String name;

    public Variable(String name) {
        this.name = name;
    }
}
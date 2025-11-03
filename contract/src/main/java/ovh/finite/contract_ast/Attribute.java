package ovh.finite.contract_ast;

public class Attribute {
    public final String name;
    public final String parameter; // optional

    public Attribute(String name, String parameter) {
        this.name = name;
        this.parameter = parameter;
    }
}
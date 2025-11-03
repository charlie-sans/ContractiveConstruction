package ovh.finite.contract_ast;

import java.util.List;

public class VarDecl extends ContractStatement {
    public final List<Attribute> attributes;
    public final String name;
    public final ContractExpression initializer;

    public VarDecl(List<Attribute> attributes, String name, ContractExpression initializer) {
        this.attributes = attributes;
        this.name = name;
        this.initializer = initializer;
    }
}
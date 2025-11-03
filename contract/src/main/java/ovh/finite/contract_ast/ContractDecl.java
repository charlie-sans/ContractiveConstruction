package ovh.finite.contract_ast;

import java.util.List;

public class ContractDecl extends ContractStatement {
    public final List<Attribute> attributes;
    public final String name;
    public final List<ContractStatement> members;

    public ContractDecl(List<Attribute> attributes, String name, List<ContractStatement> members) {
        this.attributes = attributes;
        this.name = name;
        this.members = members;
    }
}
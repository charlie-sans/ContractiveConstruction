package ovh.finite.contract_ast;

import java.util.List;

public class FunctionDecl extends ContractStatement {
    public final List<Attribute> attributes;
    public final String name;
    public final List<String> paramNames;
    public final List<String> paramTypes;
    public final String returnType;
    public final List<ContractStatement> body;

    public FunctionDecl(List<Attribute> attributes, String name, List<String> paramNames, List<String> paramTypes, String returnType, List<ContractStatement> body) {
        this.attributes = attributes;
        this.name = name;
        this.paramNames = paramNames;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.body = body;
    }
}
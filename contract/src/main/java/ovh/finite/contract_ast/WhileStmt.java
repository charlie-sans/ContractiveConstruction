package ovh.finite.contract_ast;

import java.util.List;

public class WhileStmt extends ContractStatement {
    public final ContractExpression condition;
    public final List<ContractStatement> body;

    public WhileStmt(ContractExpression condition, List<ContractStatement> body) {
        this.condition = condition;
        this.body = body;
    }
}
package ovh.finite.contract_ast;

import java.util.List;

public class IfStmt extends ContractStatement {
    public final ContractExpression condition;
    public final List<ContractStatement> thenBranch;
    public final List<ContractStatement> elseBranch;

    public IfStmt(ContractExpression condition, List<ContractStatement> thenBranch, List<ContractStatement> elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
}
package ovh.finite.contract_ast;

public class ImportStatement extends ContractStatement {
    public final String filePath;

    public ImportStatement(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "ImportStatement(" + filePath + ")";
    }
}

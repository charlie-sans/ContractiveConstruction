package ovh.finite.ast;

public class ImportStatement extends Statement {
    public final String filePath;

    public ImportStatement(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "ImportStatement(" + filePath + ")";
    }
}

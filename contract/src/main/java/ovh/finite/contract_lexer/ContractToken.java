package ovh.finite.contract_lexer;

public class ContractToken {
    public final ContractTokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int column;

    public ContractToken(ContractTokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}
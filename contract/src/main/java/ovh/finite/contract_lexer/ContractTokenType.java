package ovh.finite.contract_lexer;

public enum ContractTokenType {
    // Keywords
    CONTRACT, FN, VAR, IF, ELSE, WHILE, SWITCH, CASE,

    // Literals
    IDENTIFIER, STRING, INT, FLOAT, TRUE, FALSE,

    // Symbols
    EQUAL, LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, DOT, COLON, SEMICOLON,
    PLUS, MINUS, STAR, SLASH, EQUAL_EQUAL, LESS, GREATER, BANG, BANG_EQUAL,

    // Special
    EOF
}
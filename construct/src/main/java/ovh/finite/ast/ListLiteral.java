package ovh.finite.ast;

import java.util.List;

public class ListLiteral extends Expression {
    public final List<Expression> elements;

    public ListLiteral(List<Expression> elements) {
        this.elements = elements;
    }
}
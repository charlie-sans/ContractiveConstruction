package ovh.finite.contract_ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic attribute that can be applied to declarations.
 * Supports named parameters, positional parameters, and is extensible for custom uses.
 * 
 * Examples:
 *   [DllImport("kernel32.dll")]
 *   [Serialize(format: "json")]
 *   [Obsolete]
 */
public class Attribute {
    public final String name;
    public final Map<String, Object> namedParams;
    public final List<Object> positionalParams;

    // Legacy: single string parameter for backward compatibility
    public final String parameter;

    public Attribute(String name) {
        this(name, new HashMap<>(), new ArrayList<>(), null);
    }

    public Attribute(String name, String legacyParameter) {
        this(name, new HashMap<>(), new ArrayList<>(), legacyParameter);
    }

    public Attribute(String name, Map<String, Object> namedParams) {
        this(name, namedParams, new ArrayList<>(), null);
    }

    public Attribute(String name, List<Object> positionalParams) {
        this(name, new HashMap<>(), positionalParams, null);
    }

    public Attribute(String name, Map<String, Object> namedParams, List<Object> positionalParams) {
        this(name, namedParams, positionalParams, null);
    }

    public Attribute(String name, Map<String, Object> namedParams, List<Object> positionalParams, String legacyParameter) {
        this.name = name;
        this.namedParams = namedParams != null ? namedParams : new HashMap<>();
        this.positionalParams = positionalParams != null ? positionalParams : new ArrayList<>();
        this.parameter = legacyParameter; // backward compatibility
    }

    /**
     * Get a named parameter value, or null if not present.
     */
    public Object getParameter(String key) {
        return namedParams.get(key);
    }

    /**
     * Get a positional parameter by index.
     */
    public Object getPositionalParameter(int index) {
        if (index < 0 || index >= positionalParams.size()) {
            return null;
        }
        return positionalParams.get(index);
    }

    /**
     * Get a named parameter as a string, or a default value if not present.
     */
    public String getParameterAsString(String key, String defaultValue) {
        Object val = namedParams.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    /**
     * Check if this attribute has a specific named parameter.
     */
    public boolean hasParameter(String key) {
        return namedParams.containsKey(key);
    }

    /**
     * Get first positional parameter as string (common for single-param attributes like [DllImport("dll")]).
     */
    public String getFirstPositionalAsString() {
        if (positionalParams.isEmpty()) {
            return null;
        }
        Object first = positionalParams.get(0);
        return first != null ? first.toString() : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[").append(name);
        if (!namedParams.isEmpty() || !positionalParams.isEmpty()) {
            sb.append("(");
            boolean first = true;
            for (Object pos : positionalParams) {
                if (!first) sb.append(", ");
                sb.append(pos);
                first = false;
            }
            for (Map.Entry<String, Object> entry : namedParams.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
                first = false;
            }
            sb.append(")");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(name, attribute.name) &&
                Objects.equals(namedParams, attribute.namedParams) &&
                Objects.equals(positionalParams, attribute.positionalParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namedParams, positionalParams);
    }
}
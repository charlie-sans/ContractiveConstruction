package ovh.finite;

public class Diagnostic {
    public enum Level {
        ERROR, WARNING
    }

    private final Level level;
    private final String message;
    private final String file;
    private final int line;
    private final int column;
    private final String code;
    private final String suggestion;

    public Diagnostic(Level level, String message, String file, int line, int column, String code, String suggestion) {
        this.level = level;
        this.message = message;
        this.file = file;
        this.line = line;
        this.column = column;
        this.code = code;
        this.suggestion = suggestion;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getCode() {
        return code;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
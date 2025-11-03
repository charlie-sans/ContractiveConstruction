package ovh.finite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiagnosticReporter {
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final String source;
    private final String filePath;
    private static final int MAX_ERRORS = 1;
    private List<String> lines = null;

    public DiagnosticReporter(String source, String filePath) {
        this.source = source;
        this.filePath = filePath;
    }

    public void report(Diagnostic diagnostic) {
        // printDiagnostic(diagnostic);
        if (diagnostics.size() < MAX_ERRORS) {
            diagnostics.add(diagnostic);
        }
    }

    public boolean hasReachedMaxErrors() {
        return diagnostics.size() >= MAX_ERRORS;
    }

    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.getLevel() == Diagnostic.Level.ERROR);
    }

    public void printDiagnostics() {
        for (Diagnostic d : diagnostics) {
            printDiagnostic(d);
        }
    }

    private void printDiagnostic(Diagnostic d) {
        if (lines == null) {
            lines = Arrays.asList(source.split("\n"));
        }
        String color = d.getLevel() == Diagnostic.Level.ERROR ? "\u001B[31m" : "\u001B[33m"; // Red for error, yellow for warning
        String reset = "\u001B[0m";
        System.err.println(color + d.getLevel().toString().toLowerCase() + reset + ": " + d.getMessage());
        if (filePath != null) {
            System.err.println("  --> " + filePath + ":" + d.getLine() + ":" + d.getColumn());
        }
        if (d.getCode() != null) {
            System.err.println("  |");
            // if (d.getLine() - 1 < lines.size()) {
            //     System.err.println("  | " + lines.get(d.getLine() - 1));
            //     System.err.println("  | " + " ".repeat(d.getColumn() - 1) + "^");
            // }
        }
        if (d.getSuggestion() != null) {
            System.err.println("  = help: " + d.getSuggestion());
        }
    }
}
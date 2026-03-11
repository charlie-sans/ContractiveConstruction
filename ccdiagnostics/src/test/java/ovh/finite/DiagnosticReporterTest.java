package ovh.finite;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class DiagnosticReporterTest {
    private DiagnosticReporter reporter;

    @Before
    public void setUp() {
        reporter = new DiagnosticReporter("let x = 1\nlet y = 2", "test.construct");
    }

    @Test
    public void testNewReporterHasNoErrors() {
        assertFalse("New reporter should have no errors", reporter.hasErrors());
        assertFalse("New reporter should not have reached max errors", reporter.hasReachedMaxErrors());
    }

    @Test
    public void testReportError() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.ERROR, "Unexpected token", null, 1, 1, "E001", null);
        reporter.report(d);

        assertTrue("Reporter should have errors after reporting an error", reporter.hasErrors());
    }

    @Test
    public void testReportWarningDoesNotSetHasErrors() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.WARNING, "Unused variable", null, 1, 1, "W001", null);
        reporter.report(d);

        assertFalse("Reporter should not report hasErrors() for warnings only", reporter.hasErrors());
    }

    @Test
    public void testHasReachedMaxErrors() {
        // MAX_ERRORS is 1, so after one error we should hit the limit
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "First error", null, 1, 1, "E001", null));

        assertTrue("Should have reached max errors", reporter.hasReachedMaxErrors());
    }

    @Test
    public void testMaxErrorsLimitsCollection() {
        // Report more than MAX_ERRORS
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Error 1", null, 1, 1, "E001", null));
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Error 2", null, 2, 1, "E001", null));
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Error 3", null, 3, 1, "E001", null));

        // Should still be in error state but not crash
        assertTrue("Should have errors", reporter.hasErrors());
        assertTrue("Should have reached max errors", reporter.hasReachedMaxErrors());
    }

    @Test
    public void testReporterWithEmptySource() {
        DiagnosticReporter emptyReporter = new DiagnosticReporter("", "empty.construct");
        assertFalse("Empty source reporter should have no errors", emptyReporter.hasErrors());
    }

    @Test
    public void testPrintDiagnosticsDoesNotThrow() {
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Test error", null, 1, 5, "E001", "Fix this"));
        // printDiagnostics should not throw an exception
        reporter.printDiagnostics();
    }

    @Test
    public void testPrintDiagnosticsWithSuggestion() {
        reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Unexpected '!'", null, 1, 3, "E001", "Did you mean '!='?"));
        // Should print suggestion, not throw
        reporter.printDiagnostics();
    }
}

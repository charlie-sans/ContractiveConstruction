package ovh.finite;

import static org.junit.Assert.*;
import org.junit.Test;

public class DiagnosticTest {

    @Test
    public void testDiagnosticErrorLevel() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.ERROR, "Something went wrong", "test.ct", 1, 5, "E001", "Fix it");

        assertEquals(Diagnostic.Level.ERROR, d.getLevel());
        assertEquals("Something went wrong", d.getMessage());
        assertEquals("test.ct", d.getFile());
        assertEquals(1, d.getLine());
        assertEquals(5, d.getColumn());
        assertEquals("E001", d.getCode());
        assertEquals("Fix it", d.getSuggestion());
    }

    @Test
    public void testDiagnosticWarningLevel() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.WARNING, "Unused variable", "test.ct", 3, 10, "W001", null);

        assertEquals(Diagnostic.Level.WARNING, d.getLevel());
        assertEquals("Unused variable", d.getMessage());
        assertEquals(3, d.getLine());
        assertEquals(10, d.getColumn());
        assertNull(d.getSuggestion());
    }

    @Test
    public void testDiagnosticNullFile() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.ERROR, "Error message", null, 1, 1, "E002", null);

        assertNull(d.getFile());
    }

    @Test
    public void testDiagnosticNullCode() {
        Diagnostic d = new Diagnostic(Diagnostic.Level.ERROR, "Error message", "test.ct", 1, 1, null, null);

        assertNull(d.getCode());
    }

    @Test
    public void testDiagnosticLevelEnum() {
        assertEquals(2, Diagnostic.Level.values().length);
        assertEquals(Diagnostic.Level.ERROR, Diagnostic.Level.valueOf("ERROR"));
        assertEquals(Diagnostic.Level.WARNING, Diagnostic.Level.valueOf("WARNING"));
    }
}

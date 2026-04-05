package org.finite;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;
import ovh.finite.DiagnosticReporter;
import ovh.finite.ImportResolver;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;
import ovh.finite.contract_ast.ContractStatement;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;
import ovh.finite.ast.Statement;

public class ImportResolverTest {
    @Test
    public void testContractCircularImport() throws Exception {
        Path dir = Files.createTempDirectory("contract_import_test");
        Path a = dir.resolve("A.ct");
        Path b = dir.resolve("B.ct");

        Files.writeString(a, "import [\"B.ct\"]");
        Files.writeString(b, "import [\"A.ct\"]");

        String source = Files.readString(a);
        DiagnosticReporter reporter = new DiagnosticReporter(source, a.toString());

        ContractLexer lexer = new ContractLexer(source, reporter);
        List<ContractToken> tokens = lexer.scanTokens();
        ContractParser parser = new ContractParser(tokens, reporter);
        List<ContractStatement> stmts = parser.parse();

        List<ContractStatement> resolved = ImportResolver.resolveContractImports(stmts, a.toString(), reporter, false);
        assertTrue("Should report errors for circular import", reporter.hasErrors());
    }

    @Test
    public void testConstructCircularImport() throws Exception {
        Path dir = Files.createTempDirectory("construct_import_test");
        Path a = dir.resolve("A.construct");
        Path b = dir.resolve("B.construct");

        Files.writeString(a, "import [\"B.construct\"]");
        Files.writeString(b, "import [\"A.construct\"]");

        String source = Files.readString(a);
        DiagnosticReporter reporter = new DiagnosticReporter(source, a.toString());

        Lexer lexer = new Lexer(source, reporter);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens, reporter);
        List<Statement> stmts = parser.parse();

        List<Statement> resolved = ImportResolver.resolveConstructImports(stmts, a.toString(), reporter, false);
        assertTrue("Should report errors for circular import", reporter.hasErrors());
    }
}

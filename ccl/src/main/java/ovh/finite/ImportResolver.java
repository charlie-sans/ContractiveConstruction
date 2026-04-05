package ovh.finite;

import ovh.finite.contract_ast.ContractStatement;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ImportResolver {
    public static List<ContractStatement> resolveContractImports(List<ContractStatement> statements, String basePath, DiagnosticReporter reporter, boolean debug) throws IOException {
        return resolveContractImports(statements, basePath, reporter, debug, new java.util.HashSet<>());
    }

    public static List<ContractStatement> resolveContractImports(List<ContractStatement> statements, String basePath, DiagnosticReporter reporter, boolean debug, Set<Path> visited) throws IOException {
        List<ContractStatement> resolved = new ArrayList<>();
        Path baseDirPath = Paths.get(basePath).toAbsolutePath().getParent();
        if (baseDirPath == null) {
            baseDirPath = Paths.get(".").toAbsolutePath();
        }

        for (ContractStatement stmt : statements) {
            if (stmt instanceof ovh.finite.contract_ast.ImportStatement) {
                ovh.finite.contract_ast.ImportStatement importStmt = (ovh.finite.contract_ast.ImportStatement) stmt;
                Path importPath = baseDirPath.resolve(importStmt.filePath).toAbsolutePath().normalize();

                if (!Files.exists(importPath)) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Import file not found: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                Path realPath;
                try {
                    realPath = importPath.toRealPath();
                } catch (IOException e) {
                    realPath = importPath;
                }

                if (visited.contains(realPath)) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Circular import detected: " + importPath, null, 0, 0, "E101", null));
                    continue;
                }
                visited.add(realPath);

                String fileName = importPath.getFileName().toString();
                if (fileName.endsWith(".construct")) {
                    reporter.report(new Diagnostic(Diagnostic.Level.WARNING, "Import file appears to be a Construct file: " + importPath, null, 0, 0, "W100", "Imported file has a different language extension"));
                }

                String importSource = new String(Files.readAllBytes(importPath));
                DiagnosticReporter importReporter = new DiagnosticReporter(importSource, importPath.toString());

                ContractLexer importLexer = new ContractLexer(importSource, importReporter, debug);
                List<ContractToken> importTokens = importLexer.scanTokens();

                if (importReporter.hasErrors()) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Errors in imported file: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                ContractParser importParser = new ContractParser(importTokens, importReporter, debug);
                List<ContractStatement> importedStmts = importParser.parse();

                if (importReporter.hasErrors()) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Parse errors in imported file: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                List<ContractStatement> resolvedImported = resolveContractImports(importedStmts, importPath.toString(), reporter, debug, visited);
                resolved.addAll(resolvedImported);
            } else {
                resolved.add(stmt);
            }
        }

        return resolved;
    }

    public static List<ovh.finite.ast.Statement> resolveConstructImports(List<ovh.finite.ast.Statement> statements, String basePath, DiagnosticReporter reporter, boolean debug) throws IOException {
        return resolveConstructImports(statements, basePath, reporter, debug, new java.util.HashSet<>());
    }

    public static List<ovh.finite.ast.Statement> resolveConstructImports(List<ovh.finite.ast.Statement> statements, String basePath, DiagnosticReporter reporter, boolean debug, Set<Path> visited) throws IOException {
        List<ovh.finite.ast.Statement> resolved = new ArrayList<>();
        Path baseDirPath = Paths.get(basePath).toAbsolutePath().getParent();
        if (baseDirPath == null) {
            baseDirPath = Paths.get(".").toAbsolutePath();
        }

        for (ovh.finite.ast.Statement stmt : statements) {
            if (stmt instanceof ovh.finite.ast.ImportStatement) {
                ovh.finite.ast.ImportStatement importStmt = (ovh.finite.ast.ImportStatement) stmt;
                Path importPath = baseDirPath.resolve(importStmt.filePath).toAbsolutePath().normalize();

                if (!Files.exists(importPath)) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Import file not found: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                Path realPath;
                try {
                    realPath = importPath.toRealPath();
                } catch (IOException e) {
                    realPath = importPath;
                }

                if (visited.contains(realPath)) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Circular import detected: " + importPath, null, 0, 0, "E101", null));
                    continue;
                }
                visited.add(realPath);

                String fileName = importPath.getFileName().toString();
                if (fileName.endsWith(".ct") || fileName.endsWith(".contract")) {
                    reporter.report(new Diagnostic(Diagnostic.Level.WARNING, "Import file appears to be a Contract file: " + importPath, null, 0, 0, "W100", "Imported file has a different language extension"));
                }

                String importSource = new String(Files.readAllBytes(importPath));
                DiagnosticReporter importReporter = new DiagnosticReporter(importSource, importPath.toString());

                Lexer importLexer = new Lexer(importSource, importReporter, debug);
                List<Token> importTokens = importLexer.scanTokens();

                if (importReporter.hasErrors()) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Errors in imported file: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                Parser importParser = new Parser(importTokens, importReporter, debug);
                List<ovh.finite.ast.Statement> importedStmts = importParser.parse();

                if (importReporter.hasErrors()) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Parse errors in imported file: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                List<ovh.finite.ast.Statement> resolvedImported = resolveConstructImports(importedStmts, importPath.toString(), reporter, debug, visited);
                resolved.addAll(resolvedImported);
            } else {
                resolved.add(stmt);
            }
        }

        return resolved;
    }
}

package ovh.finite;

import ovh.finite.DiagnosticReporter;
import ovh.finite.ast.Statement;
import ovh.finite.codegen.CodeGenerator;
import ovh.finite.contract_ast.ContractStatement;
import ovh.finite.contract_ast.ImportStatement;
import ovh.finite.contract_codegen.ContractCodeGenerator;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Main {
    enum Language {
        CONSTRUCT, CONTRACT
    }

    public static void main(String[] args) throws IOException {
        String langArg = null;
        String filePath = null;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--lang")) {
                if (i + 1 < args.length) {
                    langArg = args[i + 1];
                    i++;
                } else {
                    System.err.println("Error: --lang requires a value");
                    System.exit(1);
                }
            } else if (args[i].equals("--debug")) {
                debug = true;
            } else if (filePath == null) {
                filePath = args[i];
            } else {
                System.err.println("Usage: java -jar ccl.jar [--lang <language>] [--debug] <source file>");
                System.exit(1);
            }
        }

        if (filePath == null) {
            System.err.println("Usage: java -jar ccl.jar [--lang <language>] [--debug] <source file>");
            System.exit(1);
        }

        Language lang = null;
        if (langArg != null) {
            if (langArg.equals("construct")) {
                lang = Language.CONSTRUCT;
            } else if (langArg.equals("contract")) {
                lang = Language.CONTRACT;
            } else {
                System.err.println("Unknown language: " + langArg);
                System.exit(1);
            }
        } else {
            // Detect by extension
            if (filePath.endsWith(".construct")) {
                lang = Language.CONSTRUCT;
            } else if (filePath.endsWith(".ct") || filePath.endsWith(".contract")) {
                lang = Language.CONTRACT;
            } else {
                System.err.println("Cannot detect language from file extension. Use --lang to specify.");
                System.exit(1);
            }
        }

        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        DiagnosticReporter reporter = new DiagnosticReporter(source, filePath);

        if (lang == Language.CONSTRUCT) {
            Lexer lexer = new Lexer(source, reporter, debug);
            List<Token> tokens = lexer.scanTokens();

            if (reporter.hasErrors()) {
                reporter.printDiagnostics();
                System.exit(1);
            }

            Parser parser = new Parser(tokens, reporter, debug);
            List<Statement> statements = parser.parse();

            if (reporter.hasErrors()) {
                reporter.printDiagnostics();
                System.exit(1);
            }

            CodeGenerator generator = new CodeGenerator();
            byte[] bytecode = generator.generate(statements);

            createJar(bytecode, "Main.jar");
        } else {
            ContractLexer lexer = new ContractLexer(source, reporter, debug);
            List<ContractToken> tokens = lexer.scanTokens();

            if (reporter.hasErrors()) {
                reporter.printDiagnostics();
                System.exit(1);
            }

            ContractParser parser = new ContractParser(tokens, reporter, debug);
            List<ContractStatement> statements = parser.parse();

            if (reporter.hasErrors()) {
                reporter.printDiagnostics();
                System.exit(1);
            }

            // Resolve imports
            statements = resolveImports(statements, filePath, reporter, debug);

            if (reporter.hasErrors()) {
                reporter.printDiagnostics();
                System.exit(1);
            }

            ContractCodeGenerator generator = new ContractCodeGenerator(filePath);
            try {
                byte[] bytecode = generator.generate(statements);
                createJar(bytecode, "Main.jar");
            } catch (Exception e) {
                System.err.println("Error during code generation:");
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println("Compiled to Main.jar");
    }

    private static void createJar(byte[] bytecode, String jarPath) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Main-Class", "Main");

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(Paths.get(jarPath)), manifest)) {
            // Add Main.class
            JarEntry mainEntry = new JarEntry("Main.class");
            jos.putNextEntry(mainEntry);
            jos.write(bytecode);
            jos.closeEntry();

            // Add JNA dependencies if available
            addDependencyClasses(jos, "com.sun.jna");
            addDependencyClasses(jos, "org.finite");
        }
    }

    private static void addDependencyClasses(JarOutputStream jos, String packageName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        // Get the package path
        String path = packageName.replace('.', '/');
        // Get all resources in the package
        var resources = cl.getResources(path);
        while (resources.hasMoreElements()) {
            var url = resources.nextElement();
            if (url.getProtocol().equals("jar")) {
                // Extract from JAR
                String jarPath = url.getPath();
                int bangIndex = jarPath.indexOf("!");
                if (bangIndex > 0) {
                    jarPath = jarPath.substring(5, bangIndex); // remove "file:" prefix
                    jarPath = URLDecoder.decode(jarPath, java.nio.charset.StandardCharsets.UTF_8);
                    try (var jarFile = new java.util.jar.JarFile(jarPath)) {
                        var entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            var entry = entries.nextElement();
                            if (entry.getName().startsWith(path) && !entry.isDirectory()) {
                                JarEntry jarEntry = new JarEntry(entry.getName());
                                jos.putNextEntry(jarEntry);
                                try (var is = jarFile.getInputStream(entry)) {
                                    is.transferTo(jos);
                                }
                                jos.closeEntry();
                            }
                        }
                    }
                }
            }
        }
    }

    private static List<ContractStatement> resolveImports(List<ContractStatement> statements, String basePath, DiagnosticReporter reporter, boolean debug) throws IOException {
        List<ContractStatement> resolved = new ArrayList<>();
        Path baseDirPath = Paths.get(basePath).toAbsolutePath().getParent();
        if (baseDirPath == null) {
            baseDirPath = Paths.get(".").toAbsolutePath();
        }

        for (ContractStatement stmt : statements) {
            if (stmt instanceof ImportStatement) {
                ImportStatement importStmt = (ImportStatement) stmt;
                Path importPath = baseDirPath.resolve(importStmt.filePath).toAbsolutePath();

                if (!Files.exists(importPath)) {
                    reporter.report(new Diagnostic(Diagnostic.Level.ERROR, "Import file not found: " + importPath, null, 0, 0, "E100", null));
                    continue;
                }

                String importSource = new String(Files.readAllBytes(importPath));
                DiagnosticReporter importReporter = new DiagnosticReporter(importSource, importPath.toString());

                // Parse imported file
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

                // Recursively resolve imports in the imported file
                List<ContractStatement> resolvedImported = resolveImports(importedStmts, importPath.toString(), reporter, debug);
                resolved.addAll(resolvedImported);
            } else {
                resolved.add(stmt);
            }
        }

        return resolved;
    }
}
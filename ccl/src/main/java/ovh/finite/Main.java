package ovh.finite;

import ovh.finite.DiagnosticReporter;
import ovh.finite.ast.Statement;
import ovh.finite.codegen.CodeGenerator;
import ovh.finite.contract_ast.ContractStatement;
import ovh.finite.contract_codegen.ContractCodeGenerator;
import ovh.finite.contract_lexer.ContractLexer;
import ovh.finite.contract_lexer.ContractToken;
import ovh.finite.contract_parser.ContractParser;
import ovh.finite.lexer.Lexer;
import ovh.finite.lexer.Token;
import ovh.finite.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

            Files.write(Paths.get("Main.class"), bytecode);
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

            ContractCodeGenerator generator = new ContractCodeGenerator(filePath);
            byte[] bytecode = generator.generate(statements);

            Files.write(Paths.get("Main.class"), bytecode);
        }

        System.out.println("Compiled to Main.class");
    }
}
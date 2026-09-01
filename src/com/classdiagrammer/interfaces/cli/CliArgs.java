package com.classdiagrammer.interfaces.cli;

import java.nio.file.Paths;

/**
 * Parses and validates command-line arguments.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class CliArgs {

    private final String sourceRoot;
    private final String outputPath;
    private final String javaVersion;
    private final String outputFormat;
    private final boolean helpRequested;

    private CliArgs(String sourceRoot, String outputPath,
                    String javaVersion, String outputFormat, boolean helpRequested) {
        this.sourceRoot = sourceRoot;
        this.outputPath = outputPath;
        this.javaVersion = javaVersion;
        this.outputFormat = outputFormat;
        this.helpRequested = helpRequested;
    }

    public static CliArgs parse(String[] arguments) {
        java.util.Objects.requireNonNull(arguments, "arguments is required");
        String root = null;
        String output = null;
        String javaVersion = "8";
        String format = null;
        boolean help = false;
        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            switch (argument) {
                case "-h", "--help" -> help = true;
                case "-o", "--output" -> {
                    if (i + 1 >= arguments.length) {
                        throw new IllegalArgumentException("missing value for " + argument + "\n" + usage());
                    }
                    output = arguments[++i];
                }
                case "--java" -> {
                    if (i + 1 >= arguments.length) {
                        throw new IllegalArgumentException("missing value for " + argument + "\n" + usage());
                    }
                    javaVersion = arguments[++i];
                    validateJavaVersion(javaVersion);
                }
                case "--format" -> {
                    if (i + 1 >= arguments.length) {
                        throw new IllegalArgumentException("missing value for " + argument + "\n" + usage());
                    }
                    format = arguments[++i];
                    validateFormat(format);
                }
                default -> {
                    if (root == null) {
                        root = argument;
                    } else {
                        throw new IllegalArgumentException("unexpected argument: " + argument + "\n" + usage());
                    }
                }
            }
        }
        if (!help && (root == null || root.trim().isEmpty())) {
            throw new IllegalArgumentException(usage());
        }
        String resolvedOutput = output == null ? defaultOutput(format) : output;
        String resolvedFormat = format != null ? format : inferFormat(resolvedOutput);
        return new CliArgs(root, resolvedOutput, javaVersion, resolvedFormat, help);
    }

    private static String defaultOutput() {
        return defaultOutput(null);
    }

    private static String defaultOutput(String format) {
        String ext = "json";
        if (format != null) {
            try {
                ext = com.classdiagrammer.infrastructure.output.OutputFormat.from(format).extension();
            } catch (IllegalArgumentException ignored) {}
        }
        return Paths.get(System.getProperty("user.dir"), "code." + ext).toString();
    }

    private static String inferFormat(String outputPath) {
        try {
            return com.classdiagrammer.infrastructure.output.OutputFormat.fromPath(outputPath).name().toLowerCase();
        } catch (Exception e) {
            return "json";
        }
    }

    private static void validateJavaVersion(String raw) {
        var supported = java.util.Set.of("8", "11", "17", "21", "25");
        if (!supported.contains(raw)) {
            throw new IllegalArgumentException(
                    "unsupported java version: " + raw + " (use 8, 11, 17, 21, 25)\n" + usage());
        }
    }

    private static void validateFormat(String raw) {
        com.classdiagrammer.infrastructure.output.OutputFormat.from(raw);
    }

    public static String usage() {
        return """
                Usage: classdiagrammer <source-folder> [-o output.json] [--java <8|11|17|21|25>] [--format <json|xml|yaml|toon>]
                Scans .java files in the given folder and generates a class diagram graph.
                  --java <v>    Java parser version (default 8)
                  --format <f>  Output format: json, xml, yaml, toon (default: inferred from output file, else json)
                  -o, --output <file>  Output file (default: code.<ext>)
                """;
    }

    public String sourceRoot() {
        return sourceRoot;
    }

    public String outputPath() {
        return outputPath;
    }

    public String javaVersion() {
        return javaVersion;
    }

    public String outputFormat() {
        return outputFormat;
    }

    public boolean helpRequested() {
        return helpRequested;
    }
}

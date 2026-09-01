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
    private final boolean helpRequested;

    private CliArgs(String sourceRoot, String outputPath,
                    String javaVersion, boolean helpRequested) {
        this.sourceRoot = sourceRoot;
        this.outputPath = outputPath;
        this.javaVersion = javaVersion;
        this.helpRequested = helpRequested;
    }

    public static CliArgs parse(String[] arguments) {
        java.util.Objects.requireNonNull(arguments, "arguments is required");
        String root = null;
        String output = null;
        String javaVersion = "8";
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
        return new CliArgs(root, output == null ? defaultOutput() : output, javaVersion, help);
    }

    private static String defaultOutput() {
        return Paths.get(System.getProperty("user.dir"), "code.json").toString();
    }

    private static void validateJavaVersion(String raw) {
        var supported = java.util.Set.of("8", "11", "17", "21", "25");
        if (!supported.contains(raw)) {
            throw new IllegalArgumentException(
                    "unsupported java version: " + raw + " (use 8, 11, 17, 21, 25)\n" + usage());
        }
    }

    public static String usage() {
        return """
                Usage: classdiagrammer <source-folder> [-o output.json] [--java <8|11|17|21|25>]
                Scans .java files in the given folder and generates a JSON class diagram graph.
                  --java <v>  Java parser version (default 8)
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

    public boolean helpRequested() {
        return helpRequested;
    }
}

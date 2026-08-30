package com.classdiagrammer.interfaces.cli;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
        if (arguments == null) {
            throw new IllegalArgumentException(usage());
        }
        String root = null;
        String output = null;
        String javaVersion = "8";
        boolean help = false;
        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            if ("-h".equals(argument) || "--help".equals(argument)) {
                help = true;
            } else if ("-o".equals(argument) || "--output".equals(argument)) {
                if (i + 1 >= arguments.length) {
                    throw new IllegalArgumentException("missing value for " + argument + "\n" + usage());
                }
                output = arguments[++i];
            } else if ("--java".equals(argument)) {
                if (i + 1 >= arguments.length) {
                    throw new IllegalArgumentException("missing value for " + argument + "\n" + usage());
                }
                javaVersion = arguments[++i];
                validateJavaVersion(javaVersion);
            } else if (root == null) {
                root = argument;
            } else {
                throw new IllegalArgumentException("unexpected argument: " + argument + "\n" + usage());
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
        Set<String> supported = new HashSet<>(Arrays.asList("8", "11", "17", "21", "26"));
        if (!supported.contains(raw)) {
            throw new IllegalArgumentException(
                    "java version no soportada: " + raw + " (use 8, 11, 17, 21, 26)\n" + usage());
        }
    }

    public static String usage() {
        return "Uso: classdiagrammer <carpeta-fuente> [-o salida.json] [--java <8|11|17|21|26>]\n"
                + "Explora los .java de la carpeta indicada y genera un grafo JSON tipo diagrama de clases.\n"
                + "  --java <v>  version del parser Java (por defecto 8)\n";
    }

    public String sourceRoot() { return sourceRoot; }
    public String outputPath() { return outputPath; }
    public String javaVersion() { return javaVersion; }
    public boolean helpRequested() { return helpRequested; }
}

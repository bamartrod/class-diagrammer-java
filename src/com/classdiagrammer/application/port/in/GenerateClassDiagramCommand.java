package com.classdiagrammer.application.port.in;

public final class GenerateClassDiagramCommand {

    private final String sourceRoot;
    private final String outputPath;

    private GenerateClassDiagramCommand(String sourceRoot, String outputPath) {
        if (sourceRoot == null || sourceRoot.trim().isEmpty()) {
            throw new IllegalArgumentException("source root is required");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("output path is required");
        }
        this.sourceRoot = sourceRoot.trim();
        this.outputPath = outputPath.trim();
    }

    public static GenerateClassDiagramCommand of(String sourceRoot, String outputPath) {
        return new GenerateClassDiagramCommand(sourceRoot, outputPath);
    }

    public String sourceRoot() {
        return sourceRoot;
    }

    public String outputPath() {
        return outputPath;
    }
}

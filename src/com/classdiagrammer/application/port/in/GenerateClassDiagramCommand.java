package com.classdiagrammer.application.port.in;

import java.util.Objects;

/**
 * Immutable data carrier for the generate command (record where available).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public record GenerateClassDiagramCommand(String sourceRoot, String outputPath) {

    public GenerateClassDiagramCommand {
        if (sourceRoot == null || sourceRoot.trim().isEmpty()) {
            throw new IllegalArgumentException("source root is required");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("output path is required");
        }
        sourceRoot = sourceRoot.trim();
        outputPath = outputPath.trim();
    }

    public static GenerateClassDiagramCommand of(String sourceRoot, String outputPath) {
        return new GenerateClassDiagramCommand(sourceRoot, outputPath);
    }
}

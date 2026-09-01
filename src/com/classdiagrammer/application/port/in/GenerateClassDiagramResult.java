package com.classdiagrammer.application.port.in;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable result carrier.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public record GenerateClassDiagramResult(Path writtenTo, int typeCount, int edgeCount) {

    public GenerateClassDiagramResult {
        Objects.requireNonNull(writtenTo, "written path is required");
    }
}

package com.classdiagrammer.application.port.in;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Input port GenerateClassDiagramResult defining the use-case contract.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class GenerateClassDiagramResult {

    private final Path writtenTo;
    private final int typeCount;
    private final int edgeCount;

    public GenerateClassDiagramResult(Path writtenTo, int typeCount, int edgeCount) {
        if (writtenTo == null) {
            throw new IllegalArgumentException("written path is required");
        }
        this.writtenTo = writtenTo;
        this.typeCount = typeCount;
        this.edgeCount = edgeCount;
    }

    public Path writtenTo() {
        return writtenTo;
    }

    public int typeCount() {
        return typeCount;
    }

    public int edgeCount() {
        return edgeCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenerateClassDiagramResult)) {
            return false;
        }
        GenerateClassDiagramResult that = (GenerateClassDiagramResult) other;
        return typeCount == that.typeCount
                && edgeCount == that.edgeCount
                && writtenTo.equals(that.writtenTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(writtenTo, typeCount, edgeCount);
    }
}

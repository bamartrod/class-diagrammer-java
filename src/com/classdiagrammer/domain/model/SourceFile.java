package com.classdiagrammer.domain.model;

import java.util.Objects;

/**
 * Value object for a source file (path and content).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class SourceFile {

    private final String folder;
    private final String file;
    private final String content;

    public SourceFile(String folder, String file, String content) {
        if (folder == null) {
            throw new IllegalArgumentException("source folder is required");
        }
        if (file == null || file.trim().isEmpty()) {
            throw new IllegalArgumentException("source file path is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("source content is required");
        }
        this.folder = folder.trim();
        this.file = file.trim();
        this.content = content;
    }

    public String folder() {
        return folder;
    }

    public String file() {
        return file;
    }

    public String content() {
        return content;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SourceFile)) {
            return false;
        }
        SourceFile that = (SourceFile) other;
        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }
}

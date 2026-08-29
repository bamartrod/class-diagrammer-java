package com.classdiagrammer.tests.support;

import com.classdiagrammer.domain.model.SourceFile;

public final class Sources {

    private Sources() {
    }

    public static SourceFile java(String relativePath, String... lines) {
        StringBuilder joined = new StringBuilder();
        for (String line : lines) {
            if (joined.length() > 0) {
                joined.append('\n');
            }
            joined.append(line);
        }
        return new SourceFile(parentOf(relativePath), relativePath, joined.toString());
    }

    private static String parentOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash);
    }
}

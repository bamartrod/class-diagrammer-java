package com.classdiagrammer.infrastructure.parsing.java;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Helper TypeDeclaration supporting the Java parser pipeline.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class TypeDeclaration {

    private final Set<String> modifiers;
    private final String kindToken;
    private final String name;
    private final String header;

    TypeDeclaration(Set<String> modifiers, String kindToken, String name, String header) {
        this.modifiers = Collections.unmodifiableSet(new LinkedHashSet<>(modifiers));
        this.kindToken = kindToken;
        this.name = name;
        this.header = header;
    }

    Set<String> modifiers() {
        return modifiers;
    }

    String kindToken() {
        return kindToken;
    }

    String name() {
        return name;
    }

    String header() {
        return header;
    }
}

package com.classdiagrammer.domain.model;

/**
 * Enumeration of typerelationkind values in the domain model.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum TypeRelationKind {
    EXTENDS,
    IMPLEMENTS,
    IMPORTS,
    PERMITS;

    public String jsonName() {
        return name().toLowerCase();
    }
}

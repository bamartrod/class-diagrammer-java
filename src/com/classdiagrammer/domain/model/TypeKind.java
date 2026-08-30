package com.classdiagrammer.domain.model;

/**
 * Enumeration of typekind values in the domain model.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum TypeKind {
    CLASS,
    INTERFACE,
    ENUM,
    ANNOTATION,
    TEMPLATE,
    FORM;

    public String jsonName() {
        return name().toLowerCase();
    }
}

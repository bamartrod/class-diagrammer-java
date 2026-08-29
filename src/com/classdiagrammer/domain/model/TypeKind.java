package com.classdiagrammer.domain.model;

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

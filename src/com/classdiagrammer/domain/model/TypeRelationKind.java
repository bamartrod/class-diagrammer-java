package com.classdiagrammer.domain.model;

public enum TypeRelationKind {
    EXTENDS,
    IMPLEMENTS,
    IMPORTS,
    PERMITS;

    public String jsonName() {
        return name().toLowerCase();
    }
}

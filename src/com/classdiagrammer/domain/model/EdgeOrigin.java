package com.classdiagrammer.domain.model;

public enum EdgeOrigin {
    PROJECT,
    EXTERNAL,
    UNKNOWN;

    public String jsonName() {
        return name().toLowerCase();
    }
}

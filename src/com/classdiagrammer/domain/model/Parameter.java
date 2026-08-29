package com.classdiagrammer.domain.model;

public record Parameter(String type, String name) {

    public Parameter {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("parameter type is required");
        }
        type = type.trim();
        name = name == null ? "" : name.trim();
    }

    @Override
    public String toString() {
        return name.isEmpty() ? type : type + " " + name;
    }
}

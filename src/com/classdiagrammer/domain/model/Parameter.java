package com.classdiagrammer.domain.model;

import java.util.Objects;

/**
 * Represents a method parameter with type and name.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Parameter {

    private final String type;
    private final String name;

    public Parameter(String type, String name) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("parameter type is required");
        }
        this.type = type.trim();
        this.name = name == null ? "" : name.trim();
    }

    public String type() { return type; }
    public String name() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parameter)) return false;
        Parameter that = (Parameter) o;
        return type.equals(that.type) && name.equals(that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(type, name); }

    @Override
    public String toString() { return name.isEmpty() ? type : type + " " + name; }
}

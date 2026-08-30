package com.classdiagrammer.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a class field with name, type and visibility.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Field {

    private final String name;
    private final String type;
    private final Visibility visibility;
    private final Set<String> modifiers;

    private Field(String name, String type, Visibility visibility, Set<String> modifiers) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("field name is required");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("field type is required");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("field visibility is required");
        }
        this.name = name.trim();
        this.type = type.trim();
        this.visibility = visibility;
        this.modifiers = Collections.unmodifiableSet(new HashSet<>(modifiers));
    }

    public static Field named(String name, String type, Visibility visibility, Set<String> modifiers) {
        return new Field(name, type, visibility, modifiers);
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public Visibility visibility() {
        return visibility;
    }

    public Set<String> modifiers() {
        return modifiers;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Field)) {
            return false;
        }
        Field that = (Field) other;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}

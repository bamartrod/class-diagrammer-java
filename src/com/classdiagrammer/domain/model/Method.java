package com.classdiagrammer.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class Method {

    private final String name;
    private final String returnType;
    private final Visibility visibility;
    private final Set<String> modifiers;
    private final List<Parameter> parameters;

    private Method(String name, String returnType, Visibility visibility,
                   Set<String> modifiers, List<Parameter> parameters) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("method name is required");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("method visibility is required");
        }
        this.name = name.trim();
        this.returnType = returnType == null ? "" : returnType.trim();
        this.visibility = visibility;
        this.modifiers = Collections.unmodifiableSet(new HashSet<>(modifiers));
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }

    public static Method constructor(String name, Visibility visibility,
                                     Set<String> modifiers, List<Parameter> parameters) {
        return new Method(name, "", visibility, modifiers, parameters);
    }

    public static Method returning(String name, String returnType, Visibility visibility,
                                   Set<String> modifiers, List<Parameter> parameters) {
        if (returnType == null || returnType.trim().isEmpty()) {
            throw new IllegalArgumentException("method return type is required");
        }
        return new Method(name, returnType, visibility, modifiers, parameters);
    }

    public String name() {
        return name;
    }

    public String returnType() {
        return returnType;
    }

    public boolean isConstructor() {
        return returnType.isEmpty();
    }

    public Visibility visibility() {
        return visibility;
    }

    public Set<String> modifiers() {
        return modifiers;
    }

    public List<Parameter> parameters() {
        return parameters;
    }
}

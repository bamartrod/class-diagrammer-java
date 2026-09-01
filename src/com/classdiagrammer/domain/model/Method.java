package com.classdiagrammer.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a method or constructor with parameters and visibility.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Method {

    private final String name;
    private final String returnType;
    private final Visibility visibility;
    private final Set<String> modifiers;
    private final List<Parameter> parameters;
    private final List<String> requiredImports;

    private Method(String name, String returnType, Visibility visibility,
                   Set<String> modifiers, List<Parameter> parameters, List<String> requiredImports) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("method name is required");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("method visibility is required");
        }
        this.name = name.trim();
        this.returnType = returnType == null ? "" : returnType.trim();
        this.visibility = visibility;
        this.modifiers = Collections.unmodifiableSet(new HashSet<String>(modifiers));
        this.parameters = Collections.unmodifiableList(new ArrayList<Parameter>(parameters));
        this.requiredImports = requiredImports == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<String>(requiredImports));
    }

    public static Method constructor(String name, Visibility visibility,
                                     Set<String> modifiers, List<Parameter> parameters) {
        return new Method(name, "", visibility, modifiers, parameters, Collections.emptyList());
    }

    public static Method constructor(String name, Visibility visibility,
                                     Set<String> modifiers, List<Parameter> parameters, List<String> requiredImports) {
        return new Method(name, "", visibility, modifiers, parameters, requiredImports);
    }

    public static Method returning(String name, String returnType, Visibility visibility,
                                   Set<String> modifiers, List<Parameter> parameters) {
        if (returnType == null || returnType.trim().isEmpty()) {
            throw new IllegalArgumentException("method return type is required");
        }
        return new Method(name, returnType, visibility, modifiers, parameters, Collections.emptyList());
    }

    public static Method returning(String name, String returnType, Visibility visibility,
                                   Set<String> modifiers, List<Parameter> parameters, List<String> requiredImports) {
        if (returnType == null || returnType.trim().isEmpty()) {
            throw new IllegalArgumentException("method return type is required");
        }
        return new Method(name, returnType, visibility, modifiers, parameters, requiredImports);
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

    public List<String> requiredImports() {
        return requiredImports;
    }
}

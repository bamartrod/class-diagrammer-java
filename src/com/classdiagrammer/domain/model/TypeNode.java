package com.classdiagrammer.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Domain node representing a Java type (class, interface, enum, etc.).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class TypeNode {

    private final String qualifiedName;
    private final String simpleName;
    private final String packageName;
    private final TypeKind kind;
    private final Visibility visibility;
    private final Set<String> modifiers;
    private final String folder;
    private final String file;
    private final List<String> imports;
    private final List<String> extendsTypes;
    private final List<String> implementsTypes;
    private final List<String> permitsTypes;
    private final List<Method> methods;
    private final List<Method> constructors;
    private final List<Field> fields;

    private TypeNode(Builder builder) {
        this.qualifiedName = builder.qualifiedName;
        this.simpleName = builder.simpleName;
        this.packageName = builder.packageName;
        this.kind = builder.kind;
        this.visibility = builder.visibility;
        this.modifiers = Collections.unmodifiableSet(new HashSet<>(builder.modifiers));
        this.folder = builder.folder;
        this.file = builder.file;
        this.imports = List.copyOf(builder.imports);
        this.extendsTypes = List.copyOf(builder.extendsTypes);
        this.implementsTypes = List.copyOf(builder.implementsTypes);
        this.permitsTypes = List.copyOf(builder.permitsTypes);
        this.methods = List.copyOf(builder.methods);
        this.constructors = List.copyOf(builder.constructors);
        this.fields = List.copyOf(builder.fields);
    }

    public static Builder named(String qualifiedName, String simpleName) {
        return new Builder(qualifiedName, simpleName);
    }

    public String qualifiedName() { return qualifiedName; }
    public String simpleName() { return simpleName; }
    public String packageName() { return packageName; }
    public TypeKind kind() { return kind; }
    public Visibility visibility() { return visibility; }
    public Set<String> modifiers() { return modifiers; }
    public String folder() { return folder; }
    public String file() { return file; }
    public List<String> imports() { return imports; }
    public List<String> extendsTypes() { return extendsTypes; }
    public List<String> implementsTypes() { return implementsTypes; }
    public List<String> permitsTypes() { return permitsTypes; }
    public List<Method> methods() { return methods; }
    public List<Method> constructors() { return constructors; }
    public List<Field> fields() { return fields; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TypeNode that)) return false;
        return qualifiedName.equals(that.qualifiedName);
    }

    @Override
    public int hashCode() { return Objects.hash(qualifiedName); }

    @Override
    public String toString() { return kind.jsonName() + " " + qualifiedName; }

    public static final class Builder {

        private final String qualifiedName;
        private final String simpleName;
        private String packageName = "";
        private TypeKind kind;
        private Visibility visibility = Visibility.PACKAGE_PRIVATE;
        private Set<String> modifiers = new LinkedHashSet<>();
        private String folder = "";
        private String file = "";
        private List<String> imports = new ArrayList<>();
        private List<String> extendsTypes = new ArrayList<>();
        private List<String> implementsTypes = new ArrayList<>();
        private List<String> permitsTypes = new ArrayList<>();
        private List<Method> methods = new ArrayList<>();
        private List<Method> constructors = new ArrayList<>();
        private List<Field> fields = new ArrayList<>();

        private Builder(String qualifiedName, String simpleName) {
            if (qualifiedName == null || qualifiedName.trim().isEmpty()) throw new IllegalArgumentException("type qualified name is required");
            if (simpleName == null || simpleName.trim().isEmpty()) throw new IllegalArgumentException("type simple name is required");
            this.qualifiedName = qualifiedName.trim();
            this.simpleName = simpleName.trim();
        }

        public Builder inPackage(String packageName) { this.packageName = packageName == null ? "" : packageName.trim(); return this; }
        public Builder ofKind(TypeKind kind) { this.kind = kind; return this; }
        public Builder withVisibility(Visibility visibility) { this.visibility = visibility; return this; }
        public Builder withModifiers(Set<String> modifiers) { if (modifiers != null) this.modifiers = new LinkedHashSet<>(modifiers); return this; }
        public Builder locatedAt(String folder, String file) { this.folder = folder == null ? "" : folder; this.file = file == null ? "" : file; return this; }
        public Builder importing(List<String> imports) { if (imports != null) this.imports = new ArrayList<>(imports); return this; }
        public Builder extending(List<String> parents) { if (parents != null) this.extendsTypes = new ArrayList<>(parents); return this; }
        public Builder implementing(List<String> contracts) { if (contracts != null) this.implementsTypes = new ArrayList<>(contracts); return this; }
        public Builder permitting(List<String> permits) { if (permits != null) this.permitsTypes = new ArrayList<>(permits); return this; }
        public Builder withMethods(List<Method> methods) { if (methods != null) this.methods = new ArrayList<>(methods); return this; }
        public Builder withConstructors(List<Method> constructors) { if (constructors != null) this.constructors = new ArrayList<>(constructors); return this; }
        public Builder withFields(List<Field> fields) { if (fields != null) this.fields = new ArrayList<>(fields); return this; }

        public TypeNode build() {
            if (kind == null) throw new IllegalArgumentException("type kind is required for " + qualifiedName);
            if (visibility == null) throw new IllegalArgumentException("type visibility is required for " + qualifiedName);
            return new TypeNode(this);
        }
    }
}

package com.classdiagrammer.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CodeGraph {

    private final List<TypeNode> nodes;
    private final Map<String, TypeNode> byQualifiedName;

    private CodeGraph(List<TypeNode> nodes, Map<String, TypeNode> byQualifiedName) {
        this.nodes = nodes;
        this.byQualifiedName = byQualifiedName;
    }

    public static CodeGraph of(List<TypeNode> typeNodes) {
        List<TypeNode> accepted = new ArrayList<>(typeNodes.size());
        Map<String, TypeNode> index = new HashMap<>();
        for (TypeNode node : typeNodes) {
            if (!index.containsKey(node.qualifiedName())) {
                index.put(node.qualifiedName(), node);
                accepted.add(node);
                continue;
            }
            TypeNode disambiguated = rebuildWithId(node,
                    node.qualifiedName() + "#" + provenanceOf(node));
            while (index.containsKey(disambiguated.qualifiedName())) {
                disambiguated = rebuildWithId(disambiguated,
                        disambiguated.qualifiedName() + "#");
            }
            index.put(disambiguated.qualifiedName(), disambiguated);
            accepted.add(disambiguated);
        }
        return new CodeGraph(Collections.unmodifiableList(accepted), index);
    }

    private static String provenanceOf(TypeNode node) {
        return node.folder().isEmpty()
                ? node.file()
                : node.folder() + "/" + node.file();
    }

    private static TypeNode rebuildWithId(TypeNode node, String newId) {
        return TypeNode.named(newId, node.simpleName())
                .inPackage(node.packageName())
                .ofKind(node.kind())
                .withVisibility(node.visibility())
                .withModifiers(node.modifiers())
                .locatedAt(node.folder(), node.file())
                .importing(node.imports())
                .extending(node.extendsTypes())
                .implementing(node.implementsTypes())
                .permitting(node.permitsTypes())
                .withMethods(node.methods())
                .withConstructors(node.constructors())
                .withFields(node.fields())
                .build();
    }

    public List<TypeNode> nodes() {
        return nodes;
    }

    public int size() {
        return nodes.size();
    }

    public Optional<TypeNode> find(String qualifiedName) {
        return Optional.ofNullable(byQualifiedName.get(qualifiedName));
    }

    public List<TypeNode> findBySimpleName(String simpleName) {
        List<TypeNode> matches = new ArrayList<>();
        for (TypeNode node : nodes) {
            if (node.simpleName().equals(simpleName)) {
                matches.add(node);
            }
        }
        return matches;
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public static String samePackageQualifiedName(String packageName, String rawType) {
        if (packageName == null || packageName.isEmpty() || rawType.contains(".")) {
            return rawType;
        }
        return packageName + "." + rawType;
    }

    public Set<String> identities() {
        return byQualifiedName.keySet();
    }
}

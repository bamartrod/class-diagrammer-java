package com.classdiagrammer.domain.resolution;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.TypeRelationKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves type references into graph edges.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class EdgeResolver {

    public List<Edge> resolve(CodeGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("code graph is required");
        }
        Set<Edge> edges = new LinkedHashSet<>();
        for (TypeNode node : graph.nodes()) {
            for (String parent : node.extendsTypes()) {
                edges.add(link(node, parent, TypeRelationKind.EXTENDS, graph));
            }
            for (String contract : node.implementsTypes()) {
                edges.add(link(node, contract, TypeRelationKind.IMPLEMENTS, graph));
            }
            for (String permit : node.permitsTypes()) {
                edges.add(link(node, permit, TypeRelationKind.PERMITS, graph));
            }
            collectImportEdges(node, graph, edges);
        }
        return new ArrayList<>(edges);
    }

    private void collectImportEdges(TypeNode node, CodeGraph graph, Set<Edge> edges) {
        // class-level imports are now only for hierarchy (extends/implements/permits)
        // import edges must be derived from per-member requiredImports
        java.util.Set<String> allMemberImports = new java.util.HashSet<>();
        for (com.classdiagrammer.domain.model.Field f : node.fields()) {
            allMemberImports.addAll(f.requiredImports());
        }
        for (com.classdiagrammer.domain.model.Method m : node.methods()) {
            allMemberImports.addAll(m.requiredImports());
        }
        for (com.classdiagrammer.domain.model.Method c : node.constructors()) {
            allMemberImports.addAll(c.requiredImports());
        }
        // also include class-level hierarchy imports that are attributable as imports (for completeness)
        // but they are already covered via extends edges, not needed as import edges
        for (String imported : allMemberImports) {
            Optional<TypeNode> target = graph.find(imported);
            if (!target.isPresent() && imported.contains(".")) {
                String withoutMember = imported.substring(0, imported.lastIndexOf('.'));
                target = graph.find(withoutMember);
            }
            if (target.isPresent()) {
                edges.add(new Edge(node.qualifiedName(), imported,
                        TypeRelationKind.IMPORTS, true));
            } else if (isAttributable(imported)) {
                edges.add(new Edge(node.qualifiedName(), imported,
                        TypeRelationKind.IMPORTS, false));
            }
        }
        // fallback: if no per-member imports but class-level has imports (legacy), still handle for backward compat
        if (allMemberImports.isEmpty()) {
            for (String imported : node.imports()) {
                Optional<TypeNode> target = graph.find(imported);
                if (!target.isPresent() && imported.contains(".")) {
                    String withoutMember = imported.substring(0, imported.lastIndexOf('.'));
                    target = graph.find(withoutMember);
                }
                if (target.isPresent()) {
                    edges.add(new Edge(node.qualifiedName(), imported,
                            TypeRelationKind.IMPORTS, true));
                } else if (isAttributable(imported)) {
                    edges.add(new Edge(node.qualifiedName(), imported,
                            TypeRelationKind.IMPORTS, false));
                }
            }
        }
    }

    private boolean isAttributable(String imported) {
        if (imported.endsWith(".*") || !imported.contains(".")) {
            return false;
        }
        return !imported.startsWith("java.") && !imported.startsWith("javax.");
    }

    private Edge link(TypeNode node, String rawTarget, TypeRelationKind kind, CodeGraph graph) {
        String candidate = rawTarget.contains("<")
                ? rawTarget.substring(0, rawTarget.indexOf('<')).trim()
                : rawTarget.trim();

        Optional<TypeNode> exact = graph.find(candidate);
        if (exact.isPresent()) {
            return new Edge(node.qualifiedName(), candidate, kind, true);
        }

        if (!candidate.contains(".")) {
            Optional<TypeNode> samePackage =
                    graph.find(CodeGraph.samePackageQualifiedName(node.packageName(), candidate));
            if (samePackage.isPresent()) {
                return new Edge(node.qualifiedName(),
                        samePackage.get().qualifiedName(), kind, true);
            }
            List<TypeNode> matches = graph.findBySimpleName(simpleNameOf(candidate));
            if (matches.size() == 1) {
                return new Edge(node.qualifiedName(),
                        matches.get(0).qualifiedName(), kind, true);
            }
        }
        return new Edge(node.qualifiedName(), candidate, kind, false);
    }

    private String simpleNameOf(String qualified) {
        int dot = qualified.lastIndexOf('.');
        return dot < 0 ? qualified : qualified.substring(dot + 1);
    }
}

package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.TypeRelationKind;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Behavior verification suite for InheritanceEdgesBehavior.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class InheritanceEdgesBehavior {

    private InheritanceEdgesBehavior() {
    }

    public static void verify(TestHarness h) {
        final EdgeResolver resolver = new EdgeResolver();

        h.scope("unit/aristas");
        h.expect("inheritance connects child to its parent", () -> {
            CodeGraph graph = graphOf(
                    type("com.demo.Base"),
                    child("com.demo.Child", Arrays.asList("Base"), noImports()));
            List<Edge> edges = resolver.resolve(graph);
            return contains(edges, "com.demo.Child", "com.demo.Base",
                    TypeRelationKind.EXTENDS, true);
        });
        h.expect("realization connects class to its contract", () -> {
            CodeGraph graph = graphOf(
                    type("com.demo.Named"),
                    childWithImplements("com.demo.Impl", Arrays.asList("Named")));
            List<Edge> edges = resolver.resolve(graph);
            return contains(edges, "com.demo.Impl", "com.demo.Named",
                    TypeRelationKind.IMPLEMENTS, true);
        });
        h.expect("a parent outside the graph remains as unresolved external target", () -> {
            CodeGraph graph = graphOf(child("lonely.Orphan",
                    Arrays.asList("UnknownParent"), noImports()));
            List<Edge> edges = resolver.resolve(graph);
            return contains(edges, "lonely.Orphan", "UnknownParent",
                    TypeRelationKind.EXTENDS, false);
        });
        h.expect("a parent in same package is recognized by simple name", () -> {
            TypeNode helper = TypeNode.named("q.Helper", "Helper").inPackage("q")
                    .ofKind(TypeKind.CLASS).build();
            TypeNode user = TypeNode.named("q.Client", "Client").inPackage("q")
                    .ofKind(TypeKind.CLASS)
                    .extending(Arrays.asList("Helper")).build();
            List<Edge> edges = resolver.resolve(CodeGraph.of(Arrays.asList(helper, user)));
            return contains(edges, "q.Client", "q.Helper", TypeRelationKind.EXTENDS, true);
        });
        h.expect("imports only link types that participate in the graph", () -> {
            CodeGraph graph = graphOf(
                    type("com.demo.Base"),
                    child("com.demo.User", noParents(),
                            Arrays.asList("com.demo.Base", "java.util.List")));
            List<Edge> edges = resolver.resolve(graph);
            boolean linksToKnown = contains(edges, "com.demo.User", "com.demo.Base",
                    TypeRelationKind.IMPORTS, true);
            boolean ignoresUnknown = !containsFrom(edges, "com.demo.User", "java.util.List");
            return linksToKnown && ignoresUnknown;
        });
        h.expect("a repeated import produces a single edge", () -> {
            CodeGraph graph = graphOf(
                    type("com.demo.Base"),
                    child("com.demo.User", noParents(),
                            Arrays.asList("com.demo.Base", "com.demo.Base")));
            return resolver.resolve(graph).size() == 1;
        });
    }

    private static TypeNode type(String qualifiedName) {
        String simpleName = simpleOf(qualifiedName);
        String pkg = prefixOf(qualifiedName);
        return TypeNode.named(qualifiedName, simpleName).inPackage(pkg)
                .ofKind(TypeKind.CLASS).build();
    }

    private static TypeNode child(String qualifiedName, List<String> parents, List<String> imports) {
        return TypeNode.named(qualifiedName, simpleOf(qualifiedName))
                .inPackage(prefixOf(qualifiedName))
                .ofKind(TypeKind.CLASS)
                .extending(parents).importing(imports).build();
    }

    private static TypeNode childWithImplements(String qualifiedName, List<String> contracts) {
        return TypeNode.named(qualifiedName, simpleOf(qualifiedName))
                .inPackage(prefixOf(qualifiedName))
                .ofKind(TypeKind.CLASS)
                .implementing(contracts).build();
    }

    private static List<String> noParents() {
        return new ArrayList<>();
    }

    private static List<String> noImports() {
        return new ArrayList<>();
    }

    private static CodeGraph graphOf(TypeNode... nodes) {
        return CodeGraph.of(Arrays.asList(nodes));
    }

    private static String simpleOf(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? qualifiedName : qualifiedName.substring(dot + 1);
    }

    private static String prefixOf(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot < 0 ? "" : qualifiedName.substring(0, dot);
    }

    private static boolean contains(List<Edge> edges, String from, String to,
                                    TypeRelationKind kind, boolean resolved) {
        for (Edge edge : edges) {
            if (edge.from().equals(from) && edge.to().equals(to)
                    && edge.kind() == kind && edge.isResolved() == resolved) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsFrom(List<Edge> edges, String from, String to) {
        for (Edge edge : edges) {
            if (edge.from().equals(from) && edge.to().equals(to)) {
                return true;
            }
        }
        return false;
    }
}

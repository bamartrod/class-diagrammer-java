package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.tests.support.TestHarness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Behavior verification suite for CodeGraphIntegrity.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class CodeGraphIntegrity {

    private CodeGraphIntegrity() {
    }

    public static void verify(TestHarness h) {
        h.scope("unit/grafo");
        h.expect("two types with same identity coexist disambiguated by path", () -> {
            TypeNode first = TypeNode.named("p.A", "A")
                    .ofKind(TypeKind.CLASS).locatedAt("src/a", "A.java").build();
            TypeNode second = TypeNode.named("p.A", "A")
                    .ofKind(TypeKind.CLASS).locatedAt("src/b", "A.java").build();
            CodeGraph graph = CodeGraph.of(Arrays.asList(first, second));
            if (graph.size() != 2) {
                return false;
            }
            String secondId = graph.nodes().get(1).qualifiedName();
            return secondId.startsWith("p.A#")
                    && secondId.contains("b/A.java")
                    && graph.find("p.A").isPresent();
        });
        h.expect("cada tipo se alcanza por su nombre calificado", () -> {
            CodeGraph graph = CodeGraph.of(Arrays.asList(type("com.demo.Base"), type("com.demo.Child")));
            return graph.find("com.demo.Base").isPresent()
                    && !graph.find("com.demo.Missing").isPresent();
        });
        h.expect("homonyms are grouped by simple name preserving arrival order", () -> {
            List<TypeNode> ordered = Arrays.asList(
                    type("a.Dup"), type("com.demo.Base"), type("b.Dup"));
            CodeGraph graph = CodeGraph.of(ordered);
            List<TypeNode> matches = graph.findBySimpleName("Dup");
            return matches.size() == 2
                    && "a.Dup".equals(matches.get(0).qualifiedName())
                    && "b.Dup".equals(matches.get(1).qualifiedName());
        });
        h.expect("un proyecto vacio sigue produciendo un grafo valido", () -> {
            CodeGraph empty = CodeGraph.of(Collections.<TypeNode>emptyList());
            return empty.isEmpty() && empty.size() == 0 && !empty.find("x.Y").isPresent();
        });
    }

    private static TypeNode type(String qualifiedName) {
        String simpleName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
        return TypeNode.named(qualifiedName, simpleName).ofKind(TypeKind.CLASS).build();
    }
}

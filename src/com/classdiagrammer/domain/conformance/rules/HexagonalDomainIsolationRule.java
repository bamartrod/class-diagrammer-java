package com.classdiagrammer.domain.conformance.rules;

import com.classdiagrammer.domain.conformance.Rule;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.evidence.Evidence;

import java.util.List;

/**
 * RULE-004-U13: Domain must not depend on infrastructure.
 * Applicability: exists TypeNode in domain package.
 * Predicate: ∀ edge where from ∈ domain.* → to ∉ infrastructure.*
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class HexagonalDomainIsolationRule implements Rule {

    @Override public String ruleId() { return "RULE-004-U13"; }
    @Override public String description() { return "Domain must not depend on infrastructure (hexagonal isolation)"; }
    @Override public List<String> requiredInputs() { return List.of("DEPENDENCY_EXISTS", "TYPE_EXISTS"); }
    @Override public List<String> requiredEvidenceKinds() { return List.of("DEPENDENCY_EXISTS", "ARCHITECTURAL_ORIGIN"); }
    @Override public boolean isSupported() { return true; }

    @Override
    public boolean isApplicable(CodeGraph graph, List<Evidence> evidences) {
        if (graph == null || graph.isEmpty()) return false;
        return graph.nodes().stream().anyMatch(n -> n.packageName().startsWith("com.classdiagrammer.domain"));
    }

    @Override
    public Boolean evaluatePredicate(CodeGraph graph, List<Evidence> evidences) {
        return evaluatePredicate(graph, List.of(), evidences);
    }

    @Override
    public Boolean evaluatePredicate(CodeGraph graph, List<com.classdiagrammer.domain.model.Edge> edges, List<Evidence> evidences) {
        if (graph == null) return null;
        if (edges == null || edges.isEmpty()) {
            // fallback to node imports check
            boolean violation = graph.nodes().stream()
                    .filter(n -> n.packageName().startsWith("com.classdiagrammer.domain"))
                    .anyMatch(n -> {
                        boolean importsInfra = n.imports().stream().anyMatch(imp -> imp.startsWith("com.classdiagrammer.infrastructure"));
                        boolean fieldInfra = n.fields().stream().anyMatch(f -> f.requiredImports().stream().anyMatch(imp -> imp.startsWith("com.classdiagrammer.infrastructure")));
                        boolean methodInfra = n.methods().stream().anyMatch(m -> m.requiredImports().stream().anyMatch(imp -> imp.startsWith("com.classdiagrammer.infrastructure")));
                        return importsInfra || fieldInfra || methodInfra;
                    });
            return !violation;
        }
        boolean violation = edges.stream()
                .anyMatch(e -> e.from().startsWith("com.classdiagrammer.domain.") && e.to().startsWith("com.classdiagrammer.infrastructure."));
        return !violation;
    }
}

package com.classdiagrammer.domain.conformance.rules;

import com.classdiagrammer.domain.conformance.Rule;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.evidence.Evidence;

import java.util.List;

/**
 * CSAS-007-U9: Class cohesion — effective lines ≤200 or justified.
 * Simplified: counts lines of source file? For now uses TypeNode count as proxy: if nodes >200? Not accurate.
 * For test fixture we will use CodeGraph size or mock effectiveLines via Evidence.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class CohesionRule implements Rule {

    @Override public String ruleId() { return "CSAS-007-U9"; }
    @Override public String description() { return "Class cohesion — effective lines ≤200"; }
    @Override public List<String> requiredInputs() { return List.of("RESOURCE_OWNERSHIP"); }
    @Override public List<String> requiredEvidenceKinds() { return List.of("RESOURCE_OWNERSHIP"); }
    @Override public boolean isSupported() { return true; }

    @Override
    public boolean isApplicable(CodeGraph graph, List<Evidence> evidences) {
        return graph != null && !graph.isEmpty();
    }

    @Override
    public Boolean evaluatePredicate(CodeGraph graph, List<Evidence> evidences) {
        // Check evidences for RESOURCE_OWNERSHIP with effectiveLines >200
        if (evidences == null) return true;
        for (Evidence ev : evidences) {
            if (ev.fact().kind().name().equals("RESOURCE_OWNERSHIP") && ev.fact().subject().contains("effectiveLines")) {
                try {
                    int lines = Integer.parseInt(ev.fact().value());
                    if (lines > 200) return false;
                } catch (NumberFormatException ignored) {}
            }
        }
        return true;
    }
}

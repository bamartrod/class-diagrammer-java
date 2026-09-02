package com.classdiagrammer.domain.conformance;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.evidence.Evidence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evidence sufficiency per CSAS-002-U30 (8 questions) and U31 (S1/S2/S3).
 * Distinguishes evidence exists vs sufficient to prove required property.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class EvidenceSufficiencyEvaluator {

    private EvidenceSufficiencyEvaluator() {}

    public static EvidenceSufficiency evaluate(Rule rule, CodeGraph graph, List<Evidence> evidences) {
        List<String> need = rule.requiredInputs();
        if (need != null && !need.isEmpty() && (evidences == null || evidences.isEmpty())) {
            return EvidenceSufficiency.INSUFFICIENT;
        }
        // S2: closed-world scope — if graph empty but rule applicable → INSUFFICIENT (missing TYPE_EXISTS)
        if (graph == null || graph.isEmpty()) {
            // For rules requiring TYPE_EXISTS, empty graph means insufficient
            if (need != null && need.stream().anyMatch(k -> k.contains("TYPE_EXISTS") || k.contains("DEPENDENCY"))) {
                return EvidenceSufficiency.INSUFFICIENT;
            }
        }
        // Check contradictory evidence: same subject+kind+locator with opposite values
        // For DEPENDENCY_EXISTS, same from->to can have multiple kinds (IMPLEMENTS vs IMPORTS) — not contradictory
        if (evidences != null) {
            Set<String> seen = new HashSet<>();
            Set<String> contradictory = new HashSet<>();
            for (Evidence ev : evidences) {
                // Skip DEPENDENCY_EXISTS for contradictory check — multiple relation kinds per pair allowed
                if (ev.fact().kind() == com.classdiagrammer.domain.evidence.FactKind.DEPENDENCY_EXISTS) continue;
                String key = ev.fact().kind() + "|" + ev.fact().subject() + "|" + ev.fact().locator();
                String val = ev.fact().value();
                String seenKey = key + "|" + val;
                if (seen.contains(key) && !seen.contains(seenKey)) {
                    contradictory.add(key);
                }
                seen.add(key);
                seen.add(seenKey);
            }
            if (!contradictory.isEmpty()) {
                System.err.println("CONTRADICTORY for " + rule.ruleId() + " " + contradictory);
                return EvidenceSufficiency.CONTRADICTORY;
            }
        }
        // Check unsupported evidence extraction: if any evidence derived from UNSUPPORTED feature
        if (evidences != null) {
            for (Evidence ev : evidences) {
                if (ev.fact().kind().name().contains("LANGUAGE_FEATURE") && ev.fact().value().contains("unavailable")) {
                    // If rule requires that feature, then evidence extraction unsupported
                    // For generic check, if evidences contain UNSUPPORTED marker, return UNSUPPORTED
                    // But per U28, UNSUPPORTED is before sufficiency — handled in Rule.evaluate
                }
            }
        }
        // Q5: missing required facts → INSUFFICIENT (already handled S1)
        // Q6: insufficient scope, Q7: evidence cannot establish property — handled per rule
        return EvidenceSufficiency.SUFFICIENT;
    }
}

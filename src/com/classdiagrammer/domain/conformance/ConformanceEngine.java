package com.classdiagrammer.domain.conformance;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.evidence.Evidence;
import com.classdiagrammer.domain.evidence.EvaluationState;

import java.util.ArrayList;
import java.util.List;

/**
 * Conformance Engine per RULE-002-U28.
 * Flow: Rule → Applicability → RequiredInputs → Evidence → Sufficiency → Predicate → EvaluationState
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ConformanceEngine {

    private final List<Rule> rules;

    public ConformanceEngine(List<Rule> rules) {
        this.rules = rules == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(rules));
    }

    public ConformanceResult evaluate(CodeGraph graph, List<Edge> edges, List<Evidence> evidences) {
        List<EvaluationResult> results = new ArrayList<>();
        List<Evidence> safeEvidences = evidences == null ? java.util.Collections.emptyList() : evidences;
        List<Edge> safeEdges = edges == null ? java.util.Collections.emptyList() : edges;
        CodeGraph safeGraph = graph == null ? CodeGraph.of(java.util.Collections.emptyList()) : graph;

        for (Rule rule : rules) {
            boolean appl = rule.isApplicable(safeGraph, safeEvidences);
            // Use edge-aware applicability if rule overrides
            try {
                appl = rule.isApplicable(safeGraph, safeEdges, safeEvidences);
            } catch (Exception ignored) {}
            if (!appl) {
                results.add(new EvaluationResult(rule.ruleId(), false, rule.requiredInputs(), java.util.Collections.emptyList(),
                        EvidenceSufficiency.SUFFICIENT, null, EvaluationState.NOT_APPLICABLE,
                        rule.dependencies(), trace(rule, "NOT_APPLICABLE: APPL false")));
                continue;
            }
            if (!rule.isSupported()) {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        EvidenceSufficiency.UNSUPPORTED, null, EvaluationState.UNSUPPORTED,
                        rule.dependencies(), trace(rule, "UNSUPPORTED: not supported")));
                continue;
            }
            EvidenceSufficiency suff = EvidenceSufficiencyEvaluator.evaluate(rule, safeGraph, safeEvidences);
            if (suff == EvidenceSufficiency.INSUFFICIENT || suff == EvidenceSufficiency.CONTRADICTORY) {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        suff, null, EvaluationState.UNDECIDABLE,
                        rule.dependencies(), trace(rule, "UNDECIDABLE: insufficient evidence S1/S2/S3")));
                continue;
            }
            if (suff == EvidenceSufficiency.UNSUPPORTED) {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        suff, null, EvaluationState.UNSUPPORTED,
                        rule.dependencies(), trace(rule, "UNSUPPORTED: evidence extraction unsupported")));
                continue;
            }
            Boolean pred;
            try {
                pred = rule.evaluatePredicate(safeGraph, safeEdges, safeEvidences);
            } catch (Exception e) {
                pred = rule.evaluatePredicate(safeGraph, safeEvidences);
            }
            if (pred == null) {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        EvidenceSufficiency.SUFFICIENT, null, EvaluationState.REVIEW_REQUIRED,
                        rule.dependencies(), trace(rule, "REVIEW_REQUIRED: predicate indeterminate")));
            } else if (pred) {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        EvidenceSufficiency.SUFFICIENT, true, EvaluationState.CONFORMANT,
                        rule.dependencies(), trace(rule, "CONFORMANT: predicate true")));
            } else {
                results.add(new EvaluationResult(rule.ruleId(), true, rule.requiredInputs(), safeEvidences,
                        EvidenceSufficiency.SUFFICIENT, false, EvaluationState.NON_CONFORMANT,
                        rule.dependencies(), trace(rule, "NON_CONFORMANT: predicate false")));
            }
        }
        return ConformanceResult.aggregate(results);
    }

    public ConformanceResult evaluate(CodeGraph graph, List<Evidence> evidences) {
        return evaluate(graph, java.util.Collections.emptyList(), evidences);
    }

    public static ConformanceEngine defaultEngine() {
        return new ConformanceEngine(java.util.Arrays.asList(
                new com.classdiagrammer.domain.conformance.rules.HexagonalDomainIsolationRule(),
                new com.classdiagrammer.domain.conformance.rules.CohesionRule()
        ));
    }

    private static String trace(Rule rule, String detail) {
        return "Rule " + rule.ruleId() + " -> RequiredInputs " + rule.requiredInputs() + " -> Evidence " + rule.requiredEvidenceKinds() + " -> " + detail;
    }
}

package com.classdiagrammer.domain.conformance;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.evidence.Evidence;

import java.util.List;

/**
 * CSAS Rule Definition (APPL, PRED, NEED, EVID, EVAL, REQ, FORB, EXC, DEP, DECID, LAYER).
 * Each rule knows its applicability, required inputs, evidence kinds and normative predicate.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface Rule {

    String ruleId();

    String description();

    /**
     * APPL — applicability predicate. If false → NOT_APPLICABLE per U28.
     */
    boolean isApplicable(CodeGraph graph, List<Evidence> evidences);

    default boolean isApplicable(CodeGraph graph, List<com.classdiagrammer.domain.model.Edge> edges, List<Evidence> evidences) {
        return isApplicable(graph, evidences);
    }

    /**
     * NEED — required semantic inputs for this rule.
     */
    List<String> requiredInputs();

    /**
     * EVID — required evidence kinds.
     */
    List<String> requiredEvidenceKinds();

    /**
     * EVAL — normative predicate. Only evaluated when applicable, supported and sufficient.
     * @return true if predicate holds (→ CONFORMANT), false → NON_CONFORMANT, null if cannot be evaluated
     */
    Boolean evaluatePredicate(CodeGraph graph, List<Evidence> evidences);

    default Boolean evaluatePredicate(CodeGraph graph, List<com.classdiagrammer.domain.model.Edge> edges, List<Evidence> evidences) {
        return evaluatePredicate(graph, evidences);
    }

    /**
     * Whether ClassDiagrammer supports the semantic analysis required by this rule (§11).
     * If false and applicable → UNSUPPORTED.
     */
    boolean isSupported();

    /**
     * Dependencies on other rules (DEP).
     */
    default List<String> dependencies() { return List.of(); }

    /**
     * Layer (LAYER) per CSAS.
     */
    default String layer() { return "implementation"; }

    /**
     * Full evaluation flow U28: APPL → RequiredInputs → Evidence → Sufficiency → Predicate → State
     */
    default EvaluationResult evaluate(CodeGraph graph, List<Evidence> evidences) {
        boolean appl = isApplicable(graph, evidences);
        if (!appl) {
            return new EvaluationResult(ruleId(), false, requiredInputs(), List.of(),
                    EvidenceSufficiency.SUFFICIENT, null, com.classdiagrammer.domain.evidence.EvaluationState.NOT_APPLICABLE,
                    dependencies(), traceability("NOT_APPLICABLE: not applicable"));
        }
        if (!isSupported()) {
            return new EvaluationResult(ruleId(), true, requiredInputs(), evidences,
                    EvidenceSufficiency.UNSUPPORTED, null, com.classdiagrammer.domain.evidence.EvaluationState.UNSUPPORTED,
                    dependencies(), traceability("UNSUPPORTED: semantic analysis not supported"));
        }
        EvidenceSufficiency suff = EvidenceSufficiencyEvaluator.evaluate(this, graph, evidences);
        if (suff == EvidenceSufficiency.INSUFFICIENT || suff == EvidenceSufficiency.CONTRADICTORY) {
            return new EvaluationResult(ruleId(), true, requiredInputs(), evidences,
                    suff, null, com.classdiagrammer.domain.evidence.EvaluationState.UNDECIDABLE,
                    dependencies(), traceability("UNDECIDABLE: insufficient evidence S1/S2/S3"));
        }
        if (suff == EvidenceSufficiency.UNSUPPORTED) {
            return new EvaluationResult(ruleId(), true, requiredInputs(), evidences,
                    suff, null, com.classdiagrammer.domain.evidence.EvaluationState.UNSUPPORTED,
                    dependencies(), traceability("UNSUPPORTED: evidence extraction unsupported"));
        }
        Boolean pred = evaluatePredicate(graph, evidences);
        if (pred == null) {
            return new EvaluationResult(ruleId(), true, requiredInputs(), evidences,
                    EvidenceSufficiency.SUFFICIENT, null, com.classdiagrammer.domain.evidence.EvaluationState.REVIEW_REQUIRED,
                    dependencies(), traceability("REVIEW_REQUIRED: predicate indeterminate"));
        }
        com.classdiagrammer.domain.evidence.EvaluationState state = pred
                ? com.classdiagrammer.domain.evidence.EvaluationState.CONFORMANT
                : com.classdiagrammer.domain.evidence.EvaluationState.NON_CONFORMANT;
        return new EvaluationResult(ruleId(), true, requiredInputs(), evidences,
                EvidenceSufficiency.SUFFICIENT, pred, state,
                dependencies(), traceability(state.jsonName() + ": predicate=" + pred));
    }

    private String traceability(String detail) {
        return "Rule " + ruleId() + " → RequiredInputs " + requiredInputs() + " → Evidence " + requiredEvidenceKinds() + " → " + detail;
    }
}

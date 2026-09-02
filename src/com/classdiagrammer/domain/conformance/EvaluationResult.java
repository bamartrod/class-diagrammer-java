package com.classdiagrammer.domain.conformance;

import com.classdiagrammer.domain.evidence.EvaluationState;
import com.classdiagrammer.domain.evidence.Evidence;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Per-rule EvaluationResult per CSAS-002-U27/U28.
 * Fields: ruleId, applicability, requiredInputs, evidence, evidenceSufficiency, predicateResult, state, dependencies, traceability.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class EvaluationResult {

    private final String ruleId;
    private final boolean applicability;
    private final List<String> requiredInputs;
    private final List<Evidence> evidence;
    private final EvidenceSufficiency evidenceSufficiency;
    private final Boolean predicateResult;
    private final EvaluationState state;
    private final List<String> dependencies;
    private final String traceability;

    public EvaluationResult(String ruleId,
                            boolean applicability,
                            List<String> requiredInputs,
                            List<Evidence> evidence,
                            EvidenceSufficiency evidenceSufficiency,
                            Boolean predicateResult,
                            EvaluationState state,
                            List<String> dependencies,
                            String traceability) {
        if (ruleId == null || ruleId.trim().isEmpty()) throw new IllegalArgumentException("ruleId is required");
        if (state == null) throw new IllegalArgumentException("state is required");
        this.ruleId = ruleId.trim();
        this.applicability = applicability;
        this.requiredInputs = requiredInputs == null ? List.of() : List.copyOf(requiredInputs);
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
        this.evidenceSufficiency = evidenceSufficiency == null ? EvidenceSufficiency.SUFFICIENT : evidenceSufficiency;
        this.predicateResult = predicateResult;
        this.state = state;
        this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        this.traceability = traceability == null ? "" : traceability.trim();
    }

    public String ruleId() { return ruleId; }
    public boolean applicability() { return applicability; }
    public List<String> requiredInputs() { return requiredInputs; }
    public List<Evidence> evidence() { return evidence; }
    public EvidenceSufficiency evidenceSufficiency() { return evidenceSufficiency; }
    public Boolean predicateResult() { return predicateResult; }
    public EvaluationState state() { return state; }
    public List<String> dependencies() { return dependencies; }
    public String traceability() { return traceability; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationResult that)) return false;
        return ruleId.equals(that.ruleId) && state == that.state;
    }

    @Override
    public int hashCode() { return Objects.hash(ruleId, state); }

    @Override
    public String toString() { return ruleId + ":" + state.jsonName() + " appl=" + applicability + " suff=" + evidenceSufficiency; }
}

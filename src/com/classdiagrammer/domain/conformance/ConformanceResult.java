package com.classdiagrammer.domain.conformance;

import com.classdiagrammer.domain.evidence.EvaluationState;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate conformance per CSAS-002-U32/U33.
 * Per-rule results + aggregate state.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ConformanceResult {

    private final List<EvaluationResult> results;
    private final EvaluationState aggregateState;

    public ConformanceResult(List<EvaluationResult> results, EvaluationState aggregateState) {
        this.results = results == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(results));
        this.aggregateState = aggregateState == null ? EvaluationState.CONFORMANT : aggregateState;
    }

    public List<EvaluationResult> results() { return results; }
    public EvaluationState aggregateState() { return aggregateState; }

    /**
     * Aggregate per U33: NON_CONFORMANT > REVIEW_REQUIRED > UNDECIDABLE > UNSUPPORTED > CONFORMANT
     * NOT_APPLICABLE excluded (does not affect aggregate unless all are NOT_APPLICABLE → NOT_APPLICABLE)
     */
    public static ConformanceResult aggregate(List<EvaluationResult> results) {
        if (results == null || results.isEmpty()) {
            return new ConformanceResult(java.util.Collections.emptyList(), EvaluationState.CONFORMANT);
        }
        boolean hasNonConformant = false;
        boolean hasReviewRequired = false;
        boolean hasUndecidable = false;
        boolean hasUnsupported = false;
        boolean allNotApplicable = true;
        for (EvaluationResult r : results) {
            if (r.state() != EvaluationState.NOT_APPLICABLE) allNotApplicable = false;
            if (r.state() == EvaluationState.NON_CONFORMANT) hasNonConformant = true;
            else if (r.state() == EvaluationState.REVIEW_REQUIRED) hasReviewRequired = true;
            else if (r.state() == EvaluationState.UNDECIDABLE) hasUndecidable = true;
            else if (r.state() == EvaluationState.UNSUPPORTED) hasUnsupported = true;
        }
        if (allNotApplicable) return new ConformanceResult(results, EvaluationState.NOT_APPLICABLE);
        EvaluationState agg;
        if (hasNonConformant) agg = EvaluationState.NON_CONFORMANT;
        else if (hasReviewRequired) agg = EvaluationState.REVIEW_REQUIRED;
        else if (hasUndecidable) agg = EvaluationState.UNDECIDABLE;
        else if (hasUnsupported) agg = EvaluationState.UNSUPPORTED;
        else agg = EvaluationState.CONFORMANT;
        // Sort results by ruleId for determinism
        List<EvaluationResult> sorted = new java.util.ArrayList<>(results);
        sorted.sort((a,b) -> a.ruleId().compareTo(b.ruleId()));
        return new ConformanceResult(java.util.Collections.unmodifiableList(sorted), agg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConformanceResult)) return false;
        ConformanceResult that = (ConformanceResult) o;
        return results.equals(that.results) && aggregateState == that.aggregateState;
    }

    @Override
    public int hashCode() { return Objects.hash(results, aggregateState); }
}

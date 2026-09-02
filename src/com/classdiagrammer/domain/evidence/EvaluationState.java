package com.classdiagrammer.domain.evidence;

/**
 * Evaluation states per RULE-002-U12.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum EvaluationState {
    CONFORMANT,
    NON_CONFORMANT,
    NOT_APPLICABLE,
    UNDECIDABLE,
    UNSUPPORTED,
    REVIEW_REQUIRED;

    public String jsonName() {
        return name().toLowerCase();
    }
}

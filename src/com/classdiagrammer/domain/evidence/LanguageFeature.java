package com.classdiagrammer.domain.evidence;

/**
 * Language features whose availability is version-dependent.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum LanguageFeature {
    RECORD("record", "RECORD"),
    SEALED_TYPE("sealed", "SEALED_TYPE"),
    NON_SEALED_TYPE("non-sealed", "NON_SEALED_TYPE"),
    TEXT_BLOCK("text-block", "TEXT_BLOCK"),
    PATTERN_MATCHING("pattern-matching", "PATTERN_MATCHING"),
    SWITCH_EXPRESSION("switch-expression", "SWITCH_EXPRESSION"),
    LOCAL_VARIABLE_TYPE_INFERENCE("var", "LOCAL_VARIABLE_TYPE_INFERENCE"),
    VIRTUAL_THREAD("virtual-thread", "VIRTUAL_THREAD");

    private final String token;
    private final String factValue;

    LanguageFeature(String token, String factValue) {
        this.token = token;
        this.factValue = factValue;
    }

    public String token() { return token; }
    public String factValue() { return factValue; }

    public String jsonName() { return token; }
}

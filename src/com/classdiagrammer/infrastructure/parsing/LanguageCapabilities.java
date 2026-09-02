package com.classdiagrammer.infrastructure.parsing;

/**
 * Describes language capabilities enabled per Java version.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public record LanguageCapabilities(
        boolean textBlocks,
        boolean records,
        boolean sealedTypes,
        boolean permitsClause,
        boolean patternMatching,
        boolean switchExpression,
        boolean localVariableTypeInference,
        boolean virtualThread) {

    public static LanguageCapabilities forVersion(JavaVersion version) {
        return switch (version) {
            case V8 -> new LanguageCapabilities(false, false, false, false, false, false, false, false);
            case V11 -> new LanguageCapabilities(false, false, false, false, false, false, true, false);
            case V17 -> new LanguageCapabilities(true, true, true, true, true, true, true, true);
            case V21 -> new LanguageCapabilities(true, true, true, true, true, true, true, true);
            case V25 -> new LanguageCapabilities(true, true, true, true, true, true, true, true);
        };
    }
}

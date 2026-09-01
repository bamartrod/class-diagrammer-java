package com.classdiagrammer.domain.evidence;

import java.util.Objects;

/**
 * Thrown when source uses a language feature not available in the selected Java version.
 * Maps to UNSUPPORTED evaluation per CSAS-002-U12.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class UnsupportedLanguageFeatureException extends RuntimeException {

    private final LanguageFeature feature;
    private final String javaVersion;
    private final String sourceFile;

    public UnsupportedLanguageFeatureException(LanguageFeature feature, String javaVersion, String sourceFile) {
        super("unsupported feature " + feature.factValue() + " for Java " + javaVersion + " in " + sourceFile);
        this.feature = Objects.requireNonNull(feature);
        this.javaVersion = javaVersion == null ? "" : javaVersion;
        this.sourceFile = sourceFile == null ? "" : sourceFile;
    }

    public LanguageFeature feature() { return feature; }
    public String javaVersion() { return javaVersion; }
    public String sourceFile() { return sourceFile; }

    public ImplementationFact toFact() {
        return new ImplementationFact(
                FactKind.LANGUAGE_FEATURE_USAGE,
                sourceFile,
                sourceFile + ":1",
                feature.factValue(),
                "CSAS-007-U1"
        );
    }

    public Evidence toEvidence() {
        ImplementationFact availability = new ImplementationFact(
                FactKind.LANGUAGE_FEATURE_AVAILABILITY,
                feature.factValue(),
                "JavaVersion:" + javaVersion,
                "unavailable",
                "CSAS-007-U1"
        );
        return new Evidence(availability, sourceFile, "LanguageCapabilities.forVersion(" + javaVersion + ")", "EVID-" + feature.factValue() + "-" + sourceFile.hashCode());
    }
}

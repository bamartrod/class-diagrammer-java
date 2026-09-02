package com.classdiagrammer.domain.evidence;

import java.util.Objects;

/**
 * Reproducible attributable record that a fact holds at an addressable location (RULE-002-U5).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Evidence {

    private final ImplementationFact fact;
    private final String sourceFile;
    private final String derivation;
    private final String evidenceId;
    private final String locator;

    public Evidence(ImplementationFact fact, String sourceFile, String derivation, String evidenceId) {
        this.fact = Objects.requireNonNull(fact, "fact is required");
        this.sourceFile = sourceFile == null ? "" : sourceFile.trim();
        this.derivation = derivation == null ? "" : derivation.trim();
        this.evidenceId = evidenceId == null ? "" : evidenceId.trim();
        this.locator = fact.locator();
        if (this.sourceFile.isEmpty()) throw new IllegalArgumentException("sourceFile is required");
    }

    public ImplementationFact fact() { return fact; }
    public String sourceFile() { return sourceFile; }
    public String derivation() { return derivation; }
    public String evidenceId() { return evidenceId; }
    public String locator() { return locator; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Evidence that)) return false;
        return fact.equals(that.fact) && sourceFile.equals(that.sourceFile);
    }

    @Override
    public int hashCode() { return Objects.hash(fact, sourceFile); }

    @Override
    public String toString() { return "Evidence[" + evidenceId + "] " + fact + " via " + derivation; }
}

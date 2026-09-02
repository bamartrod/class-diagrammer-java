package com.classdiagrammer.domain.evidence;

import java.util.Objects;

/**
 * Observable, addressable property without author intent (RULE-002-U1).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ImplementationFact {

    private final FactKind kind;
    private final String subject;
    private final String locator;
    private final String value;
    private final String ruleId;

    public ImplementationFact(FactKind kind, String subject, String locator, String value, String ruleId) {
        this.kind = Objects.requireNonNull(kind, "fact kind is required");
        this.subject = subject == null ? "" : subject.trim();
        this.locator = locator == null ? "" : locator.trim();
        this.value = value == null ? "" : value.trim();
        this.ruleId = ruleId == null ? "" : ruleId.trim();
        if (this.subject.isEmpty()) throw new IllegalArgumentException("fact subject is required");
        if (this.locator.isEmpty()) throw new IllegalArgumentException("fact locator is required");
    }

    public FactKind kind() { return kind; }
    public String subject() { return subject; }
    public String locator() { return locator; }
    public String value() { return value; }
    public String ruleId() { return ruleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImplementationFact that)) return false;
        return kind == that.kind && subject.equals(that.subject) && locator.equals(that.locator);
    }

    @Override
    public int hashCode() { return Objects.hash(kind, subject, locator); }

    @Override
    public String toString() { return kind + ":" + subject + "@" + locator + "=" + value; }
}

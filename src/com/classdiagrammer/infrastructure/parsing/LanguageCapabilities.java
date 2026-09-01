package com.classdiagrammer.infrastructure.parsing;

import java.util.Objects;

public final class LanguageCapabilities {

    private final boolean textBlocks;
    private final boolean records;
    private final boolean sealedTypes;
    private final boolean permitsClause;
    private final boolean patternMatching;

    public LanguageCapabilities(boolean textBlocks, boolean records, boolean sealedTypes, boolean permitsClause, boolean patternMatching) {
        this.textBlocks = textBlocks;
        this.records = records;
        this.sealedTypes = sealedTypes;
        this.permitsClause = permitsClause;
        this.patternMatching = patternMatching;
    }

    public boolean textBlocks() { return textBlocks; }
    public boolean records() { return records; }
    public boolean sealedTypes() { return sealedTypes; }
    public boolean permitsClause() { return permitsClause; }
    public boolean patternMatching() { return patternMatching; }

    public static LanguageCapabilities forVersion(JavaVersion version) {
        if (version == null) throw new IllegalArgumentException("version is required");
        switch (version) {
            case V8: return new LanguageCapabilities(false, false, false, false, false);
            case V11: return new LanguageCapabilities(false, false, false, false, false);
            case V17: return new LanguageCapabilities(true, true, true, true, true);
            case V21: return new LanguageCapabilities(true, true, true, true, true);
            case V25: return new LanguageCapabilities(true, true, true, true, true);
            default: throw new IllegalArgumentException("unknown version: " + version);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageCapabilities)) return false;
        LanguageCapabilities that = (LanguageCapabilities) o;
        return textBlocks == that.textBlocks && records == that.records && sealedTypes == that.sealedTypes && permitsClause == that.permitsClause && patternMatching == that.patternMatching;
    }

    @Override
    public int hashCode() { return Objects.hash(textBlocks, records, sealedTypes, permitsClause, patternMatching); }
}

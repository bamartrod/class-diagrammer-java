package com.classdiagrammer.infrastructure.parsing;

/**
 * Enumeration of supported Java versions (8, 11, 17, 21, 26).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum JavaVersion {
    V8("8"),
    V11("11"),
    V17("17"),
    V21("21"),
    V26("26");

    private final String label;

    JavaVersion(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static JavaVersion from(String raw) {
        if (raw == null) {
            return V8;
        }
        String value = raw.trim();
        for (JavaVersion version : values()) {
            if (version.label.equals(value)) {
                return version;
            }
        }
        throw new IllegalArgumentException(
                "unsupported java version: " + raw + " (use 8, 11, 17, 21, 26)");
    }
}

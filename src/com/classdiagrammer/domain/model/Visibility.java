package com.classdiagrammer.domain.model;

import java.util.Set;

public enum Visibility {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    PACKAGE_PRIVATE("");

    private final String keyword;

    Visibility(String keyword) {
        this.keyword = keyword;
    }

    public static Visibility fromKeywords(Set<String> keywords) {
        for (Visibility candidate : values()) {
            if (!candidate.keyword.isEmpty() && keywords.contains(candidate.keyword)) {
                return candidate;
            }
        }
        return PACKAGE_PRIVATE;
    }

    public String keyword() {
        return keyword;
    }

    public String jsonName() {
        return this == PACKAGE_PRIVATE ? "package_private" : name().toLowerCase();
    }
}

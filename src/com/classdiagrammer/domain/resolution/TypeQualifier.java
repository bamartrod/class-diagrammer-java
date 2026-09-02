package com.classdiagrammer.domain.resolution;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves simple names to fully-qualified names using imports.
 * Separate from dependency resolution per RULE-002-U16.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class TypeQualifier {

    private TypeQualifier() {}

    public static String qualify(String fromNode, String rawTarget,
                                 Map<String, List<String>> importsByNode) {
        Objects.requireNonNull(fromNode, "fromNode is required");
        Objects.requireNonNull(rawTarget, "rawTarget is required");
        Objects.requireNonNull(importsByNode, "importsByNode is required");
        if (rawTarget.indexOf('.') >= 0 || rawTarget.contains("/")) {
            return rawTarget;
        }
        List<String> imports = importsByNode.get(fromNode);
        if (imports == null) {
            return rawTarget;
        }
        for (String imported : imports) {
            if (imported.endsWith("." + rawTarget)) {
                return imported;
            }
        }
        return rawTarget;
    }
}

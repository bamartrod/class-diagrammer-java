package com.classdiagrammer.infrastructure.parsing.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves which class-level imports are required by a specific member's type usage.
 * A member's required imports are those class imports whose simple name appears in the member's type signature.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class ImportResolver {

    private static final Pattern TYPE_TOKEN = Pattern.compile("[A-Za-z_$][\\w$]*");
    private static final Set<String> PRIMITIVES = Set.of(
            "byte", "short", "int", "long", "float", "double", "boolean", "char", "void"
    );
    private static final Set<String> JAVA_LANG = Set.of(
            "String", "Object", "Integer", "Long", "Double", "Float", "Boolean", "Character",
            "Byte", "Short", "Void", "StringBuilder", "StringBuffer", "Exception", "RuntimeException",
            "Throwable", "Class", "System", "Math", "Enum", "Override", "Deprecated", "SuppressWarnings"
    );

    private ImportResolver() {}

    static List<String> requiredImports(String typeSignature, List<String> classImports) {
        if (typeSignature == null || typeSignature.trim().isEmpty() || classImports == null || classImports.isEmpty()) {
            return List.of();
        }
        Set<String> typeTokens = extractTypeTokens(typeSignature);
        if (typeTokens.isEmpty()) return List.of();

        List<String> required = new ArrayList<>();
        for (String imported : classImports) {
            if (imported == null || imported.trim().isEmpty()) continue;
            // ignore wildcard and static and java.* imports per current edge semantics (but keep for member traceability if needed)
            if (imported.endsWith(".*")) continue;
            if (imported.startsWith("java.") || imported.startsWith("javax.")) continue;
            // import is fully qualified, e.g., com.tienda.Cliente
            int lastDot = imported.lastIndexOf('.');
            if (lastDot < 0) continue;
            String simple = imported.substring(lastDot + 1);
            if (typeTokens.contains(simple)) {
                required.add(imported);
            }
        }
        // deduplicate and sort for determinism
        Set<String> dedup = new HashSet<>(required);
        List<String> sorted = new ArrayList<>(dedup);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    static List<String> requiredImportsForMethod(String returnType, List<String> paramTypes, List<String> classImports) {
        Set<String> tokens = new HashSet<>();
        if (returnType != null && !returnType.trim().isEmpty()) {
            tokens.addAll(extractTypeTokens(returnType));
        }
        for (String pt : paramTypes) {
            if (pt != null) tokens.addAll(extractTypeTokens(pt));
        }
        if (tokens.isEmpty()) return List.of();
        List<String> required = new ArrayList<>();
        for (String imported : classImports) {
            if (imported == null || imported.trim().isEmpty()) continue;
            if (imported.endsWith(".*")) continue;
            if (imported.startsWith("java.") || imported.startsWith("javax.")) continue;
            int lastDot = imported.lastIndexOf('.');
            if (lastDot < 0) continue;
            String simple = imported.substring(lastDot + 1);
            if (tokens.contains(simple)) required.add(imported);
        }
        Set<String> dedup = new HashSet<>(required);
        List<String> sorted = new ArrayList<>(dedup);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    private static Set<String> extractTypeTokens(String type) {
        Set<String> tokens = new HashSet<>();
        Matcher m = TYPE_TOKEN.matcher(type);
        while (m.find()) {
            String tok = m.group();
            if (PRIMITIVES.contains(tok)) continue;
            // skip java.lang types unless explicitly imported? We treat them as not requiring import
            if (JAVA_LANG.contains(tok)) continue;
            // skip lowercase tokens that are not types (e.g., words in generics? but our regex only finds identifiers)
            // Keep only those starting with uppercase (convention for types)
            if (tok.length() > 0 && Character.isUpperCase(tok.charAt(0))) {
                tokens.add(tok);
            }
        }
        return tokens;
    }
}

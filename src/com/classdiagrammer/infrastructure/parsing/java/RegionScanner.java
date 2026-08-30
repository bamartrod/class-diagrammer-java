package com.classdiagrammer.infrastructure.parsing.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper RegionScanner supporting the Java parser pipeline.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class RegionScanner {

    interface TypeConsumer {
        void accept(TypeDeclaration declaration, int bodyStart, int bodyEnd, String qualifier);
    }

    private static final Pattern TYPE_DECLARATION = Pattern.compile(
            "(?<![\\w$.])((?:(?:public|protected|private|abstract|static|final|strictfp|sealed|non-sealed)\\s+)*)"
                    + "(@\\s*interface|class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)");

    void scan(String masked, int from, int to, String qualifier, TypeConsumer consumer) {
        Matcher matcher = TYPE_DECLARATION.matcher(masked);
        int cursor = Math.max(from, 0);
        while (cursor <= to && matcher.find(cursor)) {
            if (matcher.start() > to) {
                break;
            }
            int afterName = matcher.end();
            Set<String> modifiers = extractModifiers(matcher.group(1));
            String kindToken = matcher.group(2).replaceAll("\\s+", "");
            String name = matcher.group(3);

            int genericsEnd = skipGenerics(masked, afterName, to);
            int braceIndex = indexOfBrace(masked, genericsEnd, to);
            if (braceIndex < 0) {
                cursor = afterName;
                continue;
            }
            int bodyEnd = TokenOps.matchingBrace(masked, braceIndex, to);
            if (bodyEnd < 0) {
                cursor = afterName;
                continue;
            }
            String header = masked.substring(genericsEnd, braceIndex).trim();
            TypeDeclaration declaration =
                    new TypeDeclaration(modifiers, kindToken, name, header);
            consumer.accept(declaration, braceIndex, bodyEnd, qualifier);

            String nestedQualifier = qualifier.isEmpty() ? name : qualifier + "." + name;
            scan(masked, braceIndex + 1, bodyEnd, nestedQualifier, consumer);
            cursor = bodyEnd + 1;
        }
    }

    private Set<String> extractModifiers(String rawModifiers) {
        Set<String> found = new LinkedHashSet<>();
        if (rawModifiers != null && !rawModifiers.trim().isEmpty()) {
            found.addAll(Arrays.asList(rawModifiers.trim().split("\\s+")));
        }
        return found;
    }

    private int skipGenerics(String masked, int start, int limit) {
        int i = start;
        while (i < limit && Character.isWhitespace(masked.charAt(i))) {
            i++;
        }
        if (i >= limit || masked.charAt(i) != '<') {
            return start;
        }
        int depth = 0;
        while (i < limit) {
            char c = masked.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            } else if (c == ';' || c == '{') {
                return start;
            }
            i++;
        }
        return start;
    }

    private int indexOfBrace(String masked, int start, int limit) {
        for (int i = start; i < limit; i++) {
            char c = masked.charAt(i);
            if (c == '{') {
                return i;
            }
            if (c == ';') {
                return -1;
            }
        }
        return -1;
    }

    static List<String> splitClauseList(String clauseBody) {
        List<String> entries = new ArrayList<>();
        for (String part : TokenOps.splitTopLevel(clauseBody, ',')) {
            String cleaned = part.replaceAll("\\s*<.*>", "").trim();
            if (!cleaned.isEmpty() && !cleaned.contains(";")) {
                entries.add(cleaned);
            }
        }
        return entries;
    }
}

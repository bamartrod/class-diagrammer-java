package com.classdiagrammer.infrastructure.parsing.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper HeaderParser supporting the Java parser pipeline.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class HeaderParser {

    private static final Pattern EXTENDS_CLAUSE = Pattern.compile("\\bextends\\b");
    private static final Pattern IMPLEMENTS_CLAUSE = Pattern.compile("\\bimplements\\b");
    private static final Pattern PERMITS_CLAUSE = Pattern.compile("\\bpermits\\b");

    static final class ParsedHeader {

        private final List<String> extendsTypes;
        private final List<String> implementsTypes;
        private final List<String> permitsTypes;

        ParsedHeader(List<String> extendsTypes, List<String> implementsTypes, List<String> permitsTypes) {
            this.extendsTypes = Collections.unmodifiableList(new ArrayList<>(extendsTypes));
            this.implementsTypes = Collections.unmodifiableList(new ArrayList<>(implementsTypes));
            this.permitsTypes = Collections.unmodifiableList(new ArrayList<>(permitsTypes));
        }

        List<String> extendsTypes() {
            return extendsTypes;
        }

        List<String> implementsTypes() {
            return implementsTypes;
        }

        List<String> permitsTypes() {
            return permitsTypes;
        }
    }

    ParsedHeader parse(String header) {
        int extendsIndex = indexOfClause(EXTENDS_CLAUSE, header);
        int implementsIndex = indexOfClause(IMPLEMENTS_CLAUSE, header);
        int permitsIndex = indexOfClause(PERMITS_CLAUSE, header);

        List<String> extendsTypes = new ArrayList<>();
        List<String> implementsTypes = new ArrayList<>();
        List<String> permitsTypes = new ArrayList<>();

        if (extendsIndex >= 0) {
            String body = segmentAfter(header, extendsIndex,
                    firstPositive(implementsIndex, permitsIndex));
            extendsTypes.addAll(RegionScanner.splitClauseList(body));
        }
        if (implementsIndex >= 0) {
            String body = segmentAfter(header, implementsIndex, permitsIndex);
            implementsTypes.addAll(RegionScanner.splitClauseList(body));
        }
        if (permitsIndex >= 0) {
            String body = segmentAfter(header, permitsIndex, -1);
            permitsTypes.addAll(RegionScanner.splitClauseList(body));
        }
        return new ParsedHeader(extendsTypes, implementsTypes, permitsTypes);
    }

    private int firstPositive(int first, int second) {
        if (first >= 0 && second >= 0) {
            return Math.min(first, second);
        }
        return Math.max(first, second);
    }

    private int indexOfClause(Pattern clause, String header) {
        Matcher matcher = clause.matcher(header);
        return matcher.find() ? matcher.start() : -1;
    }

    private String segmentAfter(String header, int clauseIndex, int endIndex) {
        int start = clauseIndex;
        while (start < header.length() && !Character.isWhitespace(header.charAt(start))) {
            start++;
        }
        int end = endIndex < 0 || endIndex > header.length() ? header.length() : endIndex;
        if (end <= start) {
            return "";
        }
        return header.substring(start, end);
    }
}

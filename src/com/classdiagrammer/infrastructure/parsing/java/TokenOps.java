package com.classdiagrammer.infrastructure.parsing.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TokenOps {

    private static final Pattern ANNOTATION =
            Pattern.compile("@[\\w.$]+\\s*(?:\\((?:[^()]|\\([^()]*\\))*\\))?\\s*");

    static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList(
            "public", "private", "protected", "static", "final", "abstract",
            "synchronized", "native", "default", "transient", "volatile", "strictfp"));

    private TokenOps() {
    }

    static List<String> splitWords(String segment) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int paren = 0;
        int bracket = 0;
        int angle = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '(') {
                paren++;
            } else if (c == ')') {
                paren--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            } else if (c == '<') {
                angle++;
            } else if (c == '>' && angle > 0) {
                angle--;
            }
            current.append(c);
            boolean separator = Character.isWhitespace(c);
            if (separator && paren == 0 && bracket == 0 && angle == 0) {
                flush(tokens, current);
            }
        }
        flush(tokens, current);
        return tokens;
    }

    private static void flush(List<String> tokens, StringBuilder current) {
        String token = current.toString().trim();
        if (!token.isEmpty()) {
            tokens.add(token);
        }
        current.setLength(0);
    }

    static List<String> splitTopLevel(String segment, char separator) {
        List<String> parts = new ArrayList<>();
        int paren = 0;
        int bracket = 0;
        int angle = 0;
        int brace = 0;
        int start = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '(') {
                paren++;
            } else if (c == ')') {
                paren--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            } else if (c == '<') {
                angle++;
            } else if (c == '>' && angle > 0) {
                angle--;
            } else if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
            }
            boolean splitHere = c == separator && paren == 0 && bracket == 0 && angle == 0 && brace == 0;
            if (splitHere) {
                addPart(parts, segment.substring(start, i));
                start = i + 1;
            }
        }
        addPart(parts, segment.substring(start));
        return parts;
    }

    private static void addPart(List<String> parts, String raw) {
        String trimmed = raw.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }

    static String stripLeadingAnnotations(String chunk) {
        String rest = chunk.trim();
        while (!rest.isEmpty()) {
            Matcher matcher = ANNOTATION.matcher(rest);
            if (matcher.lookingAt()) {
                rest = rest.substring(matcher.end()).trim();
            } else {
                break;
            }
        }
        return rest;
    }

    static int topLevelEquals(String segment) {
        int paren = 0;
        int bracket = 0;
        int angle = 0;
        int brace = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '(') {
                paren++;
            } else if (c == ')') {
                paren--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket--;
            } else if (c == '<') {
                angle++;
            } else if (c == '>' && angle > 0) {
                angle--;
            } else if (c == '{') {
                brace++;
            } else if (c == '}') {
                brace--;
            } else if (c == '=' && paren == 0 && bracket == 0 && angle == 0 && brace == 0) {
                if (i + 1 < segment.length() && segment.charAt(i + 1) == '=') {
                    return -1;
                }
                return i;
            }
        }
        return -1;
    }

    static int matchingParen(String segment, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static int matchingBrace(String segment, int openIndex, int limit) {
        int depth = 0;
        for (int i = openIndex; i < limit && i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static boolean isModifier(String word) {
        return MODIFIERS.contains(word);
    }

    static boolean isIdentifier(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(token.charAt(0))) {
            return false;
        }
        for (int i = 1; i < token.length(); i++) {
            if (!Character.isJavaIdentifierPart(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

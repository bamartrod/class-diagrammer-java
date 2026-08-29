package com.classdiagrammer.infrastructure.xml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class XmlTagScanner {

    public static final class Tag {
        public final String name;
        public final Map<String, String> attributes;
        public final boolean selfClosing;

        public Tag(String name, Map<String, String> attributes, boolean selfClosing) {
            this.name = name;
            this.attributes = Collections.unmodifiableMap(attributes);
            this.selfClosing = selfClosing;
        }
    }

    public interface Listener {
        void tag(Tag tag);
    }

    private XmlTagScanner() {
    }

    public static void scan(String content, Listener listener) {
        int index = 0;
        while (index < content.length()) {
            char current = content.charAt(index);
            if (current == '<') {
                if (startsMarkupDeclaration(content, index)) {
                    index = skipMarkupDeclaration(content, index);
                    continue;
                }
                if (index + 1 < content.length() && content.charAt(index + 1) == '?') {
                    index = skipUntil(content, index + 2, "?>");
                    continue;
                }
                if (index + 1 < content.length() && content.charAt(index + 1) == '/') {
                    index = skipUntil(content, index + 2, ">");
                    continue;
                }
                index = readTag(content, index, listener);
                continue;
            }
            index++;
        }
    }

    private static boolean startsMarkupDeclaration(String content, int openIndex) {
        if (openIndex + 3 < content.length() && content.startsWith("<!--", openIndex)) {
            return true;
        }
        return openIndex + 8 < content.length()
                && content.startsWith("<![CDATA[", openIndex);
    }

    private static int skipMarkupDeclaration(String content, int openIndex) {
        if (content.startsWith("<!--", openIndex)) {
            int close = content.indexOf("-->", openIndex + 4);
            return close < 0 ? content.length() : close + 3;
        }
        int close = content.indexOf("]]>", openIndex + 9);
        return close < 0 ? content.length() : close + 3;
    }

    private static int skipUntil(String content, int from, String terminator) {
        int close = content.indexOf(terminator, from);
        return close < 0 ? content.length() : close + terminator.length();
    }

    private static int readTag(String content, int openIndex, Listener listener) {
        int cursor = openIndex + 1;
        int nameStart = cursor;
        while (cursor < content.length() && !Character.isWhitespace(content.charAt(cursor))
                && content.charAt(cursor) != '>'
                && content.charAt(cursor) != '/') {
            cursor++;
        }
        String name = content.substring(nameStart, cursor);
        Map<String, String> attributes = new LinkedHashMap<>();
        boolean selfClosing = false;
        while (cursor < content.length()) {
            char current = content.charAt(cursor);
            if (current == '>') {
                break;
            }
            if (current == '/') {
                selfClosing = true;
                cursor++;
                continue;
            }
            if (Character.isWhitespace(current)) {
                cursor++;
                continue;
            }
            int[] next = readAttribute(content, cursor, attributes);
            if (next[0] == cursor) {
                cursor++;
            } else {
                cursor = next[0];
            }
        }
        if (!name.isEmpty() && Character.isLetter(name.charAt(0))) {
            listener.tag(new Tag(name, attributes, selfClosing));
        }
        return Math.min(cursor + 1, content.length());
    }

    private static int[] readAttribute(String content, int start, Map<String, String> into) {
        int cursor = start;
        while (cursor < content.length() && !Character.isWhitespace(content.charAt(cursor))
                && content.charAt(cursor) != '='
                && content.charAt(cursor) != '>'
                && content.charAt(cursor) != '/') {
            cursor++;
        }
        String attributeName = content.substring(start, cursor).toLowerCase();
        int probe = cursor;
        while (probe < content.length() && Character.isWhitespace(content.charAt(probe))) {
            probe++;
        }
        if (probe >= content.length() || content.charAt(probe) != '=') {
            if (!attributeName.isEmpty()) {
                into.put(attributeName, "");
            }
            return new int[]{cursor};
        }
        probe++;
        while (probe < content.length() && Character.isWhitespace(content.charAt(probe))) {
            probe++;
        }
        if (probe < content.length()
                && (content.charAt(probe) == '"' || content.charAt(probe) == '\'')) {
            char quote = content.charAt(probe);
            int valueStart = probe + 1;
            int valueEnd = valueStart;
            while (valueEnd < content.length() && content.charAt(valueEnd) != quote) {
                valueEnd++;
            }
            into.put(attributeName, content.substring(valueStart,
                    Math.min(valueEnd, content.length())));
            return new int[]{Math.min(valueEnd + 1, content.length())};
        }
        int valueStart = probe;
        while (probe < content.length() && !Character.isWhitespace(content.charAt(probe))
                && content.charAt(probe) != '>') {
            probe++;
        }
        into.put(attributeName, content.substring(valueStart, probe));
        return new int[]{probe};
    }

    public static String localNameOf(String qualifiedName) {
        int colon = qualifiedName.indexOf(':');
        return colon < 0 ? qualifiedName : qualifiedName.substring(colon + 1);
    }

    public static String prefixOf(String qualifiedName) {
        int colon = qualifiedName.indexOf(':');
        return colon < 0 ? "" : qualifiedName.substring(0, colon);
    }
}

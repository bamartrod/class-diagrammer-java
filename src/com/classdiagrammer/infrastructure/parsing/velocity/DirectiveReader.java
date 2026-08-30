package com.classdiagrammer.infrastructure.parsing.velocity;

/**
 * Infrastructure parsing component DirectiveReader.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class DirectiveReader {

    private DirectiveReader() {
    }

    static Directive read(String text, int start) {
        int nameEnd = start + 1;
        while (nameEnd < text.length() && Character.isLetter(text.charAt(nameEnd))) {
            nameEnd++;
        }
        String keyword = text.substring(start + 1, nameEnd);
        int cursor = skipSpaces(text, nameEnd);
        int bodyStart = cursor;
        int bodyEnd;
        if (cursor < text.length() && text.charAt(cursor) == '(') {
            bodyStart++;
            cursor++;
            int depth = 0;
            while (cursor < text.length()) {
                char current = text.charAt(cursor);
                if (current == '"' || current == '\'') {
                    cursor = skipQuoted(text, cursor);
                    continue;
                }
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    if (depth == 0) {
                        break;
                    }
                    depth--;
                }
                cursor++;
            }
            bodyEnd = cursor;
            cursor = Math.min(cursor + 1, text.length());
        } else {
            while (cursor < text.length() && text.charAt(cursor) != '\n') {
                if (text.charAt(cursor) == '"' || text.charAt(cursor) == '\'') {
                    cursor = skipQuoted(text, cursor);
                } else {
                    cursor++;
                }
            }
            bodyEnd = cursor;
        }
        return new Directive(keyword, text.substring(bodyStart, bodyEnd), cursor);
    }

    static int skipQuoted(String text, int openQuoteIndex) {
        char quote = text.charAt(openQuoteIndex);
        int index = openQuoteIndex + 1;
        while (index < text.length()) {
            if (text.charAt(index) == '\\' && index + 1 < text.length()) {
                index += 2;
                continue;
            }
            if (text.charAt(index) == quote) {
                return index + 1;
            }
            index++;
        }
        return index;
    }

    static final class Directive {
        private final String keyword;
        private final String body;
        private final int end;

        private Directive(String keyword, String body, int end) {
            this.keyword = keyword;
            this.body = body;
            this.end = end;
        }

        String keyword() {
            return keyword;
        }

        String body() {
            return body;
        }

        int end() {
            return end;
        }
    }

    private static int skipSpaces(String text, int from) {
        int index = from;
        while (index < text.length() && (text.charAt(index) == ' '
                || text.charAt(index) == '\t')) {
            index++;
        }
        return index;
    }
}

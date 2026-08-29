package com.classdiagrammer.infrastructure.parsing.java;

public final class SourceText {

    private static final int CODE = 0;
    private static final int LINE_COMMENT = 1;
    private static final int BLOCK_COMMENT = 2;
    private static final int IN_STRING = 3;
    private static final int IN_CHAR = 4;
    private static final int TEXT_BLOCK = 5;

    private final String raw;
    private final String masked;

    private SourceText(String raw, String masked) {
        this.raw = raw;
        this.masked = masked;
    }

    public static SourceText of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("source content is required");
        }
        StringBuilder out = new StringBuilder(raw.length());
        int state = CODE;
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            char next = i + 1 < raw.length() ? raw.charAt(i + 1) : '\0';
            switch (state) {
                case CODE:
                    if (c == '/' && next == '/') {
                        out.append("  ");
                        state = LINE_COMMENT;
                        i += 2;
                    } else if (c == '/' && next == '*') {
                        out.append("  ");
                        state = BLOCK_COMMENT;
                        i += 2;
                    } else if (c == '"' && next == '"' && i + 2 < raw.length() && raw.charAt(i + 2) == '"') {
                        out.append("\"\"\"");
                        state = TEXT_BLOCK;
                        i += 3;
                    } else if (c == '"') {
                        out.append(c);
                        state = IN_STRING;
                        i++;
                    } else if (c == '\'') {
                        out.append(c);
                        state = IN_CHAR;
                        i++;
                    } else {
                        out.append(c);
                        i++;
                    }
                    break;
                case LINE_COMMENT:
                    if (c == '\n') {
                        out.append('\n');
                        state = CODE;
                    } else {
                        out.append(' ');
                    }
                    i++;
                    break;
                case BLOCK_COMMENT:
                    if (c == '*' && next == '/') {
                        out.append("  ");
                        state = CODE;
                        i += 2;
                    } else {
                        out.append(c == '\n' ? '\n' : ' ');
                        i++;
                    }
                    break;
                case IN_STRING:
                case IN_CHAR:
                    char quote = state == IN_STRING ? '"' : '\'';
                    if (c == '\\') {
                        out.append("  ");
                        i += 2;
                    } else if (c == quote) {
                        out.append(c);
                        state = CODE;
                        i++;
                    } else {
                        out.append(c == '\n' ? '\n' : ' ');
                        i++;
                    }
                    break;
                case TEXT_BLOCK:
                    if (c == '"' && next == '"' && i + 2 < raw.length() && raw.charAt(i + 2) == '"') {
                        out.append("\"\"\"");
                        state = CODE;
                        i += 3;
                    } else if (c == '\\') {
                        out.append(raw.charAt(i) == '\n' ? '\n' : ' ');
                        if (i + 1 < raw.length()) {
                            out.append(raw.charAt(i + 1) == '\n' ? '\n' : ' ');
                            i += 2;
                        } else {
                            i++;
                        }
                    } else {
                        out.append(c == '\n' ? '\n' : ' ');
                        i++;
                    }
                    break;
                default:
                    out.append(c);
                    i++;
            }
        }
        return new SourceText(raw, out.toString());
    }

    public String raw() {
        return raw;
    }

    public String masked() {
        return masked;
    }
}

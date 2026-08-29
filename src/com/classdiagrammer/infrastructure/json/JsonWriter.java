package com.classdiagrammer.infrastructure.json;

import java.util.ArrayDeque;
import java.util.Deque;

public final class JsonWriter {

    private static final String INDENT = "  ";

    private final StringBuilder out;
    private final Deque<Frame> frames = new ArrayDeque<>();
    private int depth = 0;

    private static final class Frame {
        final boolean array;
        boolean empty = true;

        Frame(boolean array) {
            this.array = array;
        }
    }

    public JsonWriter(StringBuilder out) {
        if (out == null) {
            throw new IllegalArgumentException("string builder is required");
        }
        this.out = out;
    }

    public JsonWriter beginObject() {
        preValue();
        openContainer(false);
        return this;
    }

    public JsonWriter beginObject(String key) {
        writeKey(key);
        openContainer(false);
        return this;
    }

    public JsonWriter beginArray(String key) {
        writeKey(key);
        openContainer(true);
        return this;
    }

    public JsonWriter endObject() {
        closeContainer('}');
        return this;
    }

    public JsonWriter endArray() {
        closeContainer(']');
        return this;
    }

    public JsonWriter field(String key, String value) {
        writeKey(key);
        out.append(quoted(value));
        return this;
    }

    public JsonWriter field(String key, int value) {
        writeKey(key);
        out.append(value);
        return this;
    }

    public JsonWriter field(String key, boolean value) {
        writeKey(key);
        out.append(value);
        return this;
    }

    public JsonWriter stringValue(String value) {
        preValue();
        out.append(quoted(value));
        return this;
    }

    private void openContainer(boolean isArray) {
        frames.push(new Frame(isArray));
        depth++;
        out.append(isArray ? '[' : '{');
    }

    private void closeContainer(char closer) {
        Frame frame = frames.pop();
        depth--;
        if (!frame.empty) {
            out.append('\n').append(indent());
        }
        out.append(closer);
    }

    private void writeKey(String key) {
        preValue();
        out.append(quoted(key)).append(": ");
    }

    private void preValue() {
        Frame frame = frames.peek();
        if (frame == null) {
            return;
        }
        if (!frame.empty) {
            out.append(',');
        }
        frame.empty = false;
        out.append('\n').append(indent());
    }

    private String indent() {
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            spaces.append(INDENT);
        }
        return spaces.toString();
    }

    private String quoted(String raw) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.append('"').toString();
    }

    public String text() {
        return out.toString();
    }
}

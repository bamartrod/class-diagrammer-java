package com.classdiagrammer.infrastructure.output;

/**
 * Supported output formats.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum OutputFormat {
    JSON("json"),
    XML("xml"),
    YAML("yaml"),
    TOON("toon");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static OutputFormat from(String raw) {
        if (raw == null) return JSON;
        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "json" -> JSON;
            case "xml" -> XML;
            case "yaml", "yml" -> YAML;
            case "toon" -> TOON;
            default -> throw new IllegalArgumentException("unsupported output format: " + raw + " (use json, xml, yaml, toon)");
        };
    }

    public static OutputFormat fromPath(String path) {
        if (path == null) return JSON;
        String lower = path.toLowerCase();
        if (lower.endsWith(".xml")) return XML;
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) return YAML;
        if (lower.endsWith(".toon")) return TOON;
        return JSON;
    }
}

package com.classdiagrammer.infrastructure.output;

import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.application.port.out.DiagramReport;
import com.classdiagrammer.domain.model.Field;
import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.Parameter;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.evidence.Evidence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * YAML output adapter — zero dependencies, YAML 1.2 compatible.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class YamlDiagramOutput implements DiagramOutput {

    public Path write(DiagramReport report, String targetPath) {
        if (report == null) throw new IllegalArgumentException("report is required");
        if (targetPath == null || targetPath.trim().isEmpty()) throw new IllegalArgumentException("target path is required");
        StringBuilder out = new StringBuilder();
        yaml(out, 0, "tool", "ClassDiagrammer");
        yaml(out, 0, "version", "2.0.0");
        yaml(out, 0, "sourceRoot", report.sourceRoot());
        yaml(out, 0, "generatedAtUtc", report.generatedAtUtc());
        yaml(out, 0, "evaluation", report.evaluation().jsonName());
        out.append("summary:\n");
        yaml(out, 1, "types", String.valueOf(report.nodes().size()));
        yaml(out, 1, "relations", String.valueOf(report.edges().size()));
        yaml(out, 1, "evidences", String.valueOf(report.evidences().size()));
        yaml(out, 1, "evaluation", report.evaluation().jsonName());

        List<TypeNode> sortedNodes = new ArrayList<>(report.nodes());
        sortedNodes.sort((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()));
        out.append("nodes:\n");
        for (TypeNode n : sortedNodes) {
            out.append("  - id: ").append(escapeYaml(n.qualifiedName())).append("\n");
            yaml(out, 2, "name", n.simpleName());
            yaml(out, 2, "kind", n.kind().jsonName());
            yaml(out, 2, "visibility", n.visibility().jsonName());
            yaml(out, 2, "package", n.packageName());
            yaml(out, 2, "folder", n.folder());
            yaml(out, 2, "file", n.file());
            yamlList(out, 2, "modifiers", n.modifiers());
            yamlList(out, 2, "imports", n.imports());
            yamlList(out, 2, "extends", n.extendsTypes());
            yamlList(out, 2, "implements", n.implementsTypes());
            yamlList(out, 2, "permits", n.permitsTypes());
            yamlFields(out, n.fields());
            yamlMethods(out, "constructors", n.constructors(), false);
            yamlMethods(out, "methods", n.methods(), true);
        }
        if (sortedNodes.isEmpty()) out.append("  []\n");

        List<com.classdiagrammer.domain.model.Edge> sortedEdges = new ArrayList<>(report.edges());
        sortedEdges.sort((a, b) -> {
            int c = a.from().compareTo(b.from());
            if (c != 0) return c;
            c = a.to().compareTo(b.to());
            if (c != 0) return c;
            return a.kind().jsonName().compareTo(b.kind().jsonName());
        });
        out.append("edges:\n");
        for (var e : sortedEdges) {
            out.append("  - from: ").append(escapeYaml(e.from())).append("\n");
            yaml(out, 2, "to", e.to());
            yaml(out, 2, "kind", e.kind().jsonName());
            yaml(out, 2, "resolved", String.valueOf(e.isResolved()));
            yaml(out, 2, "origin", e.origin().jsonName());
            if (e.artifact() != null) {
                out.append("    artifact:\n");
                yaml(out, 3, "groupId", e.artifact().groupId());
                yaml(out, 3, "artifactId", e.artifact().artifactId());
                yaml(out, 3, "version", e.artifact().version());
            }
        }
        if (sortedEdges.isEmpty()) out.append("  []\n");

        if (!report.evidences().isEmpty()) {
            out.append("evidences:\n");
            for (Evidence ev : report.evidences()) {
                out.append("  - evidenceId: ").append(escapeYaml(ev.evidenceId())).append("\n");
                yaml(out, 2, "sourceFile", ev.sourceFile());
                yaml(out, 2, "locator", ev.locator());
                yaml(out, 2, "derivation", ev.derivation());
                yaml(out, 2, "factKind", ev.fact().kind().name());
                yaml(out, 2, "subject", ev.fact().subject());
                yaml(out, 2, "value", ev.fact().value());
                yaml(out, 2, "ruleId", ev.fact().ruleId());
            }
        }

        Path target = Paths.get(targetPath);
        ensureParentExists(target);
        try {
            Files.write(target, out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("unable to write diagram to " + target, e);
        }
        return target;
    }

    private void yaml(StringBuilder out, int indent, String key, String value) {
        for (int i = 0; i < indent; i++) out.append("  ");
        out.append(key).append(": ").append(escapeYaml(value)).append("\n");
    }

    private void yamlList(StringBuilder out, int indent, String key, Iterable<String> values) {
        for (int i = 0; i < indent; i++) out.append("  ");
        out.append(key).append(":\n");
        boolean empty = true;
        for (String v : values) {
            for (int i = 0; i < indent + 1; i++) out.append("  ");
            out.append("- ").append(escapeYaml(v)).append("\n");
            empty = false;
        }
        if (empty) {
            for (int i = 0; i < indent + 1; i++) out.append("  ");
            out.append("[]\n");
        }
    }

    private void yamlFields(StringBuilder out, List<Field> fields) {
        out.append("    fields:\n");
        if (fields.isEmpty()) { out.append("      []\n"); return; }
        for (Field f : fields) {
            out.append("      - name: ").append(escapeYaml(f.name())).append("\n");
            yaml(out, 3, "type", f.type());
            yaml(out, 3, "visibility", f.visibility().jsonName());
        }
    }

    private void yamlMethods(StringBuilder out, String tag, List<Method> methods, boolean includeReturn) {
        out.append("    ").append(tag).append(":\n");
        if (methods.isEmpty()) { out.append("      []\n"); return; }
        for (Method m : methods) {
            out.append("      - name: ").append(escapeYaml(m.name())).append("\n");
            if (includeReturn) yaml(out, 3, "returnType", m.returnType());
            yaml(out, 3, "visibility", m.visibility().jsonName());
            out.append("        parameters:\n");
            if (m.parameters().isEmpty()) { out.append("          []\n"); }
            else for (Parameter p : m.parameters()) {
                out.append("          - type: ").append(escapeYaml(p.type())).append("\n");
                yaml(out, 5, "name", p.name());
            }
        }
    }

    private String escapeYaml(String raw) {
        if (raw == null) return "\"\"";
        if (raw.isEmpty()) return "\"\"";
        boolean needQuotes = raw.contains(":") || raw.contains("#") || raw.contains("-") || raw.contains("\"") || raw.contains("'") || raw.contains("\n") || raw.startsWith(" ") || raw.endsWith(" ");
        String escaped = raw.replace("\"", "\\\"");
        return needQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private void ensureParentExists(Path target) {
        Path parent = target.getParent();
        if (parent == null) return;
        try { Files.createDirectories(parent); } catch (IOException e) { throw new IllegalStateException("unable to create output folder " + parent, e); }
    }
}

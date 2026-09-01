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
 * TOON output adapter — Token-Oriented Object Notation.
 * Minimal, line-based, indentation-sensitive, token-efficient.
 * Each fact is addressable and deterministic.
 *
 * <pre>
 * tool: ClassDiagrammer
 * version: 2.0.0
 * sourceRoot: ./src
 * evaluation: conformant
 * nodes[2]:
 *   node: com.shop.domain.Order
 *     name: Order
 *     kind: class
 * </pre>
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ToonDiagramOutput implements DiagramOutput {

    public Path write(DiagramReport report, String targetPath) {
        if (report == null) throw new IllegalArgumentException("report is required");
        if (targetPath == null || targetPath.trim().isEmpty()) throw new IllegalArgumentException("target path is required");
        StringBuilder out = new StringBuilder();
        // header
        line(out, 0, "tool", "ClassDiagrammer");
        line(out, 0, "version", "2.0.0");
        line(out, 0, "sourceRoot", report.sourceRoot());
        line(out, 0, "generatedAtUtc", report.generatedAtUtc());
        line(out, 0, "evaluation", report.evaluation().jsonName());
        line(out, 0, "summary.types", String.valueOf(report.nodes().size()));
        line(out, 0, "summary.relations", String.valueOf(report.edges().size()));
        line(out, 0, "summary.evidences", String.valueOf(report.evidences().size()));

        List<TypeNode> sortedNodes = new ArrayList<>(report.nodes());
        sortedNodes.sort((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()));
        out.append("nodes[").append(sortedNodes.size()).append("]:\n");
        for (TypeNode n : sortedNodes) {
            line(out, 1, "node", n.qualifiedName());
            line(out, 2, "name", n.simpleName());
            line(out, 2, "kind", n.kind().jsonName());
            line(out, 2, "visibility", n.visibility().jsonName());
            line(out, 2, "package", n.packageName());
            line(out, 2, "folder", n.folder());
            line(out, 2, "file", n.file());
            list(out, 2, "modifiers", n.modifiers());
            list(out, 2, "imports", n.imports());
            list(out, 2, "extends", n.extendsTypes());
            list(out, 2, "implements", n.implementsTypes());
            list(out, 2, "permits", n.permitsTypes());
            fields(out, n.fields());
            methods(out, "constructors", n.constructors(), false);
            methods(out, "methods", n.methods(), true);
        }

        List<com.classdiagrammer.domain.model.Edge> sortedEdges = new ArrayList<>(report.edges());
        sortedEdges.sort((a, b) -> {
            int c = a.from().compareTo(b.from());
            if (c != 0) return c;
            c = a.to().compareTo(b.to());
            if (c != 0) return c;
            return a.kind().jsonName().compareTo(b.kind().jsonName());
        });
        out.append("edges[").append(sortedEdges.size()).append("]:\n");
        for (var e : sortedEdges) {
            line(out, 1, "edge", e.from() + "->" + e.to());
            line(out, 2, "from", e.from());
            line(out, 2, "to", e.to());
            line(out, 2, "kind", e.kind().jsonName());
            line(out, 2, "resolved", String.valueOf(e.isResolved()));
            line(out, 2, "origin", e.origin().jsonName());
            if (e.artifact() != null) {
                line(out, 2, "artifact", e.artifact().groupId() + ":" + e.artifact().artifactId() + ":" + e.artifact().version());
            }
        }

        if (!report.evidences().isEmpty()) {
            out.append("evidences[").append(report.evidences().size()).append("]:\n");
            for (Evidence ev : report.evidences()) {
                line(out, 1, "evidence", ev.evidenceId());
                line(out, 2, "sourceFile", ev.sourceFile());
                line(out, 2, "locator", ev.locator());
                line(out, 2, "derivation", ev.derivation());
                line(out, 2, "factKind", ev.fact().kind().name());
                line(out, 2, "subject", ev.fact().subject());
                line(out, 2, "value", ev.fact().value());
                line(out, 2, "ruleId", ev.fact().ruleId());
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

    private void line(StringBuilder out, int indent, String key, String value) {
        for (int i = 0; i < indent; i++) out.append("  ");
        out.append(key).append(": ").append(escape(value)).append("\n");
    }

    private void list(StringBuilder out, int indent, String key, Iterable<String> values) {
        StringBuilder joined = new StringBuilder();
        boolean first = true;
        for (String v : values) {
            if (!first) joined.append(",");
            joined.append(escape(v));
            first = false;
        }
        line(out, indent, key, joined.length() == 0 ? "-" : joined.toString());
    }

    private void fields(StringBuilder out, List<Field> fields) {
        if (fields.isEmpty()) { line(out, 2, "fields", "-"); return; }
        out.append("    fields[").append(fields.size()).append("]:\n");
        for (Field f : fields) {
            line(out, 3, "field", f.name());
            line(out, 4, "type", f.type());
            line(out, 4, "visibility", f.visibility().jsonName());
            list(out, 4, "requiredImports", f.requiredImports());
        }
    }

    private void methods(StringBuilder out, String tag, List<Method> methods, boolean incRet) {
        if (methods.isEmpty()) { line(out, 2, tag, "-"); return; }
        out.append("    ").append(tag).append("[").append(methods.size()).append("]:\n");
        for (Method m : methods) {
            line(out, 3, "method", m.name());
            if (incRet) line(out, 4, "returnType", m.returnType());
            line(out, 4, "visibility", m.visibility().jsonName());
            list(out, 4, "requiredImports", m.requiredImports());
            if (m.parameters().isEmpty()) line(out, 4, "parameters", "-");
            else {
                StringBuilder ps = new StringBuilder();
                for (Parameter p : m.parameters()) ps.append(p.type()).append(" ").append(p.name()).append(",");
                if (ps.length() > 0) ps.setLength(ps.length() - 1);
                line(out, 4, "parameters", ps.toString());
            }
        }
    }

    private String escape(String raw) {
        if (raw == null) return "";
        // TOON escapes: \n, :, ,, [, ], =
        return raw.replace("\\", "\\\\").replace("\n", "\\n").replace(":", "\\:").replace(",", "\\,");
    }

    private void ensureParentExists(Path target) {
        Path parent = target.getParent();
        if (parent == null) return;
        try { Files.createDirectories(parent); } catch (IOException e) { throw new IllegalStateException("unable to create output folder " + parent, e); }
    }
}

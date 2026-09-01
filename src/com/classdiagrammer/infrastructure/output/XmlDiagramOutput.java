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
 * XML output adapter — zero dependencies.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class XmlDiagramOutput implements DiagramOutput {

    public Path write(DiagramReport report, String targetPath) {
        if (report == null) throw new IllegalArgumentException("report is required");
        if (targetPath == null || targetPath.trim().isEmpty()) throw new IllegalArgumentException("target path is required");
        StringBuilder out = new StringBuilder();
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.append("<diagram");
        attr(out, "tool", "ClassDiagrammer");
        attr(out, "version", "2.0.0");
        attr(out, "sourceRoot", report.sourceRoot());
        attr(out, "generatedAtUtc", report.generatedAtUtc());
        attr(out, "evaluation", report.evaluation().jsonName());
        out.append(">\n");

        out.append("  <summary");
        attr(out, "types", String.valueOf(report.nodes().size()));
        attr(out, "relations", String.valueOf(report.edges().size()));
        attr(out, "evidences", String.valueOf(report.evidences().size()));
        attr(out, "evaluation", report.evaluation().jsonName());
        out.append("/>\n");

        List<TypeNode> sortedNodes = new ArrayList<>(report.nodes());
        sortedNodes.sort((a, b) -> a.qualifiedName().compareTo(b.qualifiedName()));
        out.append("  <nodes>\n");
        for (TypeNode n : sortedNodes) {
            out.append("    <node");
            attr(out, "id", n.qualifiedName());
            attr(out, "name", n.simpleName());
            attr(out, "kind", n.kind().jsonName());
            attr(out, "visibility", n.visibility().jsonName());
            attr(out, "package", n.packageName());
            attr(out, "folder", n.folder());
            attr(out, "file", n.file());
            out.append(">\n");
            writeStringListXml(out, "      ", "modifiers", n.modifiers());
            writeStringListXml(out, "      ", "imports", n.imports());
            writeStringListXml(out, "      ", "extends", n.extendsTypes());
            writeStringListXml(out, "      ", "implements", n.implementsTypes());
            writeStringListXml(out, "      ", "permits", n.permitsTypes());
            writeFieldsXml(out, n.fields());
            writeMethodsXml(out, "constructors", n.constructors(), false);
            writeMethodsXml(out, "methods", n.methods(), true);
            out.append("    </node>\n");
        }
        out.append("  </nodes>\n");

        List<com.classdiagrammer.domain.model.Edge> sortedEdges = new ArrayList<>(report.edges());
        sortedEdges.sort((a, b) -> {
            int c = a.from().compareTo(b.from());
            if (c != 0) return c;
            c = a.to().compareTo(b.to());
            if (c != 0) return c;
            return a.kind().jsonName().compareTo(b.kind().jsonName());
        });
        out.append("  <edges>\n");
        for (com.classdiagrammer.domain.model.Edge e : sortedEdges) {
            out.append("    <edge");
            attr(out, "from", e.from());
            attr(out, "to", e.to());
            attr(out, "kind", e.kind().jsonName());
            attr(out, "resolved", String.valueOf(e.isResolved()));
            attr(out, "origin", e.origin().jsonName());
            if (e.artifact() == null) {
                out.append("/>\n");
            } else {
                out.append(">\n");
                out.append("      <artifact");
                attr(out, "groupId", e.artifact().groupId());
                attr(out, "artifactId", e.artifact().artifactId());
                attr(out, "version", e.artifact().version());
                out.append("/>\n");
                out.append("    </edge>\n");
            }
        }
        out.append("  </edges>\n");

        if (!report.evidences().isEmpty()) {
            out.append("  <evidences>\n");
            for (Evidence ev : report.evidences()) {
                out.append("    <evidence");
                attr(out, "evidenceId", ev.evidenceId());
                attr(out, "sourceFile", ev.sourceFile());
                attr(out, "locator", ev.locator());
                attr(out, "derivation", ev.derivation());
                attr(out, "factKind", ev.fact().kind().name());
                attr(out, "subject", ev.fact().subject());
                attr(out, "value", ev.fact().value());
                attr(out, "ruleId", ev.fact().ruleId());
                out.append("/>\n");
            }
            out.append("  </evidences>\n");
        }

        out.append("</diagram>\n");
        Path target = Paths.get(targetPath);
        ensureParentExists(target);
        try {
            Files.write(target, out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("unable to write diagram to " + target, e);
        }
        return target;
    }

    private void attr(StringBuilder out, String key, String value) {
        out.append(' ').append(key).append("=\"").append(escapeXml(value)).append('"');
    }

    private String escapeXml(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '&') sb.append("&amp;");
            else if (c == '<') sb.append("&lt;");
            else if (c == '>') sb.append("&gt;");
            else if (c == '"') sb.append("&quot;");
            else if (c == '\'') sb.append("&apos;");
            else {
                if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') sb.append("&#").append((int) c).append(';');
                else sb.append(c);
            }
        }
        return sb.toString();
    }

    private void writeStringListXml(StringBuilder out, String indent, String tag, Iterable<String> values) {
        out.append(indent).append('<').append(tag).append(">\n");
        for (String v : values) {
            out.append(indent).append("  <value>").append(escapeXml(v)).append("</value>\n");
        }
        out.append(indent).append("</").append(tag).append(">\n");
    }

    private void writeFieldsXml(StringBuilder out, List<Field> fields) {
        out.append("      <fields>\n");
        for (Field f : fields) {
            out.append("        <field");
            attr(out, "name", f.name());
            attr(out, "type", f.type());
            attr(out, "visibility", f.visibility().jsonName());
            out.append("/>\n");
        }
        out.append("      </fields>\n");
    }

    private void writeMethodsXml(StringBuilder out, String tag, List<Method> methods, boolean includeReturn) {
        out.append("      <").append(tag).append(">\n");
        for (Method m : methods) {
            out.append("        <method");
            attr(out, "name", m.name());
            if (includeReturn) attr(out, "returnType", m.returnType());
            attr(out, "visibility", m.visibility().jsonName());
            out.append(">\n");
            out.append("          <parameters>\n");
            for (Parameter p : m.parameters()) {
                out.append("            <parameter");
                attr(out, "type", p.type());
                attr(out, "name", p.name());
                out.append("/>\n");
            }
            out.append("          </parameters>\n");
            out.append("        </method>\n");
        }
        out.append("      </").append(tag).append(">\n");
    }

    private void ensureParentExists(Path target) {
        Path parent = target.getParent();
        if (parent == null) return;
        try { Files.createDirectories(parent); } catch (IOException e) { throw new IllegalStateException("unable to create output folder " + parent, e); }
    }
}

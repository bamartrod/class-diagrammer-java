package com.classdiagrammer.infrastructure.json;

import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.application.port.out.DiagramReport;
import com.classdiagrammer.domain.model.Field;
import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.Parameter;
import com.classdiagrammer.domain.model.TypeNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Infrastructure adapter writing the diagram as JSON.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class JsonDiagramOutput implements DiagramOutput {

    public Path write(DiagramReport report, String targetPath) {
        if (report == null) {
            throw new IllegalArgumentException("report is required");
        }
        if (targetPath == null || targetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("target path is required");
        }
        StringBuilder builder = new StringBuilder();
        JsonWriter json = new JsonWriter(builder);

        json.beginObject()
                .field("tool", "ClassDiagrammer")
                .field("version", "1.0.0")
                .field("sourceRoot", report.sourceRoot())
                .field("generatedAtUtc", report.generatedAtUtc());

        json.beginObject("summary")
                .field("types", report.nodes().size())
                .field("relations", report.edges().size())
                .endObject();

        json.beginArray("nodes");
        for (TypeNode node : report.nodes()) {
            writeNode(json, node);
        }
        json.endArray();

        json.beginArray("edges");
        for (com.classdiagrammer.domain.model.Edge edge : report.edges()) {
            json.beginObject()
                    .field("from", edge.from())
                    .field("to", edge.to())
                    .field("kind", edge.kind().jsonName())
                    .field("resolved", edge.isResolved())
                    .field("origin", edge.origin().jsonName());
            if (edge.artifact() != null) {
                json.beginObject("artifact")
                        .field("groupId", edge.artifact().groupId())
                        .field("artifactId", edge.artifact().artifactId())
                        .field("version", edge.artifact().version())
                        .endObject();
            }
            json.endObject();
        }
        json.endArray();

        json.endObject();

        Path target = Paths.get(targetPath);
        ensureParentExists(target);
        try {
            Files.write(target, builder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("unable to write diagram to " + target, e);
        }
        return target;
    }

    private void writeNode(JsonWriter json, TypeNode node) {
        json.beginObject()
                .field("id", node.qualifiedName())
                .field("name", node.simpleName())
                .field("kind", node.kind().jsonName())
                .field("visibility", node.visibility().jsonName())
                .field("package", node.packageName())
                .field("folder", node.folder())
                .field("file", node.file());

        writeStringList(json, "modifiers", node.modifiers());
        writeStringList(json, "imports", node.imports());
        writeStringList(json, "extends", node.extendsTypes());
        writeStringList(json, "implements", node.implementsTypes());
        writeStringList(json, "permits", node.permitsTypes());

        json.beginArray("fields");
        for (Field field : node.fields()) {
            json.beginObject()
                    .field("name", field.name())
                    .field("type", field.type())
                    .field("visibility", field.visibility().jsonName())
                    .endObject();
        }
        json.endArray();

        json.beginArray("constructors");
        for (Method constructor : node.constructors()) {
            writeOperation(json, constructor, false);
        }
        json.endArray();

        json.beginArray("methods");
        for (Method method : node.methods()) {
            writeOperation(json, method, true);
        }
        json.endArray();

        json.endObject();
    }

    private void writeOperation(JsonWriter json, Method operation, boolean includeReturnType) {
        json.beginObject()
                .field("name", operation.name());
        if (includeReturnType) {
            json.field("returnType", operation.returnType());
        }
        json.field("visibility", operation.visibility().jsonName());
        List<Parameter> parameters = operation.parameters();
        json.beginArray("parameters");
        for (Parameter parameter : parameters) {
            json.beginObject()
                    .field("type", parameter.type())
                    .field("name", parameter.name())
                    .endObject();
        }
        json.endArray();
        json.endObject();
    }

    private void writeStringList(JsonWriter json, String key, Iterable<String> values) {
        json.beginArray(key);
        for (String value : values) {
            json.stringValue(value);
        }
        json.endArray();
    }

    private void ensureParentExists(Path target) {
        Path parent = target.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("unable to create output folder " + parent, e);
        }
    }
}

package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.application.port.out.DiagramReport;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.TypeRelationKind;
import com.classdiagrammer.infrastructure.json.JsonDiagramOutput;
import com.classdiagrammer.tests.support.TestHarness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;

public final class JsonEmissionBehavior {

    private JsonEmissionBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("adapter/emision-json");

        h.expect("report is converted to readable json at requested path", () -> {
            Path root = scratch();
            try {
                Path target = root.resolve("diagrama.json");
                TypeNode base = TypeNode.named("app.Base", "Base").inPackage("app")
                        .ofKind(TypeKind.CLASS).build();
                CodeGraph graph = CodeGraph.of(Arrays.asList(base));
                DiagramReport report = DiagramReport.capture("proyecto", graph,
                        Arrays.asList(new Edge("app.Child", "app.Base",
                                TypeRelationKind.EXTENDS, true)),
                        Instant.EPOCH);
                new JsonDiagramOutput().write(report, target.toString());
                String text = readFile(target);
                return text.trim().startsWith("{")
                        && text.trim().endsWith("}")
                        && text.contains("\"nodes\"")
                        && text.contains("\"edges\"")
                        && text.contains("\"kind\": \"extends\"")
                        && text.contains("\"resolved\": true");
            } finally {
                cleanup(root);
            }
        });
        h.expect("json separators are always valid", () -> {
            Path root = scratch();
            try {
                Path target = root.resolve("estructura.json");
                TypeNode base = TypeNode.named("app.Base", "Base").inPackage("app")
                        .ofKind(TypeKind.CLASS)
                        .extending(Arrays.asList("Padre"))
                        .importing(Arrays.asList("java.util.List")).build();
                DiagramReport report = DiagramReport.capture("proyecto",
                        CodeGraph.of(Arrays.asList(base)), Arrays.asList(), Instant.EPOCH);
                new JsonDiagramOutput().write(report, target.toString());
                String text = readFile(target);
                return !text.contains(": ,") && !text.contains(", ]") && !text.contains(", }");
            } finally {
                cleanup(root);
            }
        });
        h.expect("destination folders are created if missing", () -> {
            Path root = scratch();
            try {
                Path deep = root.resolve("a/b/c/diagrama.json");
                DiagramReport report = DiagramReport.capture("x",
                        CodeGraph.of(Arrays.asList()), Arrays.asList(), Instant.EPOCH);
                new JsonDiagramOutput().write(report, deep.toString());
                return Files.exists(deep);
            } finally {
                cleanup(root);
            }
        });
        h.expect("special characters are escaped and recoverable", () -> {
            Path root = scratch();
            try {
                Path target = root.resolve("escapado.json");
                TypeNode exotic = TypeNode.named("w.X", "X").ofKind(TypeKind.CLASS)
                        .locatedAt("ca\"rpet\\nueva\nlinea", "X.java").build();
                DiagramReport report = DiagramReport.capture("x",
                        CodeGraph.of(Arrays.asList(exotic)), Arrays.asList(), Instant.EPOCH);
                new JsonDiagramOutput().write(report, target.toString());
                return readFile(target).contains("ca\\\"rpet\\\\nueva\\nlinea");
            } finally {
                cleanup(root);
            }
        });
        h.expect("origin travels in each edge and artifact only when present", () -> {
            Path root = scratch();
            try {
                Path target = root.resolve("origenes.json");
                Edge interna = new Edge("app.Child", "app.Base",
                        TypeRelationKind.EXTENDS, true).withProjectOrigin();
                Edge externa = new Edge("app.Child", "org.apache.velocity.Template",
                        TypeRelationKind.IMPLEMENTS, false)
                        .asExternal(new com.classdiagrammer.domain.model.ArtifactRef(
                                "org.apache.velocity", "velocity-engine-core", "2.4.1"));
                Edge misterio = new Edge("app.Child", "alguien.Extra",
                        TypeRelationKind.IMPLEMENTS, false);
                DiagramReport report = DiagramReport.capture("x",
                        CodeGraph.of(Arrays.asList()), Arrays.asList(
                                interna, externa, misterio), Instant.EPOCH);
                new JsonDiagramOutput().write(report, target.toString());
                String text = readFile(target);
                boolean externaCompleta = text.contains("\"origin\": \"external\"")
                        && text.contains("\"groupId\": \"org.apache.velocity\"")
                        && text.contains("\"version\": \"2.4.1\"");
                int artefactos = text.split("\"artifact\"", -1).length - 1;
                return externaCompleta
                        && text.contains("\"origin\": \"project\"")
                        && text.contains("\"origin\": \"unknown\"")
                        && artefactos == 1;
            } finally {
                cleanup(root);
            }
        });
    }

    private static String readFile(Path target) {
        try {
            return new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static Path scratch() {
        try {
            return Files.createTempDirectory("classdiagrammer-json");
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void cleanup(Path root) {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignoredCleanup) {
                }
            });
        } catch (Exception ignoredSweep) {
        }
    }
}

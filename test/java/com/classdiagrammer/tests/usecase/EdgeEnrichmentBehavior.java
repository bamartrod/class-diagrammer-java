package com.classdiagrammer.tests.usecase;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.domain.model.ArtifactRef;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.EdgeOrigin;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.TestHarness;

public final class EdgeEnrichmentBehavior {

    private EdgeEnrichmentBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("usecase/enriquecimiento-de-aristas");

        final DependencyResolver soloVelocity = name ->
                name.startsWith("org.apache.velocity.")
                        ? new ArtifactRef("org.apache.velocity",
                                "velocity-engine-core", "2.4.1")
                        : null;

        h.expect("a recognized external dependency travels with its artifact and version", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Render.java", "package app;",
                            "import org.apache.velocity.Template;",
                            "import java.util.List;",
                            "public class Render {",
                            "  private Template plantilla;",
                            "  public List<String> pintar() { return java.util.Collections.emptyList(); }",
                            "}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), soloVelocity, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            Edge haciaVelocity = findEdge(sink, "org.apache.velocity.Template");
            return haciaVelocity != null
                    && haciaVelocity.isResolved()
                    && haciaVelocity.origin() == EdgeOrigin.EXTERNAL
                    && haciaVelocity.artifact() != null
                    && haciaVelocity.artifact().version().equals("2.4.1");
        });

        h.expect("internal edges declare project origin without artifact", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Base.java", "package app;", "public class Base { }"),
                    Sources.java("src/Hija.java", "package app;",
                            "public class Hija extends Base { }"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), soloVelocity, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            Edge interna = null;
            for (Edge e : sink.lastReport.edges()) {
                if (e.to().equals("app.Base")) {
                    interna = e;
                }
            }
            return interna != null
                    && interna.origin() == EdgeOrigin.PROJECT
                    && interna.artifact() == null
                    && interna.isResolved();
        });

        h.expect("a simple name is qualified with imports from its own file", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Panel.java", "package ui;",
                            "import org.apache.velocity.BaseTemplate;",
                            "public class Panel extends BaseTemplate {",
                            "}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), soloVelocity, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            boolean calificada = false;
            for (Edge e : sink.lastReport.edges()) {
                if (e.to().equals("org.apache.velocity.BaseTemplate")
                        && e.origin() == EdgeOrigin.EXTERNAL
                        && e.kind() == com.classdiagrammer.domain.model.TypeRelationKind.EXTENDS) {
                    calificada = true;
                }
            }
            return calificada;
        });

        h.expect("what no one recognizes remains unresolved without artifact", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/X.java", "package x;",
                            "import raro.inventado.Cosa;",
                            "public class X { private Cosa cosa; }"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), soloVelocity, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            Edge misterio = findEdge(sink, "raro.inventado.Cosa");
            return misterio != null
                    && !misterio.isResolved()
                    && misterio.origin() == EdgeOrigin.UNKNOWN
                    && misterio.artifact() == null;
        });
    }

    private static Edge findEdge(RecordingDiagramSink sink, String target) {
        for (Edge edge : sink.lastReport.edges()) {
            if (edge.to().equals(target)) {
                return edge;
            }
        }
        return null;
    }
}

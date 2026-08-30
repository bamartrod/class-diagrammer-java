package com.classdiagrammer.tests.usecase;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.in.GenerateClassDiagramResult;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.ArtifactRef;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.StubDependencyResolver;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.TestHarness;

import java.nio.file.Paths;

public final class GenerationFlowBehavior {

    private GenerationFlowBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("usecase/generacion");

        h.expect("a project with linked classes arrives complete at destination", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Base.java", "package app;", "public class Base { }"),
                    Sources.java("src/Service.java",
                            "package app;", "public class Service extends Base { }"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            GenerateClassDiagramResult result = useCase(reader, sink)
                    .generate(GenerateClassDiagramCommand.of("proyecto/ficticio", "salida/diagrama.json"));

            return result.typeCount() == 2
                    && result.edgeCount() >= 1
                    && result.writtenTo().equals(Paths.get("salida/diagrama.json"))
                    && sink.writings == 1
                    && sink.lastReport.nodes().size() == 2
                    && sink.lastReport.edges().size() == result.edgeCount();
        });
        h.expect("un proyecto sin fuentes sigue entregando un diagrama vacio", () -> {
            SourceRecords reader = new SourceRecords();
            RecordingDiagramSink sink = new RecordingDiagramSink();
            GenerateClassDiagramResult result = useCase(reader, sink)
                    .generate(GenerateClassDiagramCommand.of("vacio", "vacio.json"));
            return result.typeCount() == 0 && result.edgeCount() == 0 && sink.writings == 1;
        });
        h.expect("a blank root is rejected before touching filesystem", () -> {
            SourceRecords reader = new SourceRecords();
            RecordingDiagramSink sink = new RecordingDiagramSink();
            try {
                useCase(reader, sink).generate(GenerateClassDiagramCommand.of("   ", "x.json"));
                return false;
            } catch (IllegalArgumentException expected) {
                return reader.consultations == 0 && sink.writings == 0;
            }
        });
        h.expect("a missing output path is rejected with same care", () -> {
            try {
                GenerateClassDiagramCommand.of("raiz", " ");
                return false;
            } catch (IllegalArgumentException expected) {
                return true;
            }
        });
        h.expect("cada colaborador es imprescindible para orquestar", () -> {
            JavaArtifactParser parser = new JavaArtifactParser();
            EdgeResolver resolver = new EdgeResolver();
            StubDependencyResolver dependencies =
                    new StubDependencyResolver(new ArtifactRef("g", "a", "1"));
            SourceRecords reader = new SourceRecords();
            RecordingDiagramSink sink = new RecordingDiagramSink();
            boolean missingReader = refuses(null, parser, resolver, dependencies, sink);
            boolean missingParser = refuses(reader, null, resolver, dependencies, sink);
            boolean missingResolver = refuses(reader, parser, null, dependencies, sink);
            boolean missingDependencies =
                    refuses(reader, parser, resolver, null, sink);
            boolean missingOutput =
                    refuses(reader, parser, resolver, dependencies, null);
            return missingReader && missingParser && missingResolver
                    && missingDependencies && missingOutput;
        });
    }

    private static GenerateClassDiagramUseCase useCase(SourceRecords reader,
                                                       RecordingDiagramSink sink) {
        return new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                new EdgeResolver(),
                new StubDependencyResolver(null), sink);
    }

    private static boolean refuses(SourceRecords reader, ArtifactParser parser,
                                   EdgeResolver resolver,
                                   StubDependencyResolver dependencies,
                                   RecordingDiagramSink sink) {
        try {
            new GenerateClassDiagramUseCase(reader, parser, resolver, dependencies, sink);
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }
}

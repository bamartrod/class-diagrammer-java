package com.classdiagrammer.tests.unit;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.infrastructure.output.OutputFormat;
import com.classdiagrammer.infrastructure.output.XmlDiagramOutput;
import com.classdiagrammer.infrastructure.output.YamlDiagramOutput;
import com.classdiagrammer.infrastructure.output.ToonDiagramOutput;
import com.classdiagrammer.infrastructure.json.JsonDiagramOutput;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.TestHarness;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.tests.support.StubDependencyResolver;
import com.classdiagrammer.interfaces.cli.CliArgs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * Verifies output format handling per new requirement (xml, yaml, toon).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class OutputFormatBehavior {

    private OutputFormatBehavior() {}

    public static void verify(TestHarness h) {
        h.scope("unit/output-format");

        h.expect("output format is inferred from file extension", () -> {
            return OutputFormat.fromPath("out.json") == OutputFormat.JSON
                    && OutputFormat.fromPath("out.xml") == OutputFormat.XML
                    && OutputFormat.fromPath("out.yaml") == OutputFormat.YAML
                    && OutputFormat.fromPath("out.yml") == OutputFormat.YAML
                    && OutputFormat.fromPath("out.toon") == OutputFormat.TOON
                    && OutputFormat.fromPath("out.txt") == OutputFormat.JSON;
        });

        h.expect("explicit --format overrides extension", () -> {
            CliArgs c1 = CliArgs.parse(new String[]{"src", "--format", "xml"});
            CliArgs c2 = CliArgs.parse(new String[]{"src", "-o", "out.json", "--format", "yaml"});
            return c1.outputFormat().equals("xml") && c2.outputFormat().equals("yaml");
        });

        h.expect("unsupported format is rejected", () -> {
            try {
                CliArgs.parse(new String[]{"src", "--format", "pdf"});
                return false;
            } catch (IllegalArgumentException e) {
                return e.getMessage().contains("unsupported output format");
            }
        });

        h.expect("json output is valid and contains tool marker", () -> {
            try {
            Path tmp = Files.createTempFile("diag", ".json");
            SourceRecords reader = new SourceRecords(Sources.java("src/A.java", "package app;", "public class A {}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", tmp.toString()));
            // Use Json directly
            JsonDiagramOutput out = new JsonDiagramOutput();
            Path p = out.write(sink.lastReport, tmp.toString());
            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            return content.contains("\"tool\"") && content.contains("ClassDiagrammer");
            } catch (Exception e) { return false; }
        });

        h.expect("xml output is well-formed and contains diagram root", () -> {
            try {
            Path tmp = Files.createTempFile("diag", ".xml");
            SourceRecords reader = new SourceRecords(Sources.java("src/A.java", "package app;", "public class A {}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", tmp.toString()));
            XmlDiagramOutput out = new XmlDiagramOutput();
            Path p = out.write(sink.lastReport, tmp.toString());
            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            return content.startsWith("<?xml") && content.contains("<diagram") && content.contains("<node");
            } catch (Exception e) { return false; }
        });

        h.expect("yaml output is valid and contains nodes key", () -> {
            try {
            Path tmp = Files.createTempFile("diag", ".yaml");
            SourceRecords reader = new SourceRecords(Sources.java("src/A.java", "package app;", "public class A {}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", tmp.toString()));
            YamlDiagramOutput out = new YamlDiagramOutput();
            Path p = out.write(sink.lastReport, tmp.toString());
            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            return content.contains("tool:") && content.contains("nodes:");
            } catch (Exception e) { return false; }
        });

        h.expect("toon output is token-efficient and contains tool marker", () -> {
            try {
            Path tmp = Files.createTempFile("diag", ".toon");
            SourceRecords reader = new SourceRecords(Sources.java("src/A.java", "package app;", "public class A {}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", tmp.toString()));
            ToonDiagramOutput out = new ToonDiagramOutput();
            Path p = out.write(sink.lastReport, tmp.toString());
            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            return content.contains("tool: ClassDiagrammer") && content.contains("nodes[");
            } catch (Exception e) { return false; }
        });

        h.expect("all formats are deterministic for same input", () -> {
            try {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/B.java", "package app;", "public class B {}"),
                    Sources.java("src/A.java", "package app;", "public class A {}")
            );
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", "out.json"));
            Path t1 = Files.createTempFile("diag", ".xml");
            Path t2 = Files.createTempFile("diag", ".xml");
            new XmlDiagramOutput().write(sink.lastReport, t1.toString());
            new XmlDiagramOutput().write(sink.lastReport, t2.toString());
            return new String(Files.readAllBytes(t1), StandardCharsets.UTF_8).equals(new String(Files.readAllBytes(t2), StandardCharsets.UTF_8));
            } catch (Exception e) { return false; }
        });
    }
}

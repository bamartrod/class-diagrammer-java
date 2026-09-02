package com.classdiagrammer.interfaces.cli;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.in.GenerateClassDiagramResult;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.dependencies.BuildDependencyScanner;
import com.classdiagrammer.infrastructure.dependencies.ClasspathArtifactResolver;
import com.classdiagrammer.infrastructure.dependencies.LocalRepositoryIndex;
import com.classdiagrammer.infrastructure.filesystem.FileSystemSourceReader;
import com.classdiagrammer.infrastructure.output.DiagramOutputFactory;
import com.classdiagrammer.infrastructure.output.OutputFormat;
import com.classdiagrammer.infrastructure.parsing.CompositeArtifactParser;
import com.classdiagrammer.infrastructure.parsing.JavaParserFactory;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.hibernate.HbmArtifactParser;
import com.classdiagrammer.infrastructure.parsing.velocity.VelocityArtifactParser;
import com.classdiagrammer.infrastructure.parsing.xforms.XFormsArtifactParser;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Composition root and CLI entry point wiring all adapters.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        CliArgs cli;
        try {
            cli = CliArgs.parse(args);
        } catch (IllegalArgumentException e) {
            // INPUT_VALIDATION_FAILURE / CONFIGURATION_FAILURE per RULE-002-U20
            System.err.println("Input validation failed: " + e.getMessage());
            System.exit(2);
            return;
        }
        if (cli.helpRequested()) {
            System.out.println(CliArgs.usage());
            return;
        }
        try {
            OutputFormat fmt = OutputFormat.from(cli.outputFormat());
            GenerateClassDiagramResult result = new GenerateClassDiagramUseCase(
                    new FileSystemSourceReader(),
                    new CompositeArtifactParser(Arrays.asList(
                            JavaParserFactory.forVersion(JavaVersion.from(cli.javaVersion())),
                            new VelocityArtifactParser(),
                            new XFormsArtifactParser(),
                            new HbmArtifactParser())),
                    new EdgeResolver(),
                    new ClasspathArtifactResolver(new LocalRepositoryIndex(
                            new BuildDependencyScanner().scan(cli.sourceRoot()))),
                    DiagramOutputFactory.forFormat(fmt))
                    .generate(GenerateClassDiagramCommand.of(cli.sourceRoot(), cli.outputPath()));
            System.out.printf("Graph generated: %d types, %d relations -> %s%n",
                    result.typeCount(), result.edgeCount(), result.writtenTo());
            // also report evaluation if present
            // evaluation is inside JSON; CLI could print evidence count if needed
        } catch (IllegalStateException e) {
            // EXPECTED_OPERATIONAL_FAILURE / RECOVERABLE_RUNTIME per RULE-002-U20
            System.err.println("Operational failure: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            // PROGRAMMER_DEFECT – do not hide
            System.err.println("Unexpected failure: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(3);
        } catch (Exception e) {
            System.err.println("Fatal failure: " + e.getMessage());
            System.exit(4);
        }
    }
}

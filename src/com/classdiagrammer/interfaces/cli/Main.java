package com.classdiagrammer.interfaces.cli;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.in.GenerateClassDiagramResult;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.dependencies.BuildDependencyScanner;
import com.classdiagrammer.infrastructure.dependencies.ClasspathArtifactResolver;
import com.classdiagrammer.infrastructure.dependencies.LocalRepositoryIndex;
import com.classdiagrammer.infrastructure.filesystem.FileSystemSourceReader;
import com.classdiagrammer.infrastructure.json.JsonDiagramOutput;
import com.classdiagrammer.infrastructure.parsing.CompositeArtifactParser;
import com.classdiagrammer.infrastructure.parsing.JavaParserFactory;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.hibernate.HbmArtifactParser;
import com.classdiagrammer.infrastructure.parsing.velocity.VelocityArtifactParser;
import com.classdiagrammer.infrastructure.parsing.xforms.XFormsArtifactParser;

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
        try {
            CliArgs cli = CliArgs.parse(args);
            if (cli.helpRequested()) {
                System.out.println(CliArgs.usage());
                return;
            }
            GenerateClassDiagramResult result =
                    new GenerateClassDiagramUseCase(
                            new FileSystemSourceReader(),
                            new CompositeArtifactParser(Arrays.asList(
                                    JavaParserFactory.forVersion(
                                            JavaVersion.from(cli.javaVersion())),
                                    new VelocityArtifactParser(),
                                    new XFormsArtifactParser(),
                                    new HbmArtifactParser())),
                            new EdgeResolver(),
                            new ClasspathArtifactResolver(new LocalRepositoryIndex(
                                    new BuildDependencyScanner().scan(cli.sourceRoot()))),
                            new JsonDiagramOutput())
                            .generate(GenerateClassDiagramCommand.of(
                                    cli.sourceRoot(), cli.outputPath()));
            System.out.printf("Graph generated: %d types, %d relations -> %s%n",
                    result.typeCount(), result.edgeCount(), result.writtenTo());
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }
}

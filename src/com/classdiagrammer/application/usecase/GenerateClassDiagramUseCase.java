package com.classdiagrammer.application.usecase;

import com.classdiagrammer.application.port.in.GenerateClassDiagram;
import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.in.GenerateClassDiagramResult;
import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.application.port.out.DiagramReport;
import com.classdiagrammer.application.port.out.SourceCodeReader;
import com.classdiagrammer.domain.model.ArtifactRef;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.EdgeOrigin;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.resolution.EdgeResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Application use case orchestrating parsing, resolution and output generation.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class GenerateClassDiagramUseCase implements GenerateClassDiagram {

    private final SourceCodeReader sourceReader;
    private final ArtifactParser artifactParser;
    private final EdgeResolver edgeResolver;
    private final DependencyResolver dependencyResolver;
    private final DiagramOutput diagramOutput;

    public GenerateClassDiagramUseCase(SourceCodeReader sourceReader,
                                       ArtifactParser artifactParser,
                                       EdgeResolver edgeResolver,
                                       DependencyResolver dependencyResolver,
                                       DiagramOutput diagramOutput) {
        if (sourceReader == null) {
            throw new IllegalArgumentException("source reader is required");
        }
        if (artifactParser == null) {
            throw new IllegalArgumentException("artifact parser is required");
        }
        if (edgeResolver == null) {
            throw new IllegalArgumentException("edge resolver is required");
        }
        if (dependencyResolver == null) {
            throw new IllegalArgumentException("dependency resolver is required");
        }
        if (diagramOutput == null) {
            throw new IllegalArgumentException("diagram output is required");
        }
        this.sourceReader = sourceReader;
        this.artifactParser = artifactParser;
        this.edgeResolver = edgeResolver;
        this.dependencyResolver = dependencyResolver;
        this.diagramOutput = diagramOutput;
    }

    public GenerateClassDiagramResult generate(GenerateClassDiagramCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        List<SourceFile> files = sourceReader.readAll(command.sourceRoot());
        List<TypeNode> nodes = parseConcurrently(files);
        CodeGraph graph = CodeGraph.of(nodes);
        List<Edge> edges = enrichOrigins(edgeResolver.resolve(graph), nodes);
        DiagramReport report =
                DiagramReport.capture(command.sourceRoot(), graph, edges, Instant.now());
        java.nio.file.Path written = diagramOutput.write(report, command.outputPath());
        return new GenerateClassDiagramResult(written, graph.size(), edges.size());
    }

    private List<TypeNode> parseConcurrently(List<SourceFile> files) {
        if (files.isEmpty()) {
            return new ArrayList<>();
        }
        if (files.size() == 1) {
            var file = files.getFirst();
            try {
                return new ArrayList<>(artifactParser.parse(file));
            } catch (StackOverflowError e) {
                System.err.println("Warning: file skipped due to stack overflow: " + file.file());
                return new ArrayList<>();
            } catch (Exception e) {
                System.err.println("Warning: file skipped due to error: " + file.file() + " - " + e.getMessage());
                return new ArrayList<>();
            }
        }
        var futures = new ArrayList<Future<List<TypeNode>>>(files.size());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var file : files) {
                futures.add(executor.submit(() -> {
                    try {
                        return artifactParser.parse(file);
                    } catch (StackOverflowError e) {
                        System.err.println("Warning: file skipped due to stack overflow: " + file.file());
                        return List.<TypeNode>of();
                    } catch (Exception e) {
                        System.err.println("Warning: file skipped due to error: " + file.file() + " - " + e.getMessage());
                        return List.<TypeNode>of();
                    }
                }));
            }
            var collected = new ArrayList<TypeNode>();
            for (var future : futures) {
                try {
                    collected.addAll(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("parsing interrupted", e);
                } catch (ExecutionException e) {
                    System.err.println("Warning: task failed: " + e.getCause().getMessage());
                }
            }
            return collected;
        }
    }

    private List<Edge> enrichOrigins(List<Edge> edges, List<TypeNode> nodes) {
        Map<String, List<String>> importsByNode = new HashMap<>();
        for (TypeNode node : nodes) {
            importsByNode.put(node.qualifiedName(), node.imports());
        }
        List<Edge> enriched = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.isResolved()) {
                enriched.add(edge.withProjectOrigin());
                continue;
            }
            String qualified = qualify(edge.from(), edge.to(), importsByNode);
            ArtifactRef provider = dependencyResolver.locate(qualified);
            enriched.add(provider == null
                    ? edge
                    : edge.asExternal(qualified, provider));
        }
        return enriched;
    }

    private String qualify(String fromNode, String rawTarget,
                           Map<String, List<String>> importsByNode) {
        if (rawTarget.indexOf('.') >= 0 || rawTarget.contains("/")) {
            return rawTarget;
        }
        List<String> imports = importsByNode.get(fromNode);
        if (imports == null) {
            return rawTarget;
        }
        for (String imported : imports) {
            if (imported.endsWith("." + rawTarget)) {
                return imported;
            }
        }
        return rawTarget;
    }
}

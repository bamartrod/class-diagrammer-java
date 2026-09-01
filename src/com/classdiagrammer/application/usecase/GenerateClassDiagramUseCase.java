package com.classdiagrammer.application.usecase;

import com.classdiagrammer.application.port.in.GenerateClassDiagram;
import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.in.GenerateClassDiagramResult;
import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.application.port.out.DiagramReport;
import com.classdiagrammer.application.port.out.SourceCodeReader;
import com.classdiagrammer.domain.evidence.EvaluationState;
import com.classdiagrammer.domain.evidence.Evidence;
import com.classdiagrammer.domain.evidence.FactKind;
import com.classdiagrammer.domain.evidence.ImplementationFact;
import com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.resolution.ArchitecturalOriginResolver;
import com.classdiagrammer.domain.resolution.EdgeResolver;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Application use case orchestrating parsing, resolution and output generation.
 * Single cohesive responsibility: transform validated command into implementation analysis
 * producing a class-diagram view over the semantic model (CSAS-007-U2).
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
        if (sourceReader == null) throw new IllegalArgumentException("source reader is required");
        if (artifactParser == null) throw new IllegalArgumentException("artifact parser is required");
        if (edgeResolver == null) throw new IllegalArgumentException("edge resolver is required");
        if (dependencyResolver == null) throw new IllegalArgumentException("dependency resolver is required");
        if (diagramOutput == null) throw new IllegalArgumentException("diagram output is required");
        this.sourceReader = sourceReader;
        this.artifactParser = artifactParser;
        this.edgeResolver = edgeResolver;
        this.dependencyResolver = dependencyResolver;
        this.diagramOutput = diagramOutput;
    }

    public GenerateClassDiagramResult generate(GenerateClassDiagramCommand command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        // Configuration facts: declaration -> consumption -> effect (CSAS-002-U18)
        List<Evidence> configEvidences = List.of(
                new Evidence(new ImplementationFact(FactKind.CONFIGURATION_DECLARATION, "javaVersion", "CliArgs", command.sourceRoot(), "CSAS-011-U15"), command.sourceRoot(), "CliArgs.parse", "CFG-DECL"),
                new Evidence(new ImplementationFact(FactKind.CONFIGURATION_CONSUMPTION, "javaVersion", "JavaParserFactory", "consumed", "CSAS-002-U18"), command.sourceRoot(), "JavaParserFactory.forVersion", "CFG-CONS"),
                new Evidence(new ImplementationFact(FactKind.CONFIGURATION_EFFECT, "javaVersion", "JavaArtifactParser", "affects parsing", "CSAS-002-U18"), command.sourceRoot(), "LanguageCapabilities", "CFG-EFF")
        );

        List<SourceFile> files = sourceReader.readAll(command.sourceRoot());
        ParseOutcome outcome = parseConcurrently(files);
        CodeGraph graph = CodeGraph.of(outcome.nodes());
        // Enrich origins with explicit ArchitecturalOriginResolver (separate from qualification)
        ArchitecturalOriginResolver originResolver = new ArchitecturalOriginResolver(dependencyResolver);
        ArchitecturalOriginResolver.EnrichmentResult enriched = originResolver.enrich(edgeResolver.resolve(graph), outcome.nodes());

        List<Evidence> allEvidences = new ArrayList<>();
        allEvidences.addAll(configEvidences);
        allEvidences.addAll(outcome.evidences());
        allEvidences.addAll(enriched.evidences());

        // Add structural facts
        for (TypeNode n : outcome.nodes()) {
            allEvidences.add(new Evidence(new ImplementationFact(FactKind.TYPE_EXISTS, n.qualifiedName(), n.file() + ":1", n.kind().name(), "CSAS-003-U2"), n.file(), "TypeNode", "FACT-TYPE-" + n.qualifiedName().hashCode()));
        }
        for (Edge e : enriched.edges()) {
            allEvidences.add(new Evidence(new ImplementationFact(FactKind.DEPENDENCY_EXISTS, e.from() + "->" + e.to(), e.from(), e.kind().name(), "CSAS-003-U6"), e.from(), "EdgeResolver", "FACT-DEP-" + e.hashCode()));
        }

        EvaluationState evaluation = outcome.evaluation();
        if (evaluation == EvaluationState.CONFORMANT && !allEvidences.isEmpty()) {
            // if unsupported was not already set, keep CONFORMANT; otherwise UNSUPPORTED from parse
        }

        DiagramReport report = DiagramReport.capture(command.sourceRoot(), graph, enriched.edges(), Instant.now(), allEvidences, evaluation);
        Path written = diagramOutput.write(report, command.outputPath());
        return new GenerateClassDiagramResult(written, graph.size(), enriched.edges().size());
    }

    private ParseOutcome parseConcurrently(List<SourceFile> files) {
        if (files.isEmpty()) {
            return new ParseOutcome(List.of(), List.of(), EvaluationState.CONFORMANT);
        }
        if (files.size() == 1) {
            SourceFile file = files.get(0);
            try {
                return new ParseOutcome(new ArrayList<>(artifactParser.parse(file)), List.of(), EvaluationState.CONFORMANT);
            } catch (UnsupportedLanguageFeatureException e) {
                Evidence ev = e.toEvidence();
                // also fact for usage
                ImplementationFact usage = e.toFact();
                Evidence usageEv = new Evidence(usage, e.sourceFile(), "JavaArtifactParser.detectUnsupportedFeatures", "EVID-USAGE-" + e.sourceFile().hashCode());
                return new ParseOutcome(List.of(), List.of(ev, usageEv), EvaluationState.UNSUPPORTED);
            } catch (IllegalStateException e) {
                // bounded depth exceeded -> REVIEW_REQUIRED per CSAS-002-U22
                ImplementationFact fact = new ImplementationFact(FactKind.RESOURCE_OWNERSHIP, file.file(), file.file() + ":1", "depth_exceeded", "CSAS-002-U22");
                Evidence ev = new Evidence(fact, file.file(), "RegionScanner.scanBounded", "EVID-DEPTH-" + file.file().hashCode());
                return new ParseOutcome(List.of(), List.of(ev), EvaluationState.REVIEW_REQUIRED);
            } catch (Exception e) {
                ImplementationFact fact = new ImplementationFact(FactKind.FAILURE_CLASSIFICATION, file.file(), file.file() + ":1", e.getClass().getSimpleName(), "CSAS-002-U20");
                Evidence ev = new Evidence(fact, file.file(), "ArtifactParser.parse", "EVID-FAIL-" + file.file().hashCode());
                System.err.println("Warning: file skipped due to error: " + file.file() + " - " + e.getMessage());
                return new ParseOutcome(List.of(), List.of(ev), EvaluationState.UNDECIDABLE);
            }
        }
        List<Future<ParseOutcome>> futures = new ArrayList<>(files.size());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (SourceFile file : files) {
                futures.add(executor.submit(() -> {
                    try {
                        List<TypeNode> nodes = artifactParser.parse(file);
                        return new ParseOutcome(nodes, List.of(), EvaluationState.CONFORMANT);
                    } catch (UnsupportedLanguageFeatureException e) {
                        Evidence ev = e.toEvidence();
                        ImplementationFact usage = e.toFact();
                        Evidence usageEv = new Evidence(usage, e.sourceFile(), "JavaArtifactParser.detectUnsupportedFeatures", "EVID-USAGE-" + e.sourceFile().hashCode());
                        return new ParseOutcome(List.<TypeNode>of(), List.of(ev, usageEv), EvaluationState.UNSUPPORTED);
                    } catch (IllegalStateException e) {
                        ImplementationFact fact = new ImplementationFact(FactKind.RESOURCE_OWNERSHIP, file.file(), file.file() + ":1", "depth_exceeded", "CSAS-002-U22");
                        Evidence ev = new Evidence(fact, file.file(), "RegionScanner.scanBounded", "EVID-DEPTH-" + file.file().hashCode());
                        return new ParseOutcome(List.<TypeNode>of(), List.of(ev), EvaluationState.REVIEW_REQUIRED);
                    } catch (Exception e) {
                        ImplementationFact fact = new ImplementationFact(FactKind.FAILURE_CLASSIFICATION, file.file(), file.file() + ":1", e.getClass().getSimpleName(), "CSAS-002-U20");
                        Evidence ev = new Evidence(fact, file.file(), "ArtifactParser.parse", "EVID-FAIL-" + file.file().hashCode());
                        System.err.println("Warning: file skipped due to error: " + file.file() + " - " + e.getMessage());
                        return new ParseOutcome(List.<TypeNode>of(), List.of(ev), EvaluationState.UNDECIDABLE);
                    }
                }));
            }
            List<TypeNode> collected = new ArrayList<>();
            List<Evidence> allEvs = new ArrayList<>();
            EvaluationState overall = EvaluationState.CONFORMANT;
            for (Future<ParseOutcome> future : futures) {
                try {
                    ParseOutcome po = future.get();
                    collected.addAll(po.nodes());
                    allEvs.addAll(po.evidences());
                    if (po.evaluation() == EvaluationState.UNSUPPORTED) overall = EvaluationState.UNSUPPORTED;
                    else if (po.evaluation() == EvaluationState.REVIEW_REQUIRED && overall != EvaluationState.UNSUPPORTED) overall = EvaluationState.REVIEW_REQUIRED;
                    else if (po.evaluation() == EvaluationState.UNDECIDABLE && overall == EvaluationState.CONFORMANT) overall = EvaluationState.UNDECIDABLE;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("parsing interrupted", e);
                } catch (ExecutionException e) {
                    System.err.println("Warning: task failed: " + e.getCause().getMessage());
                    ImplementationFact fact = new ImplementationFact(FactKind.FAILURE_CLASSIFICATION, "task", "executor", e.getCause().getClass().getSimpleName(), "CSAS-002-U20");
                    allEvs.add(new Evidence(fact, "executor", "GenerateClassDiagramUseCase", "EVID-TASK-" + e.hashCode()));
                    if (overall == EvaluationState.CONFORMANT) overall = EvaluationState.UNDECIDABLE;
                }
            }
            return new ParseOutcome(collected, allEvs, overall);
        }
    }

    private static final class ParseOutcome {
        private final List<TypeNode> nodes;
        private final List<Evidence> evidences;
        private final EvaluationState evaluation;

        ParseOutcome(List<TypeNode> nodes, List<Evidence> evidences, EvaluationState evaluation) {
            this.nodes = List.copyOf(nodes);
            this.evidences = List.copyOf(evidences);
            this.evaluation = evaluation;
        }
        List<TypeNode> nodes() { return nodes; }
        List<Evidence> evidences() { return evidences; }
        EvaluationState evaluation() { return evaluation; }
    }
}

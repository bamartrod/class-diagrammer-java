package com.classdiagrammer.tests.unit;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.domain.evidence.Evidence;
import com.classdiagrammer.domain.evidence.FactKind;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.resolution.ArchitecturalOriginResolver;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.StubDependencyResolver;
import com.classdiagrammer.tests.support.TestHarness;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;

import java.util.List;

/**
 * Verifies that every RULE-relevant fact is attributable to evidence (RULE-002-U5/U6) and
 * that insufficient evidence is not reported as CONFORMANT (RULE-002-U13).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class EvidenceSufficiencyBehavior {

    private EvidenceSufficiencyBehavior() {}

    public static void verify(TestHarness h) {
        h.scope("unit/evidence-sufficiency");

        h.expect("architectural origin enrichment produces attributable evidence", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/A.java", "package app;", "public class A {}"),
                    Sources.java("src/B.java", "package app;", "public class B extends A {}")
            );
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", "out.json"));
            List<Evidence> evs = sink.lastReport.evidences();
            // should contain architectural origin facts
            boolean hasOrigin = evs.stream().anyMatch(e -> e.fact().kind() == FactKind.ARCHITECTURAL_ORIGIN);
            boolean hasType = evs.stream().anyMatch(e -> e.fact().kind() == FactKind.TYPE_EXISTS);
            boolean hasDep = evs.stream().anyMatch(e -> e.fact().kind() == FactKind.DEPENDENCY_EXISTS);
            return hasOrigin && hasType && hasDep;
        });

        h.expect("evidence carries locator and derivation for traceability", () -> {
            TypeNode a = TypeNode.named("app.A", "A").ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                    .withVisibility(com.classdiagrammer.domain.model.Visibility.PUBLIC)
                    .locatedAt("src/app", "src/app/A.java").build();
            TypeNode b = TypeNode.named("app.B", "B").ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                    .withVisibility(com.classdiagrammer.domain.model.Visibility.PUBLIC)
                    .locatedAt("src/app", "src/app/B.java")
                    .extending(java.util.Arrays.asList("A")).build();
            CodeGraph g = CodeGraph.of(java.util.Arrays.asList(a, b));
            EdgeResolver resolver = new EdgeResolver();
            ArchitecturalOriginResolver enricher = new ArchitecturalOriginResolver(new StubDependencyResolver(null));
            ArchitecturalOriginResolver.EnrichmentResult res = enricher.enrich(resolver.resolve(g), java.util.Arrays.asList(a, b));
            for (Evidence ev : res.evidences()) {
                if (ev.locator() == null || ev.locator().isEmpty()) return false;
                if (ev.derivation() == null || ev.derivation().isEmpty()) return false;
                if (ev.evidenceId() == null || ev.evidenceId().isEmpty()) return false;
                if (ev.fact().ruleId() == null || ev.fact().ruleId().isEmpty()) return false;
            }
            return !res.evidences().isEmpty();
        });

        h.expect("configuration effect evidence is generated for java version", () -> {
            SourceRecords reader = new SourceRecords(Sources.java("src/A.java", "package app;", "public class A {}"));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(),
                    new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", "out.json"));
            return sink.lastReport.evidences().stream()
                    .anyMatch(e -> e.fact().kind() == FactKind.CONFIGURATION_EFFECT);
        });

        h.expect("unsupported feature generates LANGUAGE_FEATURE evidence with UNSUPPORTED evaluation", () -> {
            // Use java 8 parser with record source via direct use case that captures unsupported
            // Simulate via direct parser throw -> evidence
            com.classdiagrammer.infrastructure.parsing.JavaVersion v8 = com.classdiagrammer.infrastructure.parsing.JavaVersion.V8;
            com.classdiagrammer.infrastructure.parsing.LanguageCapabilities caps = com.classdiagrammer.infrastructure.parsing.LanguageCapabilities.forVersion(v8);
            JavaArtifactParser parser = new JavaArtifactParser(v8, caps);
            SourceFile file = new SourceFile("app", "app/R.java", "package app; record R(int x) {}");
            try {
                parser.parse(file);
                return false;
            } catch (com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException e) {
                Evidence ev = e.toEvidence();
                return ev.fact().kind() == FactKind.LANGUAGE_FEATURE_AVAILABILITY
                        && ev.fact().value().equals("unavailable");
            }
        });
    }
}

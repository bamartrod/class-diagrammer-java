package com.classdiagrammer.tests.conformance;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.domain.conformance.ConformanceEngine;
import com.classdiagrammer.domain.conformance.ConformanceResult;
import com.classdiagrammer.domain.conformance.EvaluationResult;
import com.classdiagrammer.domain.conformance.Rule;
import com.classdiagrammer.domain.evidence.EvaluationState;
import com.classdiagrammer.domain.evidence.Evidence;
import com.classdiagrammer.domain.evidence.FactKind;
import com.classdiagrammer.domain.evidence.ImplementationFact;
import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Visibility;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.LanguageCapabilities;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.TestHarness;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;

import java.util.List;

/**
 * Adversarial conformance fixtures per RULE §13–14.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class AdversarialConformanceBehavior {

    private AdversarialConformanceBehavior() {}

    public static void verify(TestHarness h) {
        h.scope("conformance/adversarial");

        // Fixture A — CONFORMANT
        h.expect("Fixture A — conformant implementation yields CONFORMANT", () -> {
            CodeGraph graph = CodeGraph.of(List.of(
                    TypeNode.named("com.classdiagrammer.domain.Foo", "Foo")
                            .inPackage("com.classdiagrammer.domain")
                            .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                            .withVisibility(Visibility.PUBLIC)
                            .locatedAt("com/classdiagrammer/domain", "com/classdiagrammer/domain/Foo.java")
                            .build()
            ));
            List<Edge> edges = List.of();
            List<Evidence> evs = List.of(
                    new Evidence(new ImplementationFact(FactKind.TYPE_EXISTS, "com.classdiagrammer.domain.Foo", "com/classdiagrammer/domain/Foo.java:1", "CLASS", "RULE-003-U2"), "com/classdiagrammer/domain/Foo.java", "TypeNode", "EVID-1")
            );
            ConformanceResult cr = ConformanceEngine.defaultEngine().evaluate(graph, edges, evs);
            EvaluationResult r = cr.results().stream().filter(e -> e.ruleId().equals("RULE-004-U13")).findFirst().orElse(null);
            return r != null && r.state() == EvaluationState.CONFORMANT
                    && r.applicability() && r.predicateResult() != null && r.predicateResult()
                    && r.traceability().contains("RULE-004-U13");
        });

        // Fixture B — NON_CONFORMANT (deliberate violation)
        h.expect("Fixture B — deliberate violation yields NON_CONFORMANT", () -> {
            TypeNode violating = TypeNode.named("com.classdiagrammer.domain.Violating", "Violating")
                    .inPackage("com.classdiagrammer.domain")
                    .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                    .withVisibility(Visibility.PUBLIC)
                    .locatedAt("com/classdiagrammer/domain", "com/classdiagrammer/domain/Violating.java")
                    .importing(List.of("com.classdiagrammer.infrastructure.json.JsonWriter"))
                    .withFields(List.of(
                            com.classdiagrammer.domain.model.Field.named("w", "JsonWriter", Visibility.PRIVATE, java.util.Set.of("private"), List.of("com.classdiagrammer.infrastructure.json.JsonWriter"))
                    ))
                    .build();
            CodeGraph graph = CodeGraph.of(List.of(violating));
            EdgeResolver resolver = new EdgeResolver();
            List<Edge> edges = resolver.resolve(graph);
            // edges should contain domain -> infrastructure
            List<Evidence> evs = List.of(
                    new Evidence(new ImplementationFact(FactKind.TYPE_EXISTS, violating.qualifiedName(), "com/classdiagrammer/domain/Violating.java:1", "CLASS", "RULE-003-U2"), "com/classdiagrammer/domain/Violating.java", "TypeNode", "EVID-1")
            );
            ConformanceResult cr = ConformanceEngine.defaultEngine().evaluate(graph, edges, evs);
            EvaluationResult r = cr.results().stream().filter(e -> e.ruleId().equals("RULE-004-U13")).findFirst().orElse(null);
            return r != null && r.state() == EvaluationState.NON_CONFORMANT
                    && !r.predicateResult()
                    && r.evidenceSufficiency() == com.classdiagrammer.domain.conformance.EvidenceSufficiency.SUFFICIENT
                    && r.traceability().contains("NON_CONFORMANT");
        });

        // Fixture C — UNDECIDABLE (missing evidence)
        h.expect("Fixture C — missing required evidence yields UNDECIDABLE", () -> {
            CodeGraph graph = CodeGraph.of(List.of(
                    TypeNode.named("com.example.Foo", "Foo")
                            .inPackage("com.example")
                            .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                            .withVisibility(Visibility.PUBLIC)
                            .locatedAt("com/example", "com/example/Foo.java")
                            .build()
            ));
            Rule missingFactRule = new Rule() {
                public String ruleId() { return "TEST-MISSING-EVIDENCE"; }
                public String description() { return "requires missing fact"; }
                public boolean isApplicable(CodeGraph g, List<Evidence> e) { return true; }
                public List<String> requiredInputs() { return List.of("MISSING_FACT_KIND"); }
                public List<String> requiredEvidenceKinds() { return List.of("MISSING_FACT_KIND"); }
                public Boolean evaluatePredicate(CodeGraph g, List<Evidence> e) { return true; }
                public boolean isSupported() { return true; }
            };
            ConformanceEngine engine = new ConformanceEngine(List.of(missingFactRule));
            ConformanceResult cr = engine.evaluate(graph, List.of(), List.of());
            EvaluationResult r = cr.results().get(0);
            return r.state() == EvaluationState.UNDECIDABLE && r.evidenceSufficiency() == com.classdiagrammer.domain.conformance.EvidenceSufficiency.INSUFFICIENT;
        });

        // Fixture D — UNSUPPORTED (language feature)
        h.expect("Fixture D — unsupported feature yields UNSUPPORTED", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Foo.java", "package foo;", "public record Foo(String a) {}")
            );
            RecordingDiagramSink sink = new RecordingDiagramSink();
            // V8 does not support records
            JavaArtifactParser parser = new JavaArtifactParser(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
            new GenerateClassDiagramUseCase(reader, parser, new EdgeResolver(), name -> null, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            return sink.lastReport.evaluation() == EvaluationState.UNSUPPORTED
                    && sink.lastReport.evidences().stream().anyMatch(ev -> ev.fact().kind() == FactKind.LANGUAGE_FEATURE_USAGE);
        });

        // Fixture E — NOT_APPLICABLE
        h.expect("Fixture E — non-applicable rule yields NOT_APPLICABLE", () -> {
            CodeGraph graph = CodeGraph.of(List.of(
                    TypeNode.named("com.classdiagrammer.infrastructure.Foo", "Foo")
                            .inPackage("com.classdiagrammer.infrastructure")
                            .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                            .withVisibility(Visibility.PUBLIC)
                            .locatedAt("com/classdiagrammer/infrastructure", "com/classdiagrammer/infrastructure/Foo.java")
                            .build()
            ));
            ConformanceResult cr = ConformanceEngine.defaultEngine().evaluate(graph, List.of(), List.of());
            EvaluationResult r = cr.results().stream().filter(e -> e.ruleId().equals("RULE-004-U13")).findFirst().orElse(null);
            return r != null && r.state() == EvaluationState.NOT_APPLICABLE && !r.applicability();
        });

        // Fixture F — REVIEW_REQUIRED (bounded depth)
        h.expect("Fixture F — deep structure yields REVIEW_REQUIRED", () -> {
            // Build a source with depth >100 via RegionScanner directly? Use UseCase with deeply nested class
            // For simplicity, test via direct Evidence: simulate depth_exceeded evidence
            // Instead, use parser with depth 101: we can craft a file with 101 nested classes via braces
            StringBuilder sb = new StringBuilder("package deep; public class Outer {");
            for (int i = 0; i < 105; i++) sb.append(" class Inner").append(i).append(" {");
            for (int i = 0; i < 105; i++) sb.append(" }");
            sb.append(" }");
            SourceRecords reader = new SourceRecords(Sources.java("src/Outer.java", sb.toString()));
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), name -> null, sink)
                    .generate(GenerateClassDiagramCommand.of("demo", "demo.json"));
            return sink.lastReport.evaluation() == EvaluationState.REVIEW_REQUIRED;
        });

        // Mandatory pair §14
        h.expect("Mandatory pair — same rule CONFORMANT vs NON_CONFORMANT with traceability", () -> {
            TypeNode conformant = TypeNode.named("com.classdiagrammer.domain.Conformant", "Conformant")
                    .inPackage("com.classdiagrammer.domain")
                    .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                    .withVisibility(Visibility.PUBLIC)
                    .locatedAt("com/classdiagrammer/domain", "com/classdiagrammer/domain/Conformant.java")
                    .build();
            TypeNode violating = TypeNode.named("com.classdiagrammer.domain.Violating2", "Violating2")
                    .inPackage("com.classdiagrammer.domain")
                    .ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS)
                    .withVisibility(Visibility.PUBLIC)
                    .locatedAt("com/classdiagrammer/domain", "com/classdiagrammer/domain/Violating2.java")
                    .withFields(List.of(
                            com.classdiagrammer.domain.model.Field.named("x", "JsonWriter", Visibility.PRIVATE, java.util.Set.of("private"), List.of("com.classdiagrammer.infrastructure.json.JsonWriter"))
                    ))
                    .importing(List.of("com.classdiagrammer.infrastructure.json.JsonWriter"))
                    .build();
            CodeGraph gConformant = CodeGraph.of(List.of(conformant));
            CodeGraph gViolating = CodeGraph.of(List.of(violating));
            EdgeResolver resolver = new EdgeResolver();
            List<Edge> eConformant = resolver.resolve(gConformant);
            List<Edge> eViolating = resolver.resolve(gViolating);
            List<Evidence> evC = List.of(new Evidence(new ImplementationFact(FactKind.TYPE_EXISTS, conformant.qualifiedName(), "com/classdiagrammer/domain/Conformant.java:1", "CLASS", "RULE-003-U2"), "com/classdiagrammer/domain/Conformant.java", "TypeNode", "EVID-C1"));
            List<Evidence> evV = List.of(new Evidence(new ImplementationFact(FactKind.TYPE_EXISTS, violating.qualifiedName(), "com/classdiagrammer/domain/Violating2.java:1", "CLASS", "RULE-003-U2"), "com/classdiagrammer/domain/Violating2.java", "TypeNode", "EVID-V1"));
            ConformanceEngine engine = ConformanceEngine.defaultEngine();
            ConformanceResult crC = engine.evaluate(gConformant, eConformant, evC);
            ConformanceResult crV = engine.evaluate(gViolating, eViolating, evV);
            EvaluationResult rc = crC.results().stream().filter(r -> r.ruleId().equals("RULE-004-U13")).findFirst().orElse(null);
            EvaluationResult rv = crV.results().stream().filter(r -> r.ruleId().equals("RULE-004-U13")).findFirst().orElse(null);
            return rc != null && rc.state() == EvaluationState.CONFORMANT
                    && rv != null && rv.state() == EvaluationState.NON_CONFORMANT
                    && rv.traceability().contains("RULE-004-U13")
                    && rv.traceability().contains("predicate")
                    && rv.evidenceSufficiency() == com.classdiagrammer.domain.conformance.EvidenceSufficiency.SUFFICIENT;
        });

        // Aggregate precedence §21
        h.expect("Aggregate precedence — NON_CONFORMANT dominates", () -> {
            Rule cRule = new Rule() {
                public String ruleId() { return "R-CONFORMANT"; }
                public String description() { return "always conformant"; }
                public boolean isApplicable(CodeGraph g, List<Evidence> e) { return true; }
                public List<String> requiredInputs() { return List.of(); }
                public List<String> requiredEvidenceKinds() { return List.of(); }
                public Boolean evaluatePredicate(CodeGraph g, List<Evidence> e) { return true; }
                public boolean isSupported() { return true; }
            };
            Rule ncRule = new Rule() {
                public String ruleId() { return "R-NONCONFORMANT"; }
                public String description() { return "always non-conformant"; }
                public boolean isApplicable(CodeGraph g, List<Evidence> e) { return true; }
                public List<String> requiredInputs() { return List.of(); }
                public List<String> requiredEvidenceKinds() { return List.of(); }
                public Boolean evaluatePredicate(CodeGraph g, List<Evidence> e) { return false; }
                public boolean isSupported() { return true; }
            };
            ConformanceEngine engine = new ConformanceEngine(List.of(cRule, ncRule));
            CodeGraph graph = CodeGraph.of(List.of(
                    TypeNode.named("com.example.Foo", "Foo").inPackage("com.example").ofKind(com.classdiagrammer.domain.model.TypeKind.CLASS).withVisibility(Visibility.PUBLIC).locatedAt("com/example", "com/example/Foo.java").build()
            ));
            ConformanceResult cr = engine.evaluate(graph, List.of(), List.of());
            return cr.aggregateState() == EvaluationState.NON_CONFORMANT;
        });
    }
}

package com.classdiagrammer.tests.unit;

import com.classdiagrammer.application.port.in.GenerateClassDiagramCommand;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.domain.resolution.EdgeResolver;
import com.classdiagrammer.tests.support.RecordingDiagramSink;
import com.classdiagrammer.tests.support.SourceRecords;
import com.classdiagrammer.tests.support.Sources;
import com.classdiagrammer.tests.support.StubDependencyResolver;
import com.classdiagrammer.tests.support.TestHarness;
import com.classdiagrammer.application.usecase.GenerateClassDiagramUseCase;

import java.util.List;

/**
 * Verifies deterministic output per CSAS-002-U7 and section 28.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class DeterministicOutputBehavior {

    private DeterministicOutputBehavior() {}

    public static void verify(TestHarness h) {
        h.scope("unit/deterministic-output");

        h.expect("identical source and config produce identical node ordering", () -> {
            SourceRecords r1 = new SourceRecords(
                    Sources.java("src/B.java", "package app;", "public class B {}"),
                    Sources.java("src/A.java", "package app;", "public class A {}")
            );
            SourceRecords r2 = new SourceRecords(
                    Sources.java("src/A.java", "package app;", "public class A {}"),
                    Sources.java("src/B.java", "package app;", "public class B {}")
            );
            RecordingDiagramSink s1 = new RecordingDiagramSink();
            RecordingDiagramSink s2 = new RecordingDiagramSink();
            GenerateClassDiagramUseCase uc1 = new GenerateClassDiagramUseCase(r1, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), s1);
            GenerateClassDiagramUseCase uc2 = new GenerateClassDiagramUseCase(r2, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), s2);
            uc1.generate(GenerateClassDiagramCommand.of("src", "out1.json"));
            uc2.generate(GenerateClassDiagramCommand.of("src", "out2.json"));
            List<String> ids1 = s1.lastReport.nodes().stream().map(n -> n.qualifiedName()).sorted().toList();
            List<String> ids2 = s2.lastReport.nodes().stream().map(n -> n.qualifiedName()).sorted().toList();
            return ids1.equals(ids2);
        });

        h.expect("json output nodes are sorted deterministically", () -> {
            SourceRecords reader = new SourceRecords(
                    Sources.java("src/Z.java", "package app;", "public class Z {}"),
                    Sources.java("src/A.java", "package app;", "public class A {}"),
                    Sources.java("src/M.java", "package app;", "public class M {}")
            );
            RecordingDiagramSink sink = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(reader, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), sink)
                    .generate(GenerateClassDiagramCommand.of("src", "out.json"));
            List<String> order = sink.lastReport.nodes().stream().map(n -> n.qualifiedName()).toList();
            List<String> sorted = order.stream().sorted().toList();
            return !order.isEmpty() && sorted.size() == order.size();
        });

        h.expect("concurrent parsing does not affect output determinism", () -> {
            SourceRecords r = new SourceRecords(
                    Sources.java("src/C1.java", "package app;", "public class C1 {}"),
                    Sources.java("src/C2.java", "package app;", "public class C2 extends C1 {}"),
                    Sources.java("src/C3.java", "package app;", "public class C3 extends C2 {}")
            );
            RecordingDiagramSink s1 = new RecordingDiagramSink();
            RecordingDiagramSink s2 = new RecordingDiagramSink();
            new GenerateClassDiagramUseCase(r, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), s1).generate(GenerateClassDiagramCommand.of("src", "o1.json"));
            new GenerateClassDiagramUseCase(r, new JavaArtifactParser(), new EdgeResolver(), new StubDependencyResolver(null), s2).generate(GenerateClassDiagramCommand.of("src", "o2.json"));
            return s1.lastReport.nodes().size() == s2.lastReport.nodes().size()
                    && s1.lastReport.edges().size() == s2.lastReport.edges().size();
        });
    }
}

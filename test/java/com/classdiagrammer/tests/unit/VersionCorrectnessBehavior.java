package com.classdiagrammer.tests.unit;

import com.classdiagrammer.domain.evidence.EvaluationState;
import com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.infrastructure.parsing.LanguageCapabilities;
import com.classdiagrammer.tests.support.TestHarness;

/**
 * Verifies version-correct parsing per CSAS-002-U12 and CSAS-007-U1.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class VersionCorrectnessBehavior {

    private VersionCorrectnessBehavior() {}

    public static void verify(TestHarness h) {
        h.scope("unit/version-correctness");

        h.expect("java 8 parser rejects records as unsupported", () -> {
            JavaArtifactParser parser = new JavaArtifactParser(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
            SourceFile file = new SourceFile("app", "app/R.java", "package app; record R(int x) {}");
            try {
                parser.parse(file);
                return false;
            } catch (UnsupportedLanguageFeatureException e) {
                return e.feature().name().equals("RECORD") && e.javaVersion().equals("8");
            }
        });

        h.expect("java 8 parser rejects text blocks as unsupported", () -> {
            JavaArtifactParser parser = new JavaArtifactParser(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
            SourceFile file = new SourceFile("app", "app/T.java", "package app; class T { String s = \"\"\"hello\"\"\"; }");
            try {
                parser.parse(file);
                return false;
            } catch (UnsupportedLanguageFeatureException e) {
                return e.feature().name().equals("TEXT_BLOCK");
            }
        });

        h.expect("java 8 parser rejects sealed types as unsupported", () -> {
            JavaArtifactParser parser = new JavaArtifactParser(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
            SourceFile file = new SourceFile("app", "app/S.java", "package app; sealed class S permits C {} final class C extends S {}");
            try {
                parser.parse(file);
                return false;
            } catch (UnsupportedLanguageFeatureException e) {
                return true;
            }
        });

        h.expect("java 11 parser still rejects records and sealed", () -> {
            JavaArtifactParser p11 = new JavaArtifactParser(JavaVersion.V11, LanguageCapabilities.forVersion(JavaVersion.V11));
            SourceFile r = new SourceFile("app", "app/R.java", "package app; record R(int x) {}");
            SourceFile s = new SourceFile("app", "app/S.java", "package app; sealed class S permits C {}");
            boolean rFail = false, sFail = false;
            try { p11.parse(r); } catch (UnsupportedLanguageFeatureException e) { rFail = true; }
            try { p11.parse(s); } catch (UnsupportedLanguageFeatureException e) { sFail = true; }
            return rFail && sFail;
        });

        h.expect("java 17 parser accepts records, sealed and text blocks", () -> {
            JavaArtifactParser p17 = new JavaArtifactParser(JavaVersion.V17, LanguageCapabilities.forVersion(JavaVersion.V17));
            SourceFile r = new SourceFile("app", "app/R.java", "package app; record R(int x) {}");
            SourceFile s = new SourceFile("app", "app/S.java", "package app; sealed class S permits C {} final class C extends S {}");
            SourceFile t = new SourceFile("app", "app/T.java", "package app; class T { String s = \"\"\"hi\"\"\"; }");
            return p17.parse(r).size() == 1 && p17.parse(s).size() == 2 && p17.parse(t).size() == 1;
        });

        h.expect("java version configuration is behaviorally effective via LanguageCapabilities", () -> {
            // same source, different version => different outcome
            String rec = "package app; record R(int x) {}";
            SourceFile file = new SourceFile("app", "app/R.java", rec);
            JavaArtifactParser v8 = new JavaArtifactParser(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
            JavaArtifactParser v17 = new JavaArtifactParser(JavaVersion.V17, LanguageCapabilities.forVersion(JavaVersion.V17));
            boolean v8Fails = false;
            try { v8.parse(file); } catch (UnsupportedLanguageFeatureException e) { v8Fails = true; }
            boolean v17Ok = false;
            try { v17.parse(file); v17Ok = true; } catch (Exception e) {}
            return v8Fails && v17Ok;
        });

        h.expect("unsupported evaluation is distinct from undecidable", () -> {
            return EvaluationState.UNSUPPORTED != EvaluationState.UNDECIDABLE
                    && EvaluationState.UNSUPPORTED != EvaluationState.CONFORMANT;
        });
    }
}

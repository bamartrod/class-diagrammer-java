package com.classdiagrammer.tests;

import com.classdiagrammer.tests.adapter.ArtifactRoutingBehavior;
import com.classdiagrammer.tests.adapter.BuildMetadataBehavior;
import com.classdiagrammer.tests.adapter.FileDiscoveryBehavior;
import com.classdiagrammer.tests.adapter.JarIndexBehavior;
import com.classdiagrammer.tests.adapter.JsonEmissionBehavior;
import com.classdiagrammer.tests.adapter.VelocityTemplateBehavior;
import com.classdiagrammer.tests.adapter.XFormsDocumentBehavior;
import com.classdiagrammer.tests.architecture.HexagonalConformanceBehavior;
import com.classdiagrammer.tests.support.TestHarness;
import com.classdiagrammer.tests.unit.CodeGraphIntegrity;
import com.classdiagrammer.tests.unit.DeterministicOutputBehavior;
import com.classdiagrammer.tests.unit.EvidenceSufficiencyBehavior;
import com.classdiagrammer.tests.unit.InheritanceEdgesBehavior;
import com.classdiagrammer.tests.unit.JavaVersionBehavior;
import com.classdiagrammer.tests.unit.SourceInterpretationBehavior;
import com.classdiagrammer.tests.unit.TypeNodeIntegrity;
import com.classdiagrammer.tests.unit.VersionCorrectnessBehavior;
import com.classdiagrammer.tests.usecase.EdgeEnrichmentBehavior;
import com.classdiagrammer.tests.usecase.GenerationFlowBehavior;

/**
 * Behavior verification suite for TestSuites.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class TestSuites {

    private TestSuites() {
    }

    public static void main(String[] args) {
        TestHarness harness = new TestHarness();
        TypeNodeIntegrity.verify(harness);
        CodeGraphIntegrity.verify(harness);
        InheritanceEdgesBehavior.verify(harness);
        SourceInterpretationBehavior.verify(harness);
        JavaVersionBehavior.verify(harness);
        VersionCorrectnessBehavior.verify(harness);
        EvidenceSufficiencyBehavior.verify(harness);
        DeterministicOutputBehavior.verify(harness);
        GenerationFlowBehavior.verify(harness);
        FileDiscoveryBehavior.verify(harness);
        BuildMetadataBehavior.verify(harness);
        JarIndexBehavior.verify(harness);
        VelocityTemplateBehavior.verify(harness);
        XFormsDocumentBehavior.verify(harness);
        ArtifactRoutingBehavior.verify(harness);
        JsonEmissionBehavior.verify(harness);
        EdgeEnrichmentBehavior.verify(harness);
        HexagonalConformanceBehavior.verify(harness);

        harness.report(System.out);
        System.out.println();
        System.out.println("Checks: " + harness.total()
                + " | Failures: " + harness.failedCount());
        if (harness.failedCount() > 0) {
            System.exit(1);
        }
    }
}

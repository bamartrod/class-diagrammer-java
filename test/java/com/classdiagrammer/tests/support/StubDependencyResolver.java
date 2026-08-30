package com.classdiagrammer.tests.support;

import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.domain.model.ArtifactRef;

/**
 * Test support utility StubDependencyResolver.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class StubDependencyResolver implements DependencyResolver {

    private final ArtifactRef answer;

    public StubDependencyResolver(ArtifactRef answer) {
        this.answer = answer;
    }

    public ArtifactRef locate(String className) {
        return answer;
    }
}

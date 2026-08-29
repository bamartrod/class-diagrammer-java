package com.classdiagrammer.infrastructure.dependencies;

import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.domain.model.ArtifactRef;

public final class ClasspathArtifactResolver implements DependencyResolver {

    private final LocalRepositoryIndex index;

    public ClasspathArtifactResolver(LocalRepositoryIndex index) {
        if (index == null) {
            throw new IllegalArgumentException("repository index is required");
        }
        this.index = index;
    }

    public ArtifactRef locate(String className) {
        DeclaredDependency dependency = index.locate(className);
        return dependency == null ? null : new ArtifactRef(
                dependency.groupId(), dependency.artifactId(), dependency.version());
    }
}

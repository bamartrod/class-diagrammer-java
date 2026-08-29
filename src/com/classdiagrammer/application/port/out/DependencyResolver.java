package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.ArtifactRef;

public interface DependencyResolver {

    ArtifactRef locate(String className);
}

package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.ArtifactRef;

/**
 * Output port DependencyResolver abstracting an infrastructure concern.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface DependencyResolver {

    ArtifactRef locate(String className);
}

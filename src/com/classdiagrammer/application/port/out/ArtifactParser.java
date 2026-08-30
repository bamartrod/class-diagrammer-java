package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;

import java.util.List;

/**
 * Output port ArtifactParser abstracting an infrastructure concern.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface ArtifactParser {

    boolean accepts(SourceFile file);

    List<TypeNode> parse(SourceFile file);
}

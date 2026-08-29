package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;

import java.util.List;

public interface ArtifactParser {

    boolean accepts(SourceFile file);

    List<TypeNode> parse(SourceFile file);
}

package com.classdiagrammer.infrastructure.parsing.hibernate;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;

import java.util.Collections;
import java.util.List;

public final class HbmArtifactParser implements ArtifactParser {

    public boolean accepts(SourceFile source) {
        return source != null
                && (source.file().endsWith(".hbm.xml") || source.file().endsWith(".hbm"));
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
        return Collections.emptyList();
    }
}

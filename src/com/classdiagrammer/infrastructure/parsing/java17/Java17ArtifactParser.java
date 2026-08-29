package com.classdiagrammer.infrastructure.parsing.java17;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.LanguageCapabilities;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;

import java.util.List;

public final class Java17ArtifactParser implements ArtifactParser {

    private final JavaArtifactParser delegate =
            new JavaArtifactParser(LanguageCapabilities.forVersion(JavaVersion.V17));

    public boolean accepts(SourceFile source) {
        return source != null && source.file().endsWith(".java");
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
        return delegate.parse(source);
    }
}

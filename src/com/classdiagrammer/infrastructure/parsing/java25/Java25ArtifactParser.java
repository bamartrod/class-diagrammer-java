package com.classdiagrammer.infrastructure.parsing.java25;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.LanguageCapabilities;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;

import java.util.List;

/**
 * Artifact parser for Java25ArtifactParser delegating to the core Java parser with version-specific capabilities.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Java25ArtifactParser implements ArtifactParser {

    private final JavaArtifactParser delegate =
            new JavaArtifactParser(JavaVersion.V25, LanguageCapabilities.forVersion(JavaVersion.V25));

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

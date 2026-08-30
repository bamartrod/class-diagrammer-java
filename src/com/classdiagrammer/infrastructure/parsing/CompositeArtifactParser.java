package com.classdiagrammer.infrastructure.parsing;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite router dispatching files to the appropriate ArtifactParser.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class CompositeArtifactParser implements ArtifactParser {

    private final List<ArtifactParser> delegates;

    public CompositeArtifactParser(List<ArtifactParser> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("at least one parser is required");
        }
        this.delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
    }

    public boolean accepts(SourceFile file) {
        return delegateFor(file) != null;
    }

    public List<TypeNode> parse(SourceFile file) {
        ArtifactParser delegate = delegateFor(file);
        return delegate == null ? Collections.<TypeNode>emptyList() : delegate.parse(file);
    }

    private ArtifactParser delegateFor(SourceFile file) {
        for (ArtifactParser delegate : delegates) {
            if (delegate.accepts(file)) {
                return delegate;
            }
        }
        return null;
    }
}

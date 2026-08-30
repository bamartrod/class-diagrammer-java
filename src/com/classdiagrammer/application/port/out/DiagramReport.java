package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.CodeGraph;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Output port DiagramReport abstracting an infrastructure concern.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class DiagramReport {

    private final String sourceRoot;
    private final List<TypeNode> nodes;
    private final List<Edge> edges;
    private final String generatedAtUtc;

    private DiagramReport(String sourceRoot, List<TypeNode> nodes,
                          List<Edge> edges, String generatedAtUtc) {
        this.sourceRoot = sourceRoot;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.edges = Collections.unmodifiableList(new ArrayList<>(edges));
        this.generatedAtUtc = generatedAtUtc;
    }

    public static DiagramReport capture(String sourceRoot, CodeGraph graph,
                                        List<Edge> resolvedEdges, Instant generatedAt) {
        if (graph == null) {
            throw new IllegalArgumentException("code graph is required");
        }
        if (resolvedEdges == null) {
            throw new IllegalArgumentException("resolved edges are required");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("generation instant is required");
        }
        return new DiagramReport(sourceRoot, graph.nodes(), resolvedEdges, generatedAt.toString());
    }

    public String sourceRoot() {
        return sourceRoot;
    }

    public List<TypeNode> nodes() {
        return nodes;
    }

    public List<Edge> edges() {
        return edges;
    }

    public String generatedAtUtc() {
        return generatedAtUtc;
    }
}

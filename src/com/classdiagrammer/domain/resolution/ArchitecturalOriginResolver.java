package com.classdiagrammer.domain.resolution;

import com.classdiagrammer.application.port.out.DependencyResolver;
import com.classdiagrammer.domain.evidence.Evidence;
import com.classdiagrammer.domain.evidence.FactKind;
import com.classdiagrammer.domain.evidence.ImplementationFact;
import com.classdiagrammer.domain.model.ArtifactRef;
import com.classdiagrammer.domain.model.Edge;
import com.classdiagrammer.domain.model.TypeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Enriches edges with architectural origin (project/external/unknown).
 * Separate from FQN qualification to respect single responsibility.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ArchitecturalOriginResolver {

    private final DependencyResolver dependencyResolver;

    public ArchitecturalOriginResolver(DependencyResolver dependencyResolver) {
        this.dependencyResolver = Objects.requireNonNull(dependencyResolver, "dependencyResolver is required");
    }

    public EnrichmentResult enrich(List<Edge> edges, List<TypeNode> nodes) {
        Objects.requireNonNull(edges, "edges is required");
        Objects.requireNonNull(nodes, "nodes is required");
        Map<String, List<String>> importsByNode = new HashMap<>();
        for (TypeNode node : nodes) {
            importsByNode.put(node.qualifiedName(), node.imports());
        }
        List<Edge> enriched = new ArrayList<>();
        List<Evidence> evidences = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.isResolved()) {
                enriched.add(edge.withProjectOrigin());
                evidences.add(archEvidence(edge.from(), edge.to(), "project", edge));
                continue;
            }
            String qualified = TypeQualifier.qualify(edge.from(), edge.to(), importsByNode);
            ArtifactRef provider = dependencyResolver.locate(qualified);
            if (provider == null) {
                enriched.add(edge);
                evidences.add(archEvidence(edge.from(), qualified, "unknown", edge));
            } else {
                enriched.add(edge.asExternal(qualified, provider));
                evidences.add(archEvidence(edge.from(), qualified, "external", edge));
            }
        }
        return new EnrichmentResult(enriched, evidences);
    }

    private Evidence archEvidence(String from, String to, String origin, Edge edge) {
        ImplementationFact fact = new ImplementationFact(
                FactKind.ARCHITECTURAL_ORIGIN,
                from + "->" + to,
                from,
                origin,
                "RULE-006-U4"
        );
        return new Evidence(fact, edge.from(), "ArchitecturalOriginResolver.enrich", "ORIGIN-" + from.hashCode() + "-" + to.hashCode());
    }

    public static final class EnrichmentResult {
        private final List<Edge> edges;
        private final List<Evidence> evidences;

        public EnrichmentResult(List<Edge> edges, List<Evidence> evidences) {
            this.edges = List.copyOf(Objects.requireNonNull(edges));
            this.evidences = List.copyOf(Objects.requireNonNull(evidences));
        }

        public List<Edge> edges() { return edges; }
        public List<Evidence> evidences() { return evidences; }
    }
}

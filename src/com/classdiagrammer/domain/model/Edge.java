package com.classdiagrammer.domain.model;

import java.util.Objects;

/**
 * Directed relation between two types (extends, implements, permits, imports).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class Edge {

    private final String from;
    private final String to;
    private final TypeRelationKind kind;
    private final boolean resolved;
    private final EdgeOrigin origin;
    private final ArtifactRef artifact;

    public Edge(String from, String to, TypeRelationKind kind, boolean resolved) {
        this(from, to, kind, resolved,
                resolved ? EdgeOrigin.PROJECT : EdgeOrigin.UNKNOWN, null);
    }

    public Edge(String from, String to, TypeRelationKind kind,
                boolean resolved, EdgeOrigin origin, ArtifactRef artifact) {
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("edge source is required");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("edge target is required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("edge kind is required");
        }
        if (origin == null) {
            throw new IllegalArgumentException("edge origin is required");
        }
        this.from = from.trim();
        this.to = to.trim();
        this.kind = kind;
        this.resolved = resolved;
        this.origin = origin;
        this.artifact = artifact;
    }

    public static Edge inheritance(String from, String to, boolean resolved) {
        return new Edge(from, to, TypeRelationKind.EXTENDS, resolved);
    }

    public static Edge realization(String from, String to, boolean resolved) {
        return new Edge(from, to, TypeRelationKind.IMPLEMENTS, resolved);
    }

    public Edge withProjectOrigin() {
        return new Edge(from, to, kind, true, EdgeOrigin.PROJECT, null);
    }

    public Edge asExternal(ArtifactRef provider) {
        if (provider == null) {
            throw new IllegalArgumentException("artifact provider is required");
        }
        return new Edge(from, to, kind, true, EdgeOrigin.EXTERNAL, provider);
    }

    public Edge asExternal(String qualifiedTarget, ArtifactRef provider) {
        if (provider == null) {
            throw new IllegalArgumentException("artifact provider is required");
        }
        var target = qualifiedTarget == null || qualifiedTarget.trim().isEmpty()
                ? to : qualifiedTarget.trim();
        return new Edge(from, target, kind, true, EdgeOrigin.EXTERNAL, provider);
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public TypeRelationKind kind() {
        return kind;
    }

    public boolean isResolved() {
        return resolved;
    }

    public EdgeOrigin origin() {
        return origin;
    }

    public ArtifactRef artifact() {
        return artifact;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Edge that)) {
            return false;
        }
        return from.equals(that.from) && to.equals(that.to) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, kind);
    }

    @Override
    public String toString() {
        var suffix = artifact == null ? "" : " [" + artifact + "]";
        return from + " -" + kind.jsonName() + "-> " + to + suffix;
    }
}

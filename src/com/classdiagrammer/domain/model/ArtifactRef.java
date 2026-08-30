package com.classdiagrammer.domain.model;

import java.util.Objects;

/**
 * Value object referencing an external artifact (groupId, artifactId, version).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class ArtifactRef {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public ArtifactRef(String groupId, String artifactId, String version) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (artifactId == null || artifactId.trim().isEmpty()) {
            throw new IllegalArgumentException("artifactId is required");
        }
        this.groupId = groupId.trim();
        this.artifactId = artifactId.trim();
        this.version = version == null ? "" : version.trim();
    }

    public String groupId() { return groupId; }
    public String artifactId() { return artifactId; }
    public String version() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactRef)) return false;
        ArtifactRef that = (ArtifactRef) o;
        return groupId.equals(that.groupId) && artifactId.equals(that.artifactId) && version.equals(that.version);
    }

    @Override
    public int hashCode() { return Objects.hash(groupId, artifactId, version); }

    @Override
    public String toString() { return groupId + ":" + artifactId + ":" + version; }
}

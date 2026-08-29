package com.classdiagrammer.domain.model;

public record ArtifactRef(String groupId, String artifactId, String version) {

    public ArtifactRef {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (artifactId == null || artifactId.trim().isEmpty()) {
            throw new IllegalArgumentException("artifactId is required");
        }
        groupId = groupId.trim();
        artifactId = artifactId.trim();
        version = version == null ? "" : version.trim();
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}

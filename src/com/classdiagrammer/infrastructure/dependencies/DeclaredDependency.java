package com.classdiagrammer.infrastructure.dependencies;

import java.util.Objects;

/**
 * Dependency resolution component DeclaredDependency.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class DeclaredDependency {

    private final String groupId;
    private final String artifactId;
    private final String version;

    public DeclaredDependency(String groupId, String artifactId, String version) {
        this.groupId = groupId == null ? "" : groupId.trim();
        this.artifactId = artifactId == null ? "" : artifactId.trim();
        this.version = version == null ? "" : version.trim();
    }

    public boolean isComplete() {
        return !groupId.isEmpty() && !artifactId.isEmpty() && !version.isEmpty()
                && !version.contains("$");
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DeclaredDependency)) {
            return false;
        }
        DeclaredDependency that = (DeclaredDependency) other;
        return groupId.equals(that.groupId)
                && artifactId.equals(that.artifactId)
                && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}

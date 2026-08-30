package com.classdiagrammer.infrastructure.dependencies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Dependency resolution component LocalRepositoryIndex.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class LocalRepositoryIndex {

    private final List<IndexedArtifact> indexed = new ArrayList<>();
    private final List<DeclaredDependency> dependencies;
    private boolean loaded;

    public LocalRepositoryIndex(List<DeclaredDependency> dependencies) {
        if (dependencies == null) {
            throw new IllegalArgumentException("declared dependencies are required");
        }
        this.dependencies = new ArrayList<>(dependencies);
    }

    public DeclaredDependency locate(String fullyQualifiedClass) {
        ensureLoaded();
        if (fullyQualifiedClass == null || fullyQualifiedClass.trim().isEmpty()
                || fullyQualifiedClass.indexOf('.') < 0) {
            return null;
        }
        IndexedArtifact best = null;
        int bestLength = -1;
        String candidate = packageNameOf(fullyQualifiedClass.trim());
        while (!candidate.isEmpty()) {
            for (IndexedArtifact artifact : indexed) {
                if (artifact.packages.contains(candidate)
                        && candidate.length() > bestLength) {
                    best = artifact;
                    bestLength = candidate.length();
                }
            }
            int dot = candidate.lastIndexOf('.');
            candidate = dot < 0 ? "" : candidate.substring(0, dot);
        }
        return best == null ? null : best.dependency;
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (DeclaredDependency dependency : dependencies) {
            Path jar = mavenJar(dependency);
            if (jar == null) {
                jar = gradleJar(dependency);
            }
            if (jar != null) {
                indexed.add(new IndexedArtifact(dependency, readPackages(jar)));
            }
        }
    }

    private Path mavenJar(DeclaredDependency dependency) {
        String localRepo = System.getProperty("maven.repo.local",
                System.getProperty("user.home") + "/.m2/repository");
        String groupPath = dependency.groupId().replace('.', '/');
        Path directory = Paths.get(localRepo, groupPath,
                dependency.artifactId(), dependency.version());
        Path jar = directory.resolve(
                dependency.artifactId() + "-" + dependency.version() + ".jar");
        return Files.isRegularFile(jar) ? jar : null;
    }

    private Path gradleJar(DeclaredDependency dependency) {
        String gradleHome = System.getenv("GRADLE_USER_HOME");
        if (gradleHome == null || gradleHome.trim().isEmpty()) {
            gradleHome = System.getProperty("user.home") + "/.gradle";
        }
        Path versionDir = Paths.get(gradleHome, "caches", "modules-2", "files-2.1",
                dependency.groupId(), dependency.artifactId(), dependency.version());
        if (!Files.isDirectory(versionDir)) {
            return null;
        }
        String expected = dependency.artifactId() + "-" + dependency.version() + ".jar";
        try (java.util.stream.Stream<Path> walked = Files.walk(versionDir)) {
            return walked.filter(p -> p.getFileName().toString().equals(expected)
                    && Files.isRegularFile(p))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private Set<String> readPackages(Path jar) {
        Set<String> packages = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    int slash = name.lastIndexOf('/');
                    if (slash > 0) {
                        packages.add(name.substring(0, slash).replace('/', '.'));
                    }
                }
            }
        } catch (IOException e) {
            return packages;
        }
        return packages;
    }

    private String packageNameOf(String fullyQualifiedClass) {
        int dot = fullyQualifiedClass.lastIndexOf('.');
        return dot < 0 ? "" : fullyQualifiedClass.substring(0, dot);
    }

    private static final class IndexedArtifact {
        private final DeclaredDependency dependency;
        private final Set<String> packages;

        IndexedArtifact(DeclaredDependency dependency, Set<String> packages) {
            this.dependency = dependency;
            this.packages = packages;
        }
    }
}

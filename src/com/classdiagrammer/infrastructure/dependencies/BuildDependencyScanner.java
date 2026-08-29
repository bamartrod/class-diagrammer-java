package com.classdiagrammer.infrastructure.dependencies;

import java.util.ArrayList;
import java.util.List;

public final class BuildDependencyScanner {

    public List<DeclaredDependency> scan(String sourceRoot) {
        if (sourceRoot == null || sourceRoot.trim().isEmpty()) {
            throw new IllegalArgumentException("source root is required");
        }
        List<DeclaredDependency> found = new ArrayList<>();
        try {
            java.nio.file.Files.walkFileTree(java.nio.file.Paths.get(sourceRoot),
                    new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                        public java.nio.file.FileVisitResult preVisitDirectory(
                                java.nio.file.Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                            String name = dir.getFileName() == null
                                    ? "" : dir.getFileName().toString();
                            return name.startsWith(".")
                                    ? java.nio.file.FileVisitResult.SKIP_SUBTREE
                                    : java.nio.file.FileVisitResult.CONTINUE;
                        }

                        public java.nio.file.FileVisitResult visitFile(
                                java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) {
                            String name = file.getFileName().toString();
                            if (name.equals("pom.xml")) {
                                found.addAll(PomDependencies.read(file));
                            } else if (name.equals("build.gradle") || name.equals("build.gradle.kts")) {
                                found.addAll(GradleDependencies.read(file));
                            }
                            return java.nio.file.FileVisitResult.CONTINUE;
                        }
                    });
        } catch (java.io.IOException e) {
            throw new IllegalStateException("unable to scan build files under " + sourceRoot, e);
        }
        return deduplicated(found);
    }

    private List<DeclaredDependency> deduplicated(List<DeclaredDependency> input) {
        List<DeclaredDependency> unique = new ArrayList<>();
        for (DeclaredDependency candidate : input) {
            if (!unique.contains(candidate)) {
                unique.add(candidate);
            }
        }
        return unique;
    }
}

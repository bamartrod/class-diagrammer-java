package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.infrastructure.dependencies.DeclaredDependency;
import com.classdiagrammer.infrastructure.dependencies.LocalRepositoryIndex;
import com.classdiagrammer.tests.support.TestHarness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class JarIndexBehavior {

    private JarIndexBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("adapter/repositorio-local");

        final DeclaredDependency velocity = new DeclaredDependency(
                "org.apache.velocity", "velocity-engine-core", "2.4.1");
        final DeclaredDependency lib = new DeclaredDependency(
                "org.example", "lib", "1.0");

        h.expect("a jar from local repository responds for its exact package", () ->
                withLocalRepo(velocity, new String[]{
                        "org/apache/velocity/Template.class",
                        "org/apache/velocity/context/InternalContext.class"}, index -> {
                    DeclaredDependency found = index.locate("org.apache.velocity.Template");
                    return found != null
                            && found.groupId().equals("org.apache.velocity")
                            && found.artifactId().equals("velocity-engine-core")
                            && found.version().equals("2.4.1");
                }));

        h.expect("a missing subpackage falls back to published ancestor", () ->
                withLocalRepo(lib, new String[]{"org/example/lib/Core.class"},
                        index -> index.locate("org.example.lib.interno.Maquina") != null));

        h.expect("clases desconocidas y simples no producen artefactos", () ->
                withLocalRepo(lib, new String[]{"org/example/lib/Core.class"}, index ->
                        index.locate("com.nadie.Conoce") == null
                                && index.locate("Simple") == null
                                && index.locate("") == null));
    }

    private interface IndexProbe {
        boolean probe(LocalRepositoryIndex index) throws IOException;
    }

    private static boolean withLocalRepo(DeclaredDependency dependency,
                                         String[] jarEntries, IndexProbe probe) {
        Path repo = scratch();
        String previousRepo = System.getProperty("maven.repo.local");
        try {
            System.setProperty("maven.repo.local", repo.toString());
            writeMavenJar(repo, dependency, jarEntries);
            LocalRepositoryIndex index =
                    new LocalRepositoryIndex(Arrays.asList(dependency));
            return probe.probe(index);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            restore(previousRepo);
            cleanup(repo);
        }
    }

    private static void writeMavenJar(Path repoRoot, DeclaredDependency dependency,
                                      String... entries) throws IOException {
        String groupPath = dependency.groupId().replace('.', '/');
        Path directory = repoRoot.resolve(Paths.get(groupPath,
                dependency.artifactId(), dependency.version()).toString());
        Files.createDirectories(directory);
        Path jar = directory.resolve(dependency.artifactId() + "-"
                + dependency.version() + ".jar");
        try (ZipOutputStream zip = new ZipOutputStream(
                Files.newOutputStream(jar), StandardCharsets.UTF_8)) {
            for (String entry : entries) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
                zip.closeEntry();
            }
        }
    }

    private static void restore(String previousValue) {
        if (previousValue == null) {
            System.clearProperty("maven.repo.local");
        } else {
            System.setProperty("maven.repo.local", previousValue);
        }
    }

    private static Path scratch() {
        try {
            return Files.createTempDirectory("classdiagrammer-m2");
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void cleanup(Path root) {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignoredCleanup) {
                }
            });
        } catch (Exception ignoredSweep) {
        }
    }
}

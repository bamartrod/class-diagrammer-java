package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.infrastructure.dependencies.BuildDependencyScanner;
import com.classdiagrammer.infrastructure.dependencies.DeclaredDependency;
import com.classdiagrammer.tests.support.TestHarness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BuildMetadataBehavior {

    private final static String POM_CON_PROPIEDADES = ""
            + "<project>\n"
            + "  <properties>\n"
            + "    <velocity.version>2.4.1</velocity.version>\n"
            + "  </properties>\n"
            + "  <!-- <dependency><groupId>oculto</groupId>"
            + "<artifactId>fantasma</artifactId><version>9</version></dependency> -->\n"
            + "  <dependencies>\n"
            + "    <dependency>\n"
            + "      <groupId>org.apache.velocity</groupId>\n"
            + "      <artifactId>velocity-engine-core</artifactId>\n"
            + "      <version>${velocity.version}</version>\n"
            + "    </dependency>\n"
            + "    <dependency>\n"
            + "      <groupId>sin.version</groupId>\n"
            + "      <artifactId>misterio</artifactId>\n"
            + "      <version>${project.version}</version>\n"
            + "    </dependency>\n"
            + "  </dependencies>\n"
            + "</project>\n";

    private BuildMetadataBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("adapter/metadatos-de-build");

        h.expect("un pom con propiedades entrega coordenadas interpoladas", () -> {
            Path root = scratch();
            try {
                write(root, "pom.xml", POM_CON_PROPIEDADES);
                List<DeclaredDependency> found = new BuildDependencyScanner().scan(
                        root.toString());
                return found.size() == 1
                        && found.get(0).toString()
                                .equals("org.apache.velocity:velocity-engine-core:2.4.1");
            } finally {
                cleanup(root);
            }
        });

        h.expect("las notaciones de gradle se leen tal cual estan declaradas", () -> {
            Path root = scratch();
            try {
                write(root, "build.gradle", ""
                        + "dependencies {\n"
                        + "  implementation 'org.apache.commons:commons-lang3:3.14.0'\n"
                        + "  testImplementation(\"junit:junit:4.13.2\")\n"
                        + "}\n");
                List<DeclaredDependency> found = new BuildDependencyScanner().scan(
                        root.toString());
                boolean commons = contains(found,
                        "org.apache.commons:commons-lang3:3.14.0");
                boolean junit = contains(found, "junit:junit:4.13.2");
                return found.size() == 2 && commons && junit;
            } finally {
                cleanup(root);
            }
        });

        h.expect("una raiz sin archivos de build no declara dependencias", () -> {
            Path root = scratch();
            try {
                write(root, "src/A.java", "class A {}");
                return new BuildDependencyScanner().scan(root.toString()).isEmpty();
            } finally {
                cleanup(root);
            }
        });
    }

    private static boolean contains(List<DeclaredDependency> found, String notation) {
        for (DeclaredDependency dependency : found) {
            if (dependency.toString().equals(notation)) {
                return true;
            }
        }
        return false;
    }

    private static Path scratch() {
        try {
            return Files.createTempDirectory("classdiagrammer-build");
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void write(Path root, String relative, String content) {
        try {
            Path target = root.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
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

package com.classdiagrammer.tests.adapter;

import com.classdiagrammer.application.port.out.SourceCodeReader;
import com.classdiagrammer.infrastructure.filesystem.FileSystemSourceReader;
import com.classdiagrammer.tests.support.TestHarness;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileDiscoveryBehavior {

    private FileDiscoveryBehavior() {
    }

    public static void verify(TestHarness h) {
        h.scope("adapter/lectura-de-fuentes");

        h.expect("los .java se descubren en profundidad con rutas relativas", () -> {
            Path root = temporaryFolder();
            try {
                write(root, "src/A.java", "package a;");
                write(root, "src/deep/nest/B.java", "package b;");
                write(root, "docs/notes.txt", "hola");
                SourceCodeReader reader = new FileSystemSourceReader();
                List<com.classdiagrammer.domain.model.SourceFile> found =
                        reader.readAll(root.toString());
                return found.size() == 2
                        && hasFile(found, "src/A.java")
                        && hasFile(found, "src/deep/nest/B.java");
            } finally {
                deleteTree(root);
            }
        });
        h.expect("las plantillas velocity y los documentos xhtml tambien viajan", () -> {
            Path root = temporaryFolder();
            try {
                write(root, "views/p.vtl", "$x");
                write(root, "views/q.vm", "#macro(m $a)\n#end");
                write(root, "forms/f.xhtml", "<html/>");
                write(root, "conf/app.properties", "x=1");
                write(root, "pom.xml", "<project/>");
                SourceCodeReader reader = new FileSystemSourceReader();
                List<com.classdiagrammer.domain.model.SourceFile> found =
                        reader.readAll(root.toString());
                return found.size() == 4
                        && hasFile(found, "views/p.vtl")
                        && hasFile(found, "views/q.vm")
                        && hasFile(found, "forms/f.xhtml")
                        && hasFile(found, "pom.xml");
            } finally {
                deleteTree(root);
            }
        });
        h.expect("las carpetas ocultas quedan fuera del barrido", () -> {
            Path root = temporaryFolder();
            try {
                write(root, ".git/C.java", "package c;");
                write(root, ".idea/D.java", "package d;");
                write(root, "src/F.java", "package f;");
                SourceCodeReader reader = new FileSystemSourceReader();
                List<com.classdiagrammer.domain.model.SourceFile> found =
                        reader.readAll(root.toString());
                return found.size() == 1 && hasFile(found, "src/F.java");
            } finally {
                deleteTree(root);
            }
        });
        h.expect("una carpeta llamada out con fuentes legitimas se lee completa", () -> {
            Path root = temporaryFolder();
            try {
                write(root, "app/port/out/Y.java", "package y;");
                write(root, "app/port/in/Z.java", "package z;");
                SourceCodeReader reader = new FileSystemSourceReader();
                List<com.classdiagrammer.domain.model.SourceFile> found =
                        reader.readAll(root.toString());
                return found.size() == 2
                        && hasFile(found, "app/port/out/Y.java")
                        && hasFile(found, "app/port/in/Z.java");
            } finally {
                deleteTree(root);
            }
        });
        h.expect("el contenido llega intacto incluyendo acentos y llaves", () -> {
            Path root = temporaryFolder();
            try {
                String expected = "package g; // ñandú { }";
                write(root, "src/G.java", expected);
                SourceCodeReader reader = new FileSystemSourceReader();
                return expected.equals(reader.readAll(root.toString()).get(0).content());
            } finally {
                deleteTree(root);
            }
        });
        h.expect("una raiz inexistente se rechaza con claridad", () -> {
            try {
                new FileSystemSourceReader().readAll("esta/carpeta/no/existe");
                return false;
            } catch (IllegalArgumentException expected) {
                return true;
            }
        });
    }

    private static Path temporaryFolder() {
        try {
            return Files.createTempDirectory("classdiagrammer-lectura");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static void write(Path root, String relativePath, String content) {
        try {
            Path target = root.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(target, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static boolean hasFile(Iterable<com.classdiagrammer.domain.model.SourceFile> files,
                                   String relativePath) {
        for (com.classdiagrammer.domain.model.SourceFile file : files) {
            if (file.file().equals(relativePath)) {
                return true;
            }
        }
        return false;
    }

    private static void deleteTree(Path root) {
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

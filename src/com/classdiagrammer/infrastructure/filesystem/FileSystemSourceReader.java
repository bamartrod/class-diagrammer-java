package com.classdiagrammer.infrastructure.filesystem;

import com.classdiagrammer.application.port.out.SourceCodeReader;
import com.classdiagrammer.domain.model.SourceFile;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filesystem adapter reading source files from disk.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class FileSystemSourceReader implements SourceCodeReader {

    private static final Set<String> SOURCE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".vm", ".vtl", ".xhtml", ".xforms", ".xml"));

    public List<SourceFile> readAll(String sourceRoot) {
        Path root = Paths.get(sourceRoot);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            throw new IllegalArgumentException("source root does not exist: " + sourceRoot);
        }
        List<SourceFile> collected = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    if (name.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (isSupported(name)) {
                        collected.add(toSourceFile(root, file));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("unable to walk sources under " + sourceRoot, e);
        }
        collected.sort((a, b) -> a.file().compareTo(b.file()));
        return collected;
    }

    private boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        for (String extension : SOURCE_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private SourceFile toSourceFile(Path root, Path file) {
        Path relative = root.relativize(file);
        String posixPath = relative.toString().replace('\\', '/');
        String folder = parentOf(posixPath);
        return new SourceFile(folder, posixPath, readContent(file));
    }

    private String parentOf(String posixPath) {
        int slash = posixPath.lastIndexOf('/');
        return slash < 0 ? "" : posixPath.substring(0, slash);
    }

    private String readContent(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to read source file " + file, e);
        }
    }
}

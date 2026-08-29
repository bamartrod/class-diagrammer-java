package com.classdiagrammer.infrastructure.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GradleDependencies {

    private static final Pattern NOTATION = Pattern.compile(
            "[\"']([\\w.\\-]+):([\\w.\\-]+):([^\"'\\s}]+)[\"']");

    private GradleDependencies() {
    }

    static List<DeclaredDependency> read(Path buildFile) {
        String content;
        try {
            content = new String(Files.readAllBytes(buildFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to read " + buildFile, e);
        }
        return parse(content);
    }

    static List<DeclaredDependency> parse(String content) {
        List<DeclaredDependency> declared = new ArrayList<>();
        Matcher matcher = NOTATION.matcher(content);
        while (matcher.find()) {
            DeclaredDependency dependency = new DeclaredDependency(
                    matcher.group(1), matcher.group(2), matcher.group(3));
            if (dependency.isComplete() && !declared.contains(dependency)) {
                declared.add(dependency);
            }
        }
        return declared;
    }
}

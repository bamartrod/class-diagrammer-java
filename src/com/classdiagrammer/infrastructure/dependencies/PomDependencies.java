package com.classdiagrammer.infrastructure.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency resolution component PomDependencies.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class PomDependencies {

    private static final Pattern PROPERTIES_BLOCK =
            Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
    private static final Pattern PROPERTY =
            Pattern.compile("<([\\w.\\-]+)>([^<]*)</\\1>");
    private static final Pattern DEPENDENCY_BLOCK =
            Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    private static final Pattern COORDINATE =
            Pattern.compile("<(groupId|artifactId|version)>([^<]*)</\\1>");

    private PomDependencies() {
    }

    static List<DeclaredDependency> read(Path pomFile) {
        String content;
        try {
            content = new String(Files.readAllBytes(pomFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to read " + pomFile, e);
        }
        return parse(content);
    }

    static List<DeclaredDependency> parse(String rawPom) {
        String pom = maskComments(rawPom);
        Map<String, String> properties = readProperties(pom);
        List<DeclaredDependency> declared = new ArrayList<>();
        Matcher blocks = DEPENDENCY_BLOCK.matcher(pom);
        while (blocks.find()) {
            Map<String, String> coordinates = new HashMap<>();
            Matcher fields = COORDINATE.matcher(blocks.group(1));
            while (fields.find()) {
                coordinates.putIfAbsent(fields.group(1), fields.group(2).trim());
            }
            DeclaredDependency dependency = new DeclaredDependency(
                    interpolate(coordinates.get("groupId"), properties),
                    interpolate(coordinates.get("artifactId"), properties),
                    interpolate(coordinates.get("version"), properties));
            if (dependency.isComplete()) {
                declared.add(dependency);
            }
        }
        return declared;
    }

    private static Map<String, String> readProperties(String pom) {
        Map<String, String> properties = new HashMap<>();
        Matcher block = PROPERTIES_BLOCK.matcher(pom);
        if (!block.find()) {
            return properties;
        }
        Matcher entries = PROPERTY.matcher(block.group(1));
        while (entries.find()) {
            properties.putIfAbsent(entries.group(1), entries.group(2).trim());
        }
        return properties;
    }

    private static String interpolate(String value, Map<String, String> properties) {
        if (value == null) {
            return "";
        }
        String resolved = value;
        for (Map.Entry<String, String> property : properties.entrySet()) {
            resolved = resolved.replace("${" + property.getKey() + "}",
                    property.getValue());
        }
        return resolved;
    }

    private static String maskComments(String content) {
        return content.replaceAll("(?s)<!--.*?-->", " ");
    }
}

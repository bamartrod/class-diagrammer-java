package com.classdiagrammer.tests.architecture;

import com.classdiagrammer.tests.support.TestHarness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class HexagonalConformanceBehavior {

    private static final Pattern IMPORT_LINE =
            Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.]+)\\s*;");
    private static final Pattern ANNOTATION_LINE = Pattern.compile("^\\s*@[A-Za-z].*$");
    private static final int COHESION_REVIEW_THRESHOLD_LINES = 200;
    private static final String ROOT_PACKAGE = "com.classdiagrammer.";
    private static final List<String> SHARED_ADAPTERS =
            java.util.Arrays.asList("xml");

    private HexagonalConformanceBehavior() {
    }

    public static void verify(TestHarness h) {
        List<SourceUnit> units = loadProductionUnits();

        h.scope("architecture/conformidad-hexagonal");

        h.expect("domain core only depends on itself and the JDK", () ->
                allImportsWithin(units, "domain", allowedLayers("domain")));
        h.expect("application only depends on domain and itself", () ->
                allImportsWithin(units, "application", allowedLayers("application")));
        h.expect("infrastructure adapters do not couple to each other", () ->
                noCrossAdapterCoupling(units));
        h.expect("the whole project lives with zero external dependencies", () -> {
            for (SourceUnit unit : units) {
                for (String imported : unit.imports) {
                    boolean jdk = imported.startsWith("java.");
                    boolean own = imported.startsWith(ROOT_PACKAGE);
                    if (!jdk && !own) {
                        return false;
                    }
                }
            }
            return true;
        });
        h.expect("production never imports verification artifacts", () -> {
            for (SourceUnit unit : units) {
                for (String imported : unit.imports) {
                    if (imported.startsWith(ROOT_PACKAGE + "tests")) {
                        return false;
                    }
                }
            }
            return true;
        });
        h.expect("core remains free of decorative annotations", () -> {
            for (SourceUnit unit : units) {
                if (!unit.layer.equals("domain") && !unit.layer.equals("application")) {
                    continue;
                }
                for (String line : unit.lines) {
                    if (ANNOTATION_LINE.matcher(line).matches()) {
                        return false;
                    }
                }
            }
            return true;
        });
        h.expect("each class respects the cohesion review threshold", () -> {
            for (SourceUnit unit : units) {
                if (unit.effectiveLines() > COHESION_REVIEW_THRESHOLD_LINES) {
                    return false;
                }
            }
            return true;
        });
    }

    private static List<String> allowedLayers(String layer) {
        if ("domain".equals(layer)) {
            return single("domain");
        }
        if ("application".equals(layer)) {
            return java.util.Arrays.asList("domain", "application");
        }
        return java.util.Arrays.asList("domain", "application", layer);
    }

    private static List<String> single(String value) {
        List<String> only = new ArrayList<>();
        only.add(value);
        return only;
    }

    private static boolean allImportsWithin(List<SourceUnit> units,
                                            String layer, List<String> reachableLayers) {
        for (SourceUnit unit : units) {
            if (!unit.layer.equals(layer)) {
                continue;
            }
            for (String imported : unit.imports) {
                if (imported.startsWith("java.")) {
                    continue;
                }
                if (!imported.startsWith(ROOT_PACKAGE)) {
                    return false;
                }
                boolean permitted = false;
                for (String reachable : reachableLayers) {
                    if (imported.startsWith(ROOT_PACKAGE + reachable + ".")) {
                        permitted = true;
                    }
                }
                if (!permitted) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean noCrossAdapterCoupling(List<SourceUnit> units) {
        for (SourceUnit unit : units) {
            if (!unit.layer.equals("infrastructure")) {
                continue;
            }
            for (String imported : unit.imports) {
                if (!imported.startsWith(ROOT_PACKAGE + "infrastructure.")) {
                    continue;
                }
                String importedAdapter = adapterSegmentOf(imported);
                boolean selfOrShared = importedAdapter.equals(unit.adapter)
                        || SHARED_ADAPTERS.contains(importedAdapter);
                if (!unit.adapter.isEmpty() && !selfOrShared) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String adapterSegmentOf(String imported) {
        String remainder = imported.substring((ROOT_PACKAGE + "infrastructure.").length());
        int slash = remainder.indexOf('.');
        return slash < 0 ? remainder : remainder.substring(0, slash);
    }

    private static List<SourceUnit> loadProductionUnits() {
        Path sourceRoot = Paths.get(System.getProperty("user.dir"), "src");
        List<SourceUnit> units = new ArrayList<>();
        try (Stream<Path> walked = Files.walk(sourceRoot)) {
            walked.filter(path -> path.toString().endsWith(".java")).forEach(path ->
                    units.add(readUnit(sourceRoot, path)));
        } catch (IOException e) {
            throw new IllegalStateException("could not read production tree", e);
        }
        return units;
    }

    private static SourceUnit readUnit(Path sourceRoot, Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("lectura imposible: " + path, e);
        }
        String relativePath = sourceRoot.relativize(path).toString().replace('\\', '/');
        String afterRoot = relativePath.startsWith("src/")
                ? relativePath.substring("src/".length())
                : relativePath;
        String withoutFile = stripFileName(afterRoot);

        List<String> imports = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = IMPORT_LINE.matcher(line);
            if (matcher.matches()) {
                imports.add(matcher.group(1));
            }
        }
        return new SourceUnit(withoutFile, lines, imports);
    }

    private static String stripFileName(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash);
    }

    private static final class SourceUnit {

        final String layer;
        final String adapter;
        final List<String> lines;
        final List<String> imports;

        SourceUnit(String folder, List<String> lines, List<String> imports) {
            this.lines = lines;
            this.imports = imports;
            int firstSlash = folder.indexOf('/');
            this.layer = firstSlash < 0 ? folder : folder.substring(0, firstSlash);
            String remainder = firstSlash < 0 ? "" : folder.substring(firstSlash + 1);
            int secondSlash = remainder.indexOf('/');
            this.adapter = secondSlash < 0 ? remainder : remainder.substring(0, secondSlash);
        }

        int effectiveLines() {
            int counted = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()
                        || trimmed.startsWith("//")
                        || trimmed.startsWith("*")
                        || trimmed.startsWith("/*")) {
                    continue;
                }
                counted++;
            }
            return counted;
        }
    }
}

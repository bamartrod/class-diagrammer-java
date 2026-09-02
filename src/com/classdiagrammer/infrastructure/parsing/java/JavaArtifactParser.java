package com.classdiagrammer.infrastructure.parsing.java;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Visibility;

import com.classdiagrammer.infrastructure.parsing.JavaVersion;
import com.classdiagrammer.infrastructure.parsing.LanguageCapabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Artifact parser for JavaArtifactParser delegating to the core Java parser with version-specific capabilities.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class JavaArtifactParser implements ArtifactParser {

    private static final Pattern PACKAGE_DECLARATION =
            Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_DECLARATION =
            Pattern.compile("\\bimport\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    private final JavaVersion javaVersion;
    private final LanguageCapabilities capabilities;
    private final RegionScanner regionScanner = new RegionScanner();
    private final HeaderParser headerParser = new HeaderParser();
    private final MemberScanner memberScanner = new MemberScanner();

    public JavaArtifactParser() {
        this(JavaVersion.V8, LanguageCapabilities.forVersion(JavaVersion.V8));
    }

    public JavaArtifactParser(LanguageCapabilities capabilities) {
        this(deduceVersion(capabilities), capabilities == null
                ? LanguageCapabilities.forVersion(JavaVersion.V8)
                : capabilities);
    }

    public JavaArtifactParser(JavaVersion javaVersion, LanguageCapabilities capabilities) {
        this.javaVersion = javaVersion == null ? JavaVersion.V8 : javaVersion;
        this.capabilities = capabilities == null
                ? LanguageCapabilities.forVersion(this.javaVersion)
                : capabilities;
    }

    private static JavaVersion deduceVersion(LanguageCapabilities caps) {
        if (caps == null) return JavaVersion.V8;
        if (!caps.records() && !caps.textBlocks() && !caps.sealedTypes() && !caps.patternMatching() && !caps.switchExpression() && !caps.localVariableTypeInference() && !caps.virtualThread()) return JavaVersion.V8;
        if (!caps.textBlocks() && !caps.records() && !caps.sealedTypes() && !caps.patternMatching()) return JavaVersion.V11;
        return JavaVersion.V17;
    }

    public boolean accepts(SourceFile source) {
        return source != null && source.file().endsWith(".java");
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
        detectUnsupportedFeatures(source);
        SourceText text = SourceText.of(source.content());
        String masked = text.masked();
        String packageName = firstGroup(PACKAGE_DECLARATION, masked);
        List<String> imports = allGroups(IMPORT_DECLARATION, masked);
        String folder = parentFolder(source.file());

        List<TypeNode> nodes = new ArrayList<>();
        regionScanner.scan(masked, 0, masked.length(), "",
                (declaration, bodyStart, bodyEnd, qualifier) -> {
                    HeaderParser.ParsedHeader header = headerParser.parse(declaration.header());
                    SignatureInterpreter.ParsedMembers members = memberScanner.scan(declaration, masked, bodyStart + 1, bodyEnd, imports);
                    List<String> hierarchyImports = filterHierarchyImports(imports, header);
                    nodes.add(buildType(declaration, qualifier, packageName,
                            hierarchyImports, folder, source.file(),
                            members, header));
                });
        return nodes;
    }

    private TypeNode buildType(TypeDeclaration declaration, String qualifier, String packageName,
                               List<String> imports, String folder, String file,
                               SignatureInterpreter.ParsedMembers members,
                               HeaderParser.ParsedHeader header) {
        String prefix = packageName.isEmpty() ? "" : packageName + ".";
        String relative = qualifier.isEmpty() ? declaration.name() : qualifier + "." + declaration.name();
        return TypeNode.named(prefix + relative, declaration.name())
                .inPackage(packageName)
                .ofKind(mapKind(declaration.kindToken()))
                .withVisibility(Visibility.fromKeywords(declaration.modifiers()))
                .withModifiers(declaration.modifiers())
                .locatedAt(folder, file)
                .importing(imports)
                .extending(header.extendsTypes())
                .implementing(header.implementsTypes())
                .permitting(header.permitsTypes())
                .withMethods(members.methods())
                .withConstructors(members.constructors())
                .withFields(members.fields())
                .build();
    }

    static TypeKind mapKind(String kindToken) {
        if ("class".equals(kindToken) || "record".equals(kindToken)) {
            return TypeKind.CLASS;
        } else if ("interface".equals(kindToken)) {
            return TypeKind.INTERFACE;
        } else if ("enum".equals(kindToken)) {
            return TypeKind.ENUM;
        } else {
            return TypeKind.ANNOTATION;
        }
    }

    private List<String> filterHierarchyImports(List<String> imports, HeaderParser.ParsedHeader header) {
        if (imports == null || imports.isEmpty()) return List.of();
        java.util.Set<String> hierarchySimpleNames = new java.util.HashSet<>();
        for (String t : header.extendsTypes()) hierarchySimpleNames.add(simpleNameOf(t));
        for (String t : header.implementsTypes()) hierarchySimpleNames.add(simpleNameOf(t));
        for (String t : header.permitsTypes()) hierarchySimpleNames.add(simpleNameOf(t));
        if (hierarchySimpleNames.isEmpty()) return List.of();
        List<String> filtered = new ArrayList<>();
        for (String imp : imports) {
            if (imp == null || imp.trim().isEmpty()) continue;
            if (imp.endsWith(".*")) continue;
            if (imp.startsWith("java.") || imp.startsWith("javax.")) continue;
            int lastDot = imp.lastIndexOf('.');
            if (lastDot < 0) continue;
            String simple = imp.substring(lastDot + 1);
            if (hierarchySimpleNames.contains(simple)) {
                filtered.add(imp);
            }
        }
        java.util.Collections.sort(filtered);
        return List.copyOf(filtered);
    }

    private String simpleNameOf(String type) {
        if (type == null) return "";
        // remove generics and array markers, then take simple name after last dot
        String cleaned = type.replaceAll("<.*>", "").replaceAll("\\[\\]", "").trim();
        int lastDot = cleaned.lastIndexOf('.');
        if (lastDot >= 0) cleaned = cleaned.substring(lastDot + 1);
        // in case still contains spaces, take last token
        String[] parts = cleaned.split("\\s+");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    static String parentFolder(String filePath) {
        int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return slash < 0 ? "" : filePath.substring(0, slash);
    }

    private void detectUnsupportedFeatures(SourceFile source) {
        String content = source.content();
        String file = source.file();
        String masked = SourceText.of(content).masked();
        // text blocks """ – require textBlocks capability (Java 15+)
        if (!capabilities.textBlocks() && content.contains("\"\"\"")) {
            throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                    com.classdiagrammer.domain.evidence.LanguageFeature.TEXT_BLOCK, javaVersion.label(), file);
        }
        // records – require records capability
        if (!capabilities.records() && content.matches("(?s).*\\brecord\\b.*")) {
            if (content.matches("(?s).*\\brecord\\s+\\w+.*") && masked.matches("(?s).*\\brecord\\s+\\w+.*")) {
                throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                        com.classdiagrammer.domain.evidence.LanguageFeature.RECORD, javaVersion.label(), file);
            }
        }
        // sealed / permits – require sealedTypes
        if (!capabilities.sealedTypes()) {
            if (content.matches("(?s).*\\bsealed\\b.*") || content.matches("(?s).*\\bpermits\\b.*") || content.contains("non-sealed")) {
                if (masked.matches("(?s).*\\b(sealed|permits|non-sealed)\\b.*")) {
                    com.classdiagrammer.domain.evidence.LanguageFeature feat = masked.contains("permits") ? com.classdiagrammer.domain.evidence.LanguageFeature.SEALED_TYPE : com.classdiagrammer.domain.evidence.LanguageFeature.SEALED_TYPE;
                    throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                            feat, javaVersion.label(), file);
                }
            }
        }
        // pattern matching instanceof – require patternMatching (Java 16+)
        if (!capabilities.patternMatching() && masked.matches("(?s).*\\binstanceof\\s+\\w+\\s+\\w+.*")) {
            throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                    com.classdiagrammer.domain.evidence.LanguageFeature.PATTERN_MATCHING, javaVersion.label(), file);
        }
        // switch expression – require switchExpression (Java 14+)
        if (!capabilities.switchExpression() && (masked.contains("->") && masked.matches("(?s).*\\bswitch\\s*\\(.*\\).*") || masked.contains("yield "))) {
            // heuristic: switch with arrow or yield keyword
            if (masked.matches("(?s).*\\bswitch\\s*\\(.*\\)\\s*\\{.*->.*") || masked.matches("(?s).*\\byield\\b.*")) {
                throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                        com.classdiagrammer.domain.evidence.LanguageFeature.SWITCH_EXPRESSION, javaVersion.label(), file);
            }
        }
        // local variable type inference var – require localVariableTypeInference (Java 10+)
        if (!capabilities.localVariableTypeInference() && masked.matches("(?s).*\\bvar\\s+\\w+\\s*[=;].*")) {
            throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                    com.classdiagrammer.domain.evidence.LanguageFeature.LOCAL_VARIABLE_TYPE_INFERENCE, javaVersion.label(), file);
        }
        // virtual thread – require virtualThread (Java 21+)
        if (!capabilities.virtualThread() && (content.contains("newVirtualThreadPerTaskExecutor") || content.contains("VirtualThread") || content.contains("Executors.newVirtualThread"))) {
            throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                    com.classdiagrammer.domain.evidence.LanguageFeature.VIRTUAL_THREAD, javaVersion.label(), file);
        }
    }

    private String firstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<String> allGroups(Pattern pattern, String input) {
        List<String> found = new ArrayList<>();
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}

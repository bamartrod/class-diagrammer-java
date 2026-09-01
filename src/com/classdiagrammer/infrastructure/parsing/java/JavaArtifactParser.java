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
        if (!caps.records() && !caps.textBlocks() && !caps.sealedTypes()) return JavaVersion.V8;
        if (!caps.textBlocks() && !caps.records()) return JavaVersion.V11;
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
                (declaration, bodyStart, bodyEnd, qualifier) ->
                        nodes.add(buildType(declaration, qualifier, packageName,
                                imports, folder, source.file(),
                                memberScanner.scan(declaration, masked, bodyStart + 1, bodyEnd),
                                headerParser.parse(declaration.header()))));
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
        return switch (kindToken) {
            case "class", "record" -> TypeKind.CLASS;
            case "interface" -> TypeKind.INTERFACE;
            case "enum" -> TypeKind.ENUM;
            default -> TypeKind.ANNOTATION;
        };
    }

    static String parentFolder(String filePath) {
        int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return slash < 0 ? "" : filePath.substring(0, slash);
    }

    private void detectUnsupportedFeatures(SourceFile source) {
        String content = source.content();
        String file = source.file();
        // text blocks """ – require textBlocks capability (Java 15+)
        if (!capabilities.textBlocks() && content.contains("\"\"\"")) {
            throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                    com.classdiagrammer.domain.evidence.LanguageFeature.TEXT_BLOCK, javaVersion.label(), file);
        }
        // records – require records capability
        if (!capabilities.records() && content.matches("(?s).*\\brecord\\b.*")) {
            // more precise: record keyword followed by identifier
            if (content.matches("(?s).*\\brecord\\s+\\w+.*")) {
                throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                        com.classdiagrammer.domain.evidence.LanguageFeature.RECORD, javaVersion.label(), file);
            }
        }
        // sealed / permits – require sealedTypes
        if (!capabilities.sealedTypes()) {
            if (content.matches("(?s).*\\bsealed\\b.*") || content.matches("(?s).*\\bpermits\\b.*") || content.contains("non-sealed")) {
                // check if actually a type declaration uses sealed/permit
                String masked = SourceText.of(content).masked();
                if (masked.matches("(?s).*\\b(sealed|permits|non-sealed)\\b.*")) {
                    com.classdiagrammer.domain.evidence.LanguageFeature feat = content.contains("permits") || content.contains("permits") ? com.classdiagrammer.domain.evidence.LanguageFeature.SEALED_TYPE : com.classdiagrammer.domain.evidence.LanguageFeature.SEALED_TYPE;
                    throw new com.classdiagrammer.domain.evidence.UnsupportedLanguageFeatureException(
                            feat, javaVersion.label(), file);
                }
            }
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

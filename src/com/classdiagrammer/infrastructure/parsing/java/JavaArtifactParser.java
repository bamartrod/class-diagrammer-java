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

    private final LanguageCapabilities capabilities;
    private final RegionScanner regionScanner = new RegionScanner();
    private final HeaderParser headerParser = new HeaderParser();
    private final MemberScanner memberScanner = new MemberScanner();

    public JavaArtifactParser() {
        this(LanguageCapabilities.forVersion(JavaVersion.V8));
    }

    public JavaArtifactParser(LanguageCapabilities capabilities) {
        this.capabilities = capabilities == null
                ? LanguageCapabilities.forVersion(JavaVersion.V8)
                : capabilities;
    }

    public boolean accepts(SourceFile source) {
        return source != null && source.file().endsWith(".java");
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
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

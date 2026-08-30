package com.classdiagrammer.infrastructure.parsing.xforms;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.infrastructure.xml.XmlTagScanner;
import com.classdiagrammer.domain.model.Field;
import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.Parameter;
import com.classdiagrammer.domain.model.SourceFile;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.TypeNode;
import com.classdiagrammer.domain.model.Visibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser for XForms documents.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class XFormsArtifactParser implements ArtifactParser {

    private static final String KIND_MARKER = "xforms";

    private SourceFile lastSniffed;
    private boolean lastSniffAccepted;

    public boolean accepts(SourceFile source) {
        if (source == null || !hasDocumentExtension(source.file())) {
            return false;
        }
        if (source == lastSniffed) {
            return lastSniffAccepted;
        }
        FormModelCollector sniff = new FormModelCollector();
        XmlTagScanner.scan(source.content(), sniff::accept);
        lastSniffed = source;
        lastSniffAccepted = sniff.declaresXFormsNamespace();
        return lastSniffAccepted;
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
        FormModelCollector collector = new FormModelCollector();
        XmlTagScanner.scan(source.content(), collector::accept);
        if (!collector.isXFormsDocument()) {
            return Collections.emptyList();
        }
        String folder = parentFolder(source.file());
        return Collections.singletonList(TypeNode
                .named(source.file(), baseName(source.file()))
                .inPackage(folder)
                .ofKind(TypeKind.FORM)
                .withVisibility(Visibility.PUBLIC)
                .withModifiers(Collections.<String>emptySet())
                .locatedAt(folder, source.file())
                .importing(documentReferences(collector))
                .withMethods(structuralMethods(collector))
                .withFields(bindsAsFields(collector))
                .build());
    }

    private List<Method> structuralMethods(FormModelCollector collector) {
        List<Method> methods = new ArrayList<>();
        for (String model : collector.models()) {
            methods.add(modelMethod("model", model));
        }
        for (String submission : collector.submissions()) {
            methods.add(modelMethod("submission", submission));
        }
        return methods;
    }

    private Method modelMethod(String role, String identifier) {
        String name = identifier.isEmpty() ? role : role + ":" + identifier;
        Set<String> modifiers = new HashSet<>();
        modifiers.add(KIND_MARKER);
        return Method.returning(name, KIND_MARKER, Visibility.PUBLIC, modifiers,
                Collections.singletonList(new Parameter(KIND_MARKER, "form")));
    }

    private List<Field> bindsAsFields(FormModelCollector collector) {
        List<Field> fields = new ArrayList<>();
        for (String nodeset : collector.binds()) {
            fields.add(Field.named(nodeset, KIND_MARKER, Visibility.PUBLIC,
                    new HashSet<String>()));
        }
        return fields;
    }

    private List<String> documentReferences(FormModelCollector collector) {
        List<String> references = new ArrayList<>();
        for (String src : collector.instanceSources()) {
            if (isDocumentReference(src)) {
                references.add(src);
            }
        }
        for (String action : collector.submissions()) {
            if (isDocumentReference(action)) {
                references.add(action);
            }
        }
        return references;
    }

    private boolean isDocumentReference(String target) {
        return target.endsWith(".xhtml") || target.endsWith(".xforms")
                || target.endsWith(".vm") || target.endsWith(".vtl");
    }

    private boolean hasDocumentExtension(String filePath) {
        return filePath.endsWith(".xhtml") || filePath.endsWith(".xforms")
                || filePath.endsWith(".xml");
    }

    private String baseName(String filePath) {
        String posix = filePath.replace('\\', '/');
        int slash = posix.lastIndexOf('/');
        String fileName = slash < 0 ? posix : posix.substring(slash + 1);
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private String parentFolder(String filePath) {
        int slash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return slash < 0 ? "" : filePath.substring(0, slash);
    }
}

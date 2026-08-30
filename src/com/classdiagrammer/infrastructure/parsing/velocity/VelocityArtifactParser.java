package com.classdiagrammer.infrastructure.parsing.velocity;

import com.classdiagrammer.application.port.out.ArtifactParser;
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
import java.util.List;
import java.util.Set;

/**
 * Parser for Apache Velocity templates (.vm/.vtl).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class VelocityArtifactParser implements ArtifactParser {

    private static final String KIND_MARKER = "velocity";

    public boolean accepts(SourceFile source) {
        if (source == null) {
            return false;
        }
        return source.file().endsWith(".vm") || source.file().endsWith(".vtl");
    }

    public List<TypeNode> parse(SourceFile source) {
        if (source == null) {
            throw new IllegalArgumentException("source file is required");
        }
        TemplateDirectives.ScanResult scan = TemplateDirectives.scan(source.content());
        String folder = parentFolder(source.file());
        return Collections.singletonList(TypeNode
                .named(source.file(), baseName(source.file()))
                .inPackage(folder)
                .ofKind(TypeKind.TEMPLATE)
                .withVisibility(Visibility.PUBLIC)
                .withModifiers(Collections.<String>emptySet())
                .locatedAt(folder, source.file())
                .importing(scan.references)
                .withMethods(methodsFrom(scan.macros))
                .withFields(fieldsFrom(scan.globalVariables))
                .build());
    }

    private List<Method> methodsFrom(List<TemplateDirectives.MacroSig> macros) {
        List<Method> methods = new ArrayList<>();
        for (TemplateDirectives.MacroSig macro : macros) {
            methods.add(Method.returning(macro.name, "void", Visibility.PUBLIC,
                    new HashSet<String>(), parametersOf(macro.parameters)));
        }
        return methods;
    }

    private List<Parameter> parametersOf(List<String> names) {
        List<Parameter> parameters = new ArrayList<>();
        for (String name : names) {
            parameters.add(new Parameter(KIND_MARKER, name));
        }
        return parameters;
    }

    private List<Field> fieldsFrom(List<String> variables) {
        List<Field> fields = new ArrayList<>();
        for (String variable : variables) {
            Set<String> modifiers = new HashSet<>();
            modifiers.add("global-set");
            fields.add(Field.named(variable, KIND_MARKER, Visibility.PUBLIC, modifiers));
        }
        return fields;
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

package com.classdiagrammer.infrastructure.parsing.java;

import com.classdiagrammer.domain.model.Field;
import com.classdiagrammer.domain.model.Method;
import com.classdiagrammer.domain.model.Parameter;
import com.classdiagrammer.domain.model.TypeKind;
import com.classdiagrammer.domain.model.Visibility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper SignatureInterpreter supporting the Java parser pipeline.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
final class SignatureInterpreter {

    private final String simpleName;
    private final TypeKind kind;
    private boolean awaitingEnumConstants;
    private Visibility currentVisibility = Visibility.PACKAGE_PRIVATE;
    private Set<String> currentModifiers = new LinkedHashSet<>();

    private final List<Method> methods = new ArrayList<>();
    private final List<Method> constructors = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();

    SignatureInterpreter(String simpleName, TypeKind kind, boolean isEnum) {
        this.simpleName = simpleName;
        this.kind = kind;
        this.awaitingEnumConstants = isEnum;
    }

    void accept(String chunk) {
        resetForNextChunk();
        String cleaned = TokenOps.stripLeadingAnnotations(chunk);
        if (cleaned.isEmpty()) {
            return;
        }
        int equalsIndex = TokenOps.topLevelEquals(cleaned);
        String signatureSide = equalsIndex >= 0 ? cleaned.substring(0, equalsIndex).trim() : cleaned;
        if (signatureSide.isEmpty()) {
            return;
        }
        int parenIndex = signatureSide.indexOf('(');
        if (parenIndex < 0) {
            interpretWithoutParentheses(signatureSide);
        } else {
            interpretWithParentheses(signatureSide, parenIndex);
        }
    }

    SignatureInterpreter.ParsedMembers toMembers() {
        return new ParsedMembers(methods, constructors, fields);
    }

    private void interpretWithoutParentheses(String side) {
        List<String> tokens = stripModifiers(TokenOps.splitWords(side));
        if (awaitingEnumConstants && tokens.size() == 1 && TokenOps.isIdentifier(tokens.get(0))) {
            return;
        }
        if (tokens.size() == 1 && TokenOps.isModifier(tokens.get(0))) {
            return;
        }
        if (tokens.size() < 2) {
            return;
        }
        String name = tokens.get(tokens.size() - 1);
        if (!TokenOps.isIdentifier(name)) {
            return;
        }
        String type = join(tokens, tokens.size() - 1);
        fields.add(Field.named(name, type, effectiveVisibility(), currentModifiers));
        awaitingEnumConstants = false;
    }

    private void interpretWithParentheses(String side, int parenIndex) {
        int closeParen = TokenOps.matchingParen(side, parenIndex);
        if (closeParen < 0) {
            return;
        }
        String head = side.substring(0, parenIndex).trim();
        String paramsBody = side.substring(parenIndex + 1, closeParen);
        List<Parameter> parameters = parseParameters(paramsBody);

        List<String> tokens = stripModifiersAndTypeParameters(TokenOps.splitWords(head));
        if (tokens.isEmpty()) {
            return;
        }
        if (awaitingEnumConstants && tokens.size() == 1 && TokenOps.isIdentifier(tokens.get(0))) {
            return;
        }

        if (tokens.size() == 1) {
            String candidate = tokens.get(0);
            if (candidate.equals(simpleName)) {
                constructors.add(Method.constructor(candidate,
                        effectiveVisibility(), currentModifiers, parameters));
                awaitingEnumConstants = false;
                return;
            }
            if (!awaitingEnumConstants || !TokenOps.isIdentifier(candidate)) {
                // ni constructor ni constante de enum: se descarta
            }
            return;
        }

        String name = tokens.get(tokens.size() - 1);
        if (!TokenOps.isIdentifier(name)) {
            return;
        }
        String returnType = join(tokens, tokens.size() - 1);
        methods.add(Method.returning(name, returnType,
                effectiveVisibility(), currentModifiers, parameters));
        awaitingEnumConstants = false;
    }

    private List<Parameter> parseParameters(String body) {
        List<Parameter> parameters = new ArrayList<>();
        for (String raw : TokenOps.splitTopLevel(body, ',')) {
            List<String> tokens = stripModifiers(TokenOps.splitWords(raw));
            if (tokens.isEmpty()) {
                continue;
            }
            if (tokens.size() == 1) {
                parameters.add(new Parameter(tokens.get(0), ""));
                continue;
            }
            String name = tokens.get(tokens.size() - 1);
            String type = join(tokens, tokens.size() - 1);
            parameters.add(new Parameter(type, name));
        }
        return parameters;
    }

    private String join(List<String> tokens, int upToExclusive) {
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < upToExclusive; i++) {
            if (joined.length() > 0) {
                joined.append(' ');
            }
            joined.append(tokens.get(i));
        }
        return joined.toString();
    }

    private List<String> stripModifiers(List<String> tokens) {
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (kept.isEmpty() && TokenOps.isModifier(token)) {
                captureModifier(token);
            } else {
                kept.add(token);
            }
        }
        return kept;
    }

    private List<String> stripModifiersAndTypeParameters(List<String> tokens) {
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (kept.isEmpty() && TokenOps.isModifier(token)) {
                captureModifier(token);
            } else if (kept.isEmpty() && token.startsWith("<")) {
                // generic type-parameter section of a generic method
            } else {
                kept.add(token);
            }
        }
        return kept;
    }

    private void captureModifier(String token) {
        if (token.equals("public")) {
            currentVisibility = Visibility.PUBLIC;
        } else if (token.equals("private")) {
            currentVisibility = Visibility.PRIVATE;
        } else if (token.equals("protected")) {
            currentVisibility = Visibility.PROTECTED;
        }
        currentModifiers.add(token);
    }

    private Visibility effectiveVisibility() {
        if (kind == TypeKind.INTERFACE && currentVisibility == Visibility.PACKAGE_PRIVATE) {
            return Visibility.PUBLIC;
        }
        return currentVisibility;
    }

    private void resetForNextChunk() {
        currentVisibility = Visibility.PACKAGE_PRIVATE;
        currentModifiers = new LinkedHashSet<>();
    }

    static final class ParsedMembers {

        private final List<Method> methods;
        private final List<Method> constructors;
        private final List<Field> fields;

        private ParsedMembers(List<Method> methods, List<Method> constructors, List<Field> fields) {
            this.methods = new ArrayList<>(methods);
            this.constructors = new ArrayList<>(constructors);
            this.fields = new ArrayList<>(fields);
        }

        List<Method> methods() {
            return methods;
        }

        List<Method> constructors() {
            return constructors;
        }

        List<Field> fields() {
            return fields;
        }
    }
}

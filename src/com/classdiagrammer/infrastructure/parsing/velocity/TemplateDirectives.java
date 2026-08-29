package com.classdiagrammer.infrastructure.parsing.velocity;

import java.util.ArrayList;
import java.util.List;

final class TemplateDirectives {

    static final class MacroSig {
        final String name;
        final List<String> parameters;

        MacroSig(String name, List<String> parameters) {
            this.name = name;
            this.parameters = parameters;
        }
    }

    static final class ScanResult {
        final List<MacroSig> macros = new ArrayList<>();
        final List<String> globalVariables = new ArrayList<>();
        final List<String> references = new ArrayList<>();
    }

    private TemplateDirectives() {
    }

    static ScanResult scan(String content) {
        ScanResult result = new ScanResult();
        String cleaned = stripBlockComments(content);
        List<String> scope = new ArrayList<>();
        int index = 0;
        while (index < cleaned.length()) {
            index = advance(cleaned, index);
            if (index < 0) {
                int hashIndex = -index - 1;
                DirectiveReader.Directive directive = DirectiveReader.read(cleaned, hashIndex);
                apply(directive, scope, result);
                index = directive.end();
            }
        }
        return result;
    }

    private static int advance(String text, int from) {
        int index = from;
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '#' && index + 1 < text.length()
                    && text.charAt(index + 1) == '#') {
                while (index < text.length() && text.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                index = DirectiveReader.skipQuoted(text, index);
                continue;
            }
            if (current == '#' && startsDirective(text, index)) {
                return -index - 1;
            }
            index++;
        }
        return index;
    }

    private static void apply(DirectiveReader.Directive directive,
                              List<String> scope, ScanResult result) {
        String keyword = directive.keyword();
        if ("macro".equals(keyword)) {
            scope.add("macro");
            MacroSig macro = parseMacro(directive.body());
            if (macro != null && !containsMacro(result.macros, macro.name)) {
                result.macros.add(macro);
            }
            return;
        }
        if ("if".equals(keyword) || "foreach".equals(keyword) || "define".equals(keyword)) {
            scope.add(keyword);
            return;
        }
        if ("end".equals(keyword) && !scope.isEmpty()) {
            scope.remove(scope.size() - 1);
            return;
        }
        if ("set".equals(keyword) && scope.isEmpty()) {
            String variable = firstVariable(directive.body());
            if (!variable.isEmpty() && !result.globalVariables.contains(variable)) {
                result.globalVariables.add(variable);
            }
            return;
        }
        if ("parse".equals(keyword) || "include".equals(keyword)) {
            String target = firstQuoted(directive.body());
            if (!target.isEmpty() && !result.references.contains(target)) {
                result.references.add(target);
            }
        }
    }

    private static MacroSig parseMacro(String body) {
        List<String> tokens = splitWhitespace(body.trim());
        if (tokens.isEmpty()) {
            return null;
        }
        List<String> parameters = new ArrayList<>();
        for (int i = 1; i < tokens.size(); i++) {
            parameters.add(normalizeVariable(tokens.get(i)));
        }
        return new MacroSig(normalizeVariable(tokens.get(0)), parameters);
    }

    private static boolean containsMacro(List<MacroSig> macros, String name) {
        for (MacroSig macro : macros) {
            if (macro.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String stripBlockComments(String content) {
        StringBuilder out = new StringBuilder(content.length());
        int index = 0;
        while (index < content.length()) {
            if (index + 1 < content.length() && content.charAt(index) == '#'
                    && content.charAt(index + 1) == '*') {
                int close = content.indexOf("*#", index + 2);
                int limit = close < 0 ? content.length() : close + 2;
                while (index < limit) {
                    out.append(content.charAt(index) == '\n' ? '\n' : ' ');
                    index++;
                }
                continue;
            }
            out.append(content.charAt(index));
            index++;
        }
        return out.toString();
    }

    private static boolean startsDirective(String text, int hashIndex) {
        if (hashIndex > 0) {
            char before = text.charAt(hashIndex - 1);
            if (Character.isLetterOrDigit(before) || before == '$' || before == '_'
                    || before == '#') {
                return false;
            }
        }
        return hashIndex + 1 < text.length() && Character.isLetter(text.charAt(hashIndex + 1));
    }

    private static List<String> splitWhitespace(String input) {
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < input.length()) {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            int start = index;
            while (index < input.length() && !Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            if (index > start) {
                tokens.add(input.substring(start, index));
            }
        }
        return tokens;
    }

    private static String normalizeVariable(String token) {
        String cleaned = token.replace("{", "").replace("}", "").replace("$", "");
        return cleaned.isEmpty() ? token : cleaned;
    }

    private static String firstVariable(String body) {
        int dollar = body.indexOf('$');
        if (dollar < 0) {
            return "";
        }
        int index = dollar + 1;
        if (index < body.length() && body.charAt(index) == '{') {
            int close = body.indexOf('}', index);
            return close < 0 ? "" : body.substring(index + 1, close);
        }
        int end = index;
        while (end < body.length() && (Character.isLetterOrDigit(body.charAt(end))
                || body.charAt(end) == '_')) {
            end++;
        }
        return body.substring(index, end);
    }

    private static String firstQuoted(String body) {
        for (int index = 0; index < body.length(); index++) {
            char current = body.charAt(index);
            if (current == '"' || current == '\'') {
                int close = index + 1;
                while (close < body.length() && body.charAt(close) != current) {
                    close += body.charAt(close) == '\\' ? 2 : 1;
                }
                if (close < body.length()) {
                    return body.substring(index + 1, close)
                            .replace("\\" + current, String.valueOf(current));
                }
            }
        }
        return "";
    }
}

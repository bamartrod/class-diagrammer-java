package com.classdiagrammer.infrastructure.parsing.java;

final class MemberScanner {

    SignatureInterpreter.ParsedMembers scan(TypeDeclaration declaration,
                                            String masked, int start, int end) {
        SignatureInterpreter interpreter = new SignatureInterpreter(
                declaration.name(),
                JavaArtifactParser.mapKind(declaration.kindToken()),
                "enum".equals(declaration.kindToken()));

        int depth = 0;
        int chunkStart = -1;
        for (int i = start; i < end; i++) {
            char c = masked.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    int close = TokenOps.matchingBrace(masked, i, end);
                    if (chunkStart >= 0) {
                        interpreter.accept(masked.substring(chunkStart, i));
                        chunkStart = -1;
                    }
                    i = close < 0 ? end - 1 : close;
                } else {
                    depth++;
                }
            } else if (c == '}') {
                if (depth > 0) {
                    depth--;
                }
            } else if (c == ';' && depth == 0) {
                if (chunkStart >= 0) {
                    interpreter.accept(masked.substring(chunkStart, i));
                    chunkStart = -1;
                }
            } else if (depth == 0 && chunkStart < 0 && !Character.isWhitespace(c)) {
                chunkStart = i;
            }
        }
        return interpreter.toMembers();
    }
}

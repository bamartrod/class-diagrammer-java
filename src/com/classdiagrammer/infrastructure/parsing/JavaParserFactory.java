package com.classdiagrammer.infrastructure.parsing;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java11.Java11ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java17.Java17ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java21.Java21ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java26.Java26ArtifactParser;

public final class JavaParserFactory {

    private JavaParserFactory() {
    }

    public static ArtifactParser forVersion(JavaVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("version is required");
        }
        if (version == JavaVersion.V11) {
            return new Java11ArtifactParser();
        }
        if (version == JavaVersion.V17) {
            return new Java17ArtifactParser();
        }
        if (version == JavaVersion.V21) {
            return new Java21ArtifactParser();
        }
        if (version == JavaVersion.V26) {
            return new Java26ArtifactParser();
        }
        return new JavaArtifactParser();
    }
}

package com.classdiagrammer.infrastructure.parsing;

import com.classdiagrammer.application.port.out.ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java.JavaArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java11.Java11ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java17.Java17ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java21.Java21ArtifactParser;
import com.classdiagrammer.infrastructure.parsing.java25.Java25ArtifactParser;

/**
 * Factory creating the correct Java parser per version.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
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
        if (version == JavaVersion.V25) {
            return new Java25ArtifactParser();
        }
        return new JavaArtifactParser();
    }
}

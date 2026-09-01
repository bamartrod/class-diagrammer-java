package com.classdiagrammer.infrastructure.output;

import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.infrastructure.json.JsonDiagramOutput;
import com.classdiagrammer.infrastructure.output.ToonDiagramOutput;
import com.classdiagrammer.infrastructure.output.XmlDiagramOutput;
import com.classdiagrammer.infrastructure.output.YamlDiagramOutput;

/**
 * Factory selecting the correct {@link DiagramOutput} by format or file extension.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class DiagramOutputFactory {

    private DiagramOutputFactory() {}

    public static DiagramOutput forFormat(OutputFormat format) {
        if (format == null) format = OutputFormat.JSON;
        return switch (format) {
            case JSON -> new JsonDiagramOutput();
            case XML -> new XmlDiagramOutput();
            case YAML -> new YamlDiagramOutput();
            case TOON -> new ToonDiagramOutput();
        };
    }

    public static DiagramOutput forPath(String outputPath, OutputFormat explicit) {
        if (explicit != null) return forFormat(explicit);
        OutputFormat inferred = OutputFormat.fromPath(outputPath);
        return forFormat(inferred);
    }
}

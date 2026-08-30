package com.classdiagrammer.tests.support;

import com.classdiagrammer.application.port.out.DiagramOutput;
import com.classdiagrammer.application.port.out.DiagramReport;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test support utility RecordingDiagramSink.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class RecordingDiagramSink implements DiagramOutput {

    public DiagramReport lastReport;
    public String lastTargetPath;
    public int writings = 0;

    public Path write(DiagramReport report, String targetPath) {
        lastReport = report;
        lastTargetPath = targetPath;
        writings++;
        return Paths.get(targetPath);
    }
}

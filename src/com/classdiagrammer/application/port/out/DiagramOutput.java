package com.classdiagrammer.application.port.out;

import java.nio.file.Path;

/**
 * Output port DiagramOutput abstracting an infrastructure concern.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface DiagramOutput {

    Path write(DiagramReport report, String targetPath);
}

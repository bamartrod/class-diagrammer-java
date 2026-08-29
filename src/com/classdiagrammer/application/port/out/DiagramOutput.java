package com.classdiagrammer.application.port.out;

import java.nio.file.Path;

public interface DiagramOutput {

    Path write(DiagramReport report, String targetPath);
}

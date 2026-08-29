package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.SourceFile;

import java.util.List;

public interface SourceCodeReader {

    List<SourceFile> readAll(String sourceRoot);
}

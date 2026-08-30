package com.classdiagrammer.application.port.out;

import com.classdiagrammer.domain.model.SourceFile;

import java.util.List;

/**
 * Output port SourceCodeReader abstracting an infrastructure concern.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface SourceCodeReader {

    List<SourceFile> readAll(String sourceRoot);
}

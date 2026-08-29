package com.classdiagrammer.tests.support;

import com.classdiagrammer.application.port.out.SourceCodeReader;
import com.classdiagrammer.domain.model.SourceFile;

import java.util.ArrayList;
import java.util.List;

public final class SourceRecords implements SourceCodeReader {

    private final List<SourceFile> records = new ArrayList<>();
    public int consultations = 0;
    public String lastRootAsked = null;

    public SourceRecords(SourceFile... files) {
        for (SourceFile file : files) {
            records.add(file);
        }
    }

    public List<SourceFile> readAll(String sourceRoot) {
        consultations++;
        lastRootAsked = sourceRoot;
        return new ArrayList<>(records);
    }
}

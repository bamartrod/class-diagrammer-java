package com.classdiagrammer.application.port.in;

public interface GenerateClassDiagram {

    GenerateClassDiagramResult generate(GenerateClassDiagramCommand command);
}

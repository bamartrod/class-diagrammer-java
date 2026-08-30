package com.classdiagrammer.application.port.in;

/**
 * Input port GenerateClassDiagram defining the use-case contract.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public interface GenerateClassDiagram {

    GenerateClassDiagramResult generate(GenerateClassDiagramCommand command);
}

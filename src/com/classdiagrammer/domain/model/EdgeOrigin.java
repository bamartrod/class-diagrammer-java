package com.classdiagrammer.domain.model;

/**
 * Enumeration of edgeorigin values in the domain model.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum EdgeOrigin {
    PROJECT,
    EXTERNAL,
    UNKNOWN;

    public String jsonName() {
        return name().toLowerCase();
    }
}

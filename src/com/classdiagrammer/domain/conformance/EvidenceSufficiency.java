package com.classdiagrammer.domain.conformance;

/**
 * Evidence sufficiency per CSAS-002-U30/U31 (S1/S2/S3).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public enum EvidenceSufficiency {
    SUFFICIENT,
    INSUFFICIENT,
    CONTRADICTORY,
    UNSUPPORTED;

    public String jsonName() { return name().toLowerCase(); }
}

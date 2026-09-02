package com.classdiagrammer.domain.evidence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stable identifier derivation via SHA-256 hex, first 8 chars.
 * Deterministic across JVMs, vs hashCode (RULE-002-U5, §20).
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class StableId {

    private StableId() {}

    public static String of(String canonical) {
        String input = canonical == null ? "" : canonical;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String evidenceId(String ruleId, String subject, String locator) {
        return "EVID-" + of(ruleId + "|" + subject + "|" + locator);
    }

    public static String factId(String kind, String subject, String locator) {
        return "FACT-" + of(kind + "|" + subject + "|" + locator);
    }

    public static String originId(String from, String to) {
        return "ORIGIN-" + of(from + "|" + to);
    }
}

package com.classdiagrammer.tests.support;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Test support utility TestHarness.
 *
 * @author Brandon Martinez - https://github.com/bamartrod
 */
public final class TestHarness {

    public interface Behavior {
        boolean holds();
    }

    public static final class Outcome {

        private final String scope;
        private final String behavior;
        private final boolean holds;
        private final String obstruction;

        Outcome(String scope, String behavior, boolean holds, String obstruction) {
            this.scope = scope;
            this.behavior = behavior;
            this.holds = holds;
            this.obstruction = obstruction;
        }

        public boolean holds() {
            return holds;
        }

        public String obstruction() {
            return obstruction;
        }
    }

    private final List<Outcome> outcomes = new ArrayList<>();
    private String currentScope = "no-scope";

    public void scope(String name) {
        this.currentScope = name;
    }

    public void expect(String behavior, Behavior verification) {
        boolean holds;
        String obstruction = null;
        try {
            holds = verification.holds();
        } catch (Throwable trouble) {
            holds = false;
            obstruction = trouble.getClass().getSimpleName()
                    + (trouble.getMessage() == null ? "" : ": " + trouble.getMessage());
        }
        outcomes.add(new Outcome(currentScope, behavior, holds, obstruction));
    }

    public void report(PrintStream destination) {
        String printedScope = "";
        for (Outcome outcome : outcomes) {
            if (!printedScope.equals(outcome.scope)) {
                printedScope = outcome.scope;
                destination.println();
                destination.println("[" + printedScope + "]");
            }
            String verdict = outcome.holds ? "OK" : "FAIL";
            destination.println("  - " + outcome.behavior + " => " + verdict);
            if (!outcome.holds && outcome.obstruction != null) {
                destination.println("      (" + outcome.obstruction + ")");
            }
        }
    }

    public int total() {
        return outcomes.size();
    }

    public int failedCount() {
        int failures = 0;
        for (Outcome outcome : outcomes) {
            if (!outcome.holds) {
                failures++;
            }
        }
        return failures;
    }
}

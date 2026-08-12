package com.periut.retrocommands.test;

/**
 * Entry point for the offline test suite: {@code java -cp <classes> com.periut.retrocommands.test.TestRunner}.
 *
 * <p>Every suite registered here must run without a Minecraft classpath.
 */
public final class TestRunner {
    public static void main(final String[] args) {
        BrigadierTest.run();
        TextTest.run();
        CommandTest.run();
        System.exit(Tests.report());
    }
}

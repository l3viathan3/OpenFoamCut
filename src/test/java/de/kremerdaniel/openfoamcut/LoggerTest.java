package de.kremerdaniel.openfoamcut;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {

    private static final PrintStream ORIGINAL_OUT = System.out;

    @AfterEach
    void restoreSystemOut() {
        System.setOut(ORIGINAL_OUT);
    }

    @Test
    void infoUsesConfiguredConsolePatternAndPlaceholderFormatting() {
        ByteArrayOutputStream output = captureSystemOut();
        Logger logger = new Logger(LoggerTest.class);

        logger.info("Loading outline for {} from {}", "LEFT", "profile.dxf");

        String logLine = output.toString(StandardCharsets.UTF_8);
        assertTrue(logLine.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[main] INFO  LoggerTest - Loading outline for LEFT from profile\\.dxf\\R"), logLine);
    }

    @Test
    void debugIsSuppressedByInfoRootLevel() {
        ByteArrayOutputStream output = captureSystemOut();
        Logger logger = new Logger(LoggerTest.class);

        logger.debug("hidden");

        assertTrue(output.toString(StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    void warnPrintsThrowableWhenPassedAsTrailingArgument() {
        ByteArrayOutputStream output = captureSystemOut();
        Logger logger = new Logger(LoggerTest.class);
        IllegalStateException failure = new IllegalStateException("boom");

        logger.warn("Failed to load machine configuration", failure);

        String logOutput = output.toString(StandardCharsets.UTF_8);
        assertTrue(logOutput.contains("WARN  LoggerTest - Failed to load machine configuration"), logOutput);
        assertTrue(logOutput.contains("java.lang.IllegalStateException: boom"), logOutput);
    }

    @Test
    void archUnitProcessorLoggerOnlyEmitsErrors() {
        ByteArrayOutputStream output = captureSystemOut();
        Logger logger = new Logger("com.tngtech.archunit.core.importer.ClassFileProcessor");

        logger.warn("suppressed");
        logger.error("visible", new IllegalArgumentException("fail"));

        String logOutput = output.toString(StandardCharsets.UTF_8);
        assertFalse(logOutput.contains("suppressed"), logOutput);
        assertTrue(logOutput.contains("ERROR ClassFileProcessor - visible"), logOutput);
    }

    private ByteArrayOutputStream captureSystemOut() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        return output;
    }
}
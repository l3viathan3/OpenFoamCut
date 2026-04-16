package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.gui.MainPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for complete GUI initialization to ensure no NPEs occur during startup.
 */
class GuiInitializationTest {

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testCompleteGuiInitialization() {
        assertDoesNotThrow(() -> new MainPanel());
    }
}
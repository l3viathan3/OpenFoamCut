package de.kremerdaniel.openfoamcut.gui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GUI tests for the 3D preview tab wiring.
 */
class Preview3DTabTest {

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void mainPanelContainsPreviewTab() {
        MainPanel mainPanel = new MainPanel();
        JTabbedPane tabbedPane = findTabbedPane(mainPanel.getPanel());

        assertNotNull(tabbedPane);
        assertTrue(hasTab(tabbedPane, "5. 3D Preview"));
        assertNotNull(findSpinner(mainPanel.getPanel()));
    }

    private JTabbedPane findTabbedPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTabbedPane tabbedPane) {
                return tabbedPane;
            }
            if (component instanceof Container child) {
                JTabbedPane nested = findTabbedPane(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private boolean hasTab(JTabbedPane tabbedPane, String title) {
        for (int index = 0; index < tabbedPane.getTabCount(); index++) {
            if (title.equals(tabbedPane.getTitleAt(index))) {
                return true;
            }
        }
        return false;
    }

    private JSpinner findSpinner(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JSpinner spinner) {
                return spinner;
            }
            if (component instanceof Container child) {
                JSpinner nested = findSpinner(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.gui.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to verify that all UI components (buttons, text fields, text areas, and lists)
 * have tooltips set in all GUI panels.
 */
class GuiTooltipTest {

    @BeforeAll
    static void setHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testArrangePanelComponentsHaveTooltips() {
        ArrangePanel panel = new ArrangePanel();
        assertAllComponentsHaveTooltips(panel, "ArrangePanel");
    }

    @Test
    void testCutOrderPanelComponentsHaveTooltips() {
        CutOrderPanel panel = new CutOrderPanel();
        assertAllComponentsHaveTooltips(panel, "CutOrderPanel");
    }

    @Test
    void testAdditionalLinesPanelComponentsHaveTooltips() {
        AdditionalLinesPanel panel = new AdditionalLinesPanel();
        assertAllComponentsHaveTooltips(panel, "AdditionalLinesPanel");
    }

    @Test
    void testGenerateGCodePanelComponentsHaveTooltips() {
        GenerateGCodePanel panel = new GenerateGCodePanel();
        assertAllComponentsHaveTooltips(panel, "GenerateGCodePanel");
    }

    @Test
    void testMachinePanelComponentsHaveTooltips() {
        MachinePanel panel = new MachinePanel();
        assertAllComponentsHaveTooltips(panel, "MachinePanel");
    }

    @Test
    void testMoveOutlinePanelComponentsHaveTooltips() {
        MoveOutlinePanel panel = new MoveOutlinePanel();
        assertAllComponentsHaveTooltips(panel, "MoveOutlinePanel");
    }

    @Test
    void testMainPanelComponentsHaveTooltips() {
        MainPanel panel = new MainPanel();
        assertAllComponentsHaveTooltips(panel, "MainPanel");
    }

    /**
     * Asserts that all JButton, JTextField, JTextArea, and JList components
     * in the given panel have tooltips set (non-null and non-empty).
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private void assertAllComponentsHaveTooltips(Object panelObject, String panelName) {
        List<String> missingTooltips = new ArrayList<>();
        
        // Get all declared fields from the panel
        Field[] fields = panelObject.getClass().getDeclaredFields();
        
        for (Field field : fields) {
            field.setAccessible(true);
            
            try {
                Object fieldValue = field.get(panelObject);
                
                // Check if it's a component that needs a tooltip
                if (fieldValue instanceof JButton button) {
                    checkTooltip(button, field.getName(), missingTooltips);
                } else if (fieldValue instanceof JTextField textField) {
                    checkTooltip(textField, field.getName(), missingTooltips);
                } else if (fieldValue instanceof JTextArea textArea) {
                    checkTooltip(textArea, field.getName(), missingTooltips);
                } else if (fieldValue instanceof JList<?> list) {
                    checkTooltip(list, field.getName(), missingTooltips);
                }
            } catch (IllegalAccessException e) {
                fail("Unable to access field: " + field.getName(), e);
            }
        }
        
        if (!missingTooltips.isEmpty()) {
            String message = panelName + " is missing tooltips for: " + String.join(", ", missingTooltips);
            fail(message);
        }
    }

    /**
     * Checks if a component has a tooltip. If not, adds it to the missing list.
     */
    private void checkTooltip(JComponent component, String fieldName, List<String> missingTooltips) {
        String tooltip = component.getToolTipText();
        if (tooltip == null || tooltip.trim().isEmpty()) {
            missingTooltips.add(fieldName);
        }
    }
}

package de.kremerdaniel.openfoamcut.gui.helper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

/**
 * GUI helper methods
 */
public final class GuiHelper {
    
    /** Color for success messages and such */
    public static final Color DARK_GREEN = new Color(0, 102, 0);

    /**
     * Enables/disables every child and their children
     * @param container The parent container to traverse
     * @param enabled Enable or disable the components?
     */
    public static void setEnabledRecursive(Container container, boolean enabled) {
        container.setEnabled(enabled);
        for (Component c : container.getComponents()) {
            if (c instanceof Container) {
                setEnabledRecursive((Container) c, enabled);
            } else {
                c.setEnabled(enabled);
            }
        }
    }

    private GuiHelper() {

    }

}

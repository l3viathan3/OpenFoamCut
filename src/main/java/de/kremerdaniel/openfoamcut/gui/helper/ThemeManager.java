package de.kremerdaniel.openfoamcut.gui.helper;

import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages and switches GUI themes (light/dark)
 */
public final class ThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManager.class);

    /**
     * Apply the selected Theme
     * @param theme The new Theme to apply _now_
     */
    public static void setTheme(Theme theme) {
        try {
            if (theme == Theme.DARK) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }

            SwingUtilities.invokeLater(() -> {
                for (var frame : javax.swing.JFrame.getFrames()) {
                    SwingUtilities.updateComponentTreeUI(frame);
                    frame.repaint();
                }
            });

        } catch (Exception e) {
            logger.error("Failed to apply theme", e);
        }
    }

    private ThemeManager() {

    }
}
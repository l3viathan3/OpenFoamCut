package de.kremerdaniel.openfoamcut.controller;

import javax.swing.Timer;

import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import de.kremerdaniel.openfoamcut.cutting.GCodeConverter;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.gui.GenerateGCodePanel;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import lombok.Getter;
import lombok.Setter;

/**
 * Controller for the G-Code generation panel, Singleton
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class GenerateGCodeController {
    
    static @Getter(lazy = true)
    private final GenerateGCodeController instance = new GenerateGCodeController();

    private GenerateGCodeController() {

    }

    // Models
    private final StateManager sm = StateManager.getInstance();
    private final RuntimeModelManager rmm = RuntimeModelManager.getInstance();

    // View
    @Setter
    private GenerateGCodePanel gCodePanel;

    /**
     * Refreshes the view belonging to this controller
     */
    public void refreshGui() {
        changeTheme(sm.getTheme());

        gCodePanel.setFoamConfig(
            sm.getFoamDepth(),
            sm.getFoamHeight(),
            sm.getFoamWidth(),
            sm.getFoamOffsetX(),
            sm.getFoamOffsetLeft(),
            sm.getFoamOffsetRight());
        
        // Enable G-Code generation only when arrangement is locked
        gCodePanel.setGenerateButtonsEnabled(sm.isArrangeLockedDown());
    }

	/**
     * Apply the selected Theme
     * @param theme The new Theme to apply _now_
     */
    public void changeTheme(Theme theme) {
        gCodePanel.changeTheme(theme);
	}
    
    /**
     * Use everything in the current application to generate G-Code and display the result
     */
    public void triggerGCodeGeneration() {

        GCodeResult gcodeResult;
        try {
            gcodeResult = new GCodeConverter().generateGCodeFromState();
        } catch (IllegalStateException e) {
            throw new UserErrorException("Error generating gcode! (" + e.getMessage() + ")", e);
        }

        rmm.setLastGCodeResult(gcodeResult);
        gCodePanel.displayGCodeResult(gcodeResult);
        gCodePanel.displaySanityCheckResults(gcodeResult.getSanityCheckList());
    }

    /**
     * Starts the cutting / wire anchor position simulation
     */
    public void triggerPlaySimulation() {

        gCodePanel.setPlayPauseEnabled(false);

        GCodeResult g = rmm.getLastGCodeResult();
        if (g == null) {
            gCodePanel.setPlayPauseEnabled(true);
            return;
        }

        int totalLines = g.getLeftCut().getLines().size();
        double linesPerTick = totalLines / 2000.0;

        final double[] displayedLines = {0};
        final int[] ticks = {0};

        Timer timer = new Timer(10, null);

        timer.addActionListener(e -> {
            displayedLines[0] += linesPerTick;
            int linesToShow = (int) displayedLines[0];

            gCodePanel.displayGCodeResult(
                g.copyWithLinesSubset(linesToShow)
            );

            ticks[0]++;
            if (ticks[0] >= 2000) {
                timer.stop();
                gCodePanel.displayGCodeResult(g);
                gCodePanel.setPlayPauseEnabled(true);
            }
        });

        timer.start();
    }

}

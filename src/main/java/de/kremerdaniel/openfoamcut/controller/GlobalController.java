package de.kremerdaniel.openfoamcut.controller;

import com.formdev.flatlaf.util.StringUtils;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.gui.helper.ThemeManager;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import de.kremerdaniel.openfoamcut.parser.DatParser;
import de.kremerdaniel.openfoamcut.parser.DxfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Global controller for the application
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class GlobalController {

    private static GlobalController instance;

    private static final Logger logger = LoggerFactory.getLogger(GlobalController.class);

    /**
     * Gets the singleton instance of GlobalController
     * @return The instance
     */
    @SuppressWarnings("PMD.NonThreadSafeSingleton")
    public static GlobalController getInstance() {
        if (instance == null) {
            instance = new GlobalController();
        }
        return instance;
    }

    // Controllers
    private final ArrangeController arrangeController;
    private final CutOrderController cutOrderController;
    private final GenerateGCodeController generateGCodeController;
    private final MachineConfigController machineConfigController;
    private final Preview3DController preview3DController;

    // Models
    private final StateManager sm = StateManager.getInstance();
    private final RuntimeModelManager rmm = RuntimeModelManager.getInstance();

    private GlobalController() {
        arrangeController = ArrangeController.getInstance(this);
        cutOrderController = CutOrderController.getInstance(this);
        generateGCodeController = GenerateGCodeController.getInstance();
        machineConfigController = MachineConfigController.getInstance();
        preview3DController = Preview3DController.getInstance();
    }


    /**
     * Refreshes everything to display and triggeres all other controllers to do a refresh
     */
    public void refreshGui() {
        // Needed here...
        ThemeManager.setTheme(sm.getTheme());
        arrangeController.refreshGui();
        cutOrderController.refreshGui();
        generateGCodeController.refreshGui();
        machineConfigController.refreshGui();
        preview3DController.refreshGui();

        UserErrorException error = null;
        try {
            loadOutline(Side.LEFT, sm.getLeftOutlineFilePath());
        } catch (UserErrorException e) {
            error = e;
        }
        try {
            loadOutline(Side.RIGHT, sm.getRightOutlineFilePath());
        } catch (UserErrorException e) {
            error = e;
        }

        arrangeController.doFoamFitCheck();
        arrangeController.doAngleCalculation();

        if(error != null) {
            throw error;
        }
    }

    private void loadOutline(Side side, String path) {
        if(StringUtils.isEmpty(path)) {
            return;
        }
        logger.info("Loading outline for {} from {}", side, path);
        try {
            arrangeController.clearOutline(side);
            cutOrderController.clearOutline(side);

            CutOutline outline;
            if(path.endsWith("dxf")) {
                outline = DxfParser.loadCutOutline(path);
            } else if(path.endsWith("dat")) {
                outline = DatParser.loadCutOutline(path);
            } else {
                throw new UserErrorException("Could not load file " + path + "\nOnly .dxf and .dat are allowed.", null);
            }
                
            checkIslands(outline);
            rmm.setOutline(side, outline);
            
            // Use rotated outline from rmm for display
            arrangeController.setMainOutline(side, rmm.getOutline(side));
            cutOrderController.setMainOutline(side, rmm.getOutline(side));
            logger.info("Successfully loaded outline for {} with {} lines", side, outline.getLines().size());
        } catch (UserErrorException e) {
            logger.warn("Failed to load outline for {}: {}", side, e.getMessage());
            arrangeController.setInputFileError(side);
            throw e;
        }
    }

    private void checkIslands(CutOutline outline) {
        int islands = outline.countIslands();
        if (islands == 0) {
            throw new UserErrorException("Select an outline file with exactly 1 'island' of lines. This file appears to have none.\n"
                    + "For more on this see the 'Help' tab", null);
        }
        if (islands > 1) {
            throw new UserErrorException("Select an outline file with exactly 1 'island' of lines. This file appears to have " + islands + "\n"
                    + "For more on this see the 'Help' tab", null);
        }
    }

    /**
     * Apply the selected Theme
     * @param theme The new Theme to apply _now_
     */
    public void triggerThemeChange(Theme theme) {
        sm.setTheme(theme);
        arrangeController.changeTheme(theme);
        cutOrderController.changeTheme(theme);
        generateGCodeController.changeTheme(theme);
        preview3DController.changeTheme(theme);
        ThemeManager.setTheme(theme);
    }

}
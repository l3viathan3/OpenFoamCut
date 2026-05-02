package de.kremerdaniel.openfoamcut.controller;

import de.kremerdaniel.openfoamcut.cutting.GCodeConverter;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.gui.Preview3DPanel;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import de.kremerdaniel.openfoamcut.preview.Preview3DScene;
import de.kremerdaniel.openfoamcut.preview.Preview3DSceneBuilder;
import lombok.Getter;
import lombok.Setter;

/**
 * Controller for the 3D preview tab.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class Preview3DController {

    static @Getter(lazy = true)
    private final Preview3DController instance = new Preview3DController();

    private final StateManager stateManager = StateManager.getInstance();
    private final RuntimeModelManager runtimeModelManager = RuntimeModelManager.getInstance();
    private final Preview3DSceneBuilder sceneBuilder = new Preview3DSceneBuilder();

    @Setter
    private Preview3DPanel preview3DPanel;

    private Preview3DController() {

    }

    /**
     * Refreshes non-geometry UI state.
     */
    public void refreshGui() {
        if (preview3DPanel != null) {
            preview3DPanel.changeTheme(stateManager.getTheme());
        }
    }

    /**
     * Applies the selected theme.
     * @param theme Theme to apply
     */
    public void changeTheme(Theme theme) {
        if (preview3DPanel != null) {
            preview3DPanel.changeTheme(theme);
        }
    }

    /**
     * Regenerates the scene when the preview tab is opened.
     */
    public void refreshPreview() {
        if (preview3DPanel == null) {
            return;
        }

        try {
            GCodeResult result = new GCodeConverter().generateGCodeFromState();
            Preview3DScene scene = sceneBuilder.buildScene(result, stateManager, preview3DPanel.getMeshStep());
            runtimeModelManager.setLastGCodeResult(result);
            preview3DPanel.displayScene(scene);
        } catch (IllegalStateException e) {
            preview3DPanel.displayError(e.getMessage());
        }
    }
}
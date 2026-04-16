package de.kremerdaniel.openfoamcut.controller;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.gui.CutOrderPanel;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.StateManager;
import lombok.Setter;


/**
 * Controller for cut order management
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class CutOrderController {

    private static CutOrderController instance;

    /**
     * Gets the instance of CutOrderController
     * @param globalController The global controller
     * @return The instance
     */
    public static synchronized CutOrderController getInstance(GlobalController globalController) {
        if(instance == null) {
            instance = new CutOrderController(globalController);
        }
        return instance;
    }

    /**
     * Constructor for CutOrderController
     * @param globalController The global controller
     */
    private CutOrderController(GlobalController globalController) {
        this.globalController = globalController;
    }

    // Controller
    private final GlobalController globalController;

    // Models
    private final StateManager sm = StateManager.getInstance();

    // View
    @Setter
    private CutOrderPanel cutOrderPanel;

    /**
     * Refreshes the view belonging to this controller
     */
    public void refreshGui() {
        changeTheme(sm.getTheme());

        cutOrderPanel.setFoamConfig(
            sm.getFoamDepth(),
            sm.getFoamHeight(),
            sm.getFoamWidth(),
            sm.getFoamOffsetX(),
            sm.getFoamOffsetLeft(),
            sm.getFoamOffsetRight());

        cutOrderPanel.setMainOutlineOffset(Side.LEFT, sm.getOffsetX(Side.LEFT), sm.getOffsetY(Side.LEFT));
        cutOrderPanel.setMainOutlineOffset(Side.RIGHT, sm.getOffsetX(Side.RIGHT), sm.getOffsetY(Side.RIGHT));

        cutOrderPanel.setArrangeLockedDown(sm.isArrangeLockedDown());

        cutOrderPanel.setAdditionalLinesOf(Side.LEFT, sm.getLeftAdditionalLines());
        cutOrderPanel.setAdditionalLinesOf(Side.RIGHT, sm.getRightAdditionalLines());

        cutOrderPanel.setSynchronizedPoints(sm.getSynchronizedPoints());
        cutOrderPanel.setCutOrderPoints(sm.getCutOrder());
    }

    /**
     * Apply the selected Theme
     * @param theme The new Theme to apply _now_
     */
    public void changeTheme(Theme theme) {
        cutOrderPanel.changeTheme(theme);
    }

    /**
     * Clears the outline for the given side
     * @param side The side
     */
    public void clearOutline(Side side) {
        cutOrderPanel.setMainOutline(side, null);
    }

    /**
     * Sets the main outline for the given side
     * @param side The side
     * @param outline The outline
     */
    public void setMainOutline(Side side, CutOutline outline) {
        cutOrderPanel.setMainOutline(side, outline);
    }

    /**
     * Triggers locking down arrange
     * @param lock Whether to lock
     */
    public void triggerLockDownArrange(boolean lock) {
        if(!sm.isArrangeLockedDown() && lock) {
            boolean ok = cutOrderPanel.displayLockingDownWarning();
            if(ok) {
                sm.setArrangeLockedDown(lock);
            }
        } else if(sm.isArrangeLockedDown() && !lock) {
            boolean ok = cutOrderPanel.displayUnlockWarning();
            if(ok) {
                sm.setArrangeLockedDown(lock);
                // Delete all additional geometry when unlocking
                sm.deleteAllAdditionalGeometry();
            }
        }
        globalController.refreshGui();
    }
    
    /**
     * Triggers adding a line
     * @param side The side
     * @param x1 X1 coordinate
     * @param y1 Y1 coordinate
     * @param x2 X2 coordinate
     * @param y2 Y2 coordinate
     */
    public void triggerAddLine(Side side, double x1, double y1, double x2, double y2) {
        Line line = new Line(
            new Point(x1, y1),
            new Point(x2, y2));
        sm.addAdditionalLine(side, line);
        globalController.refreshGui();
    }

    /**
     * Triggers adding a sync point
     * @param lx Left X
     * @param ly Left Y
     * @param rx Right X
     * @param ry Right Y
     */
    public void triggerAddSyncPoint(double lx, double ly, double rx, double ry) {
        Point l = new Point(lx, ly);
        Point r = new Point(rx, ry);
        SyncedPoints sp = new SyncedPoints(l, r);
        sm.addSynchronizedPoint(sp);
        globalController.refreshGui();
    }

    /**
     * Removes an additional line from a given side
     * @param side Left/Right
     * @param line The line to remove
     */
    public void triggerRemoveLine(Side side, Line line) {
        sm.removeAdditionalLine(side, line);
        globalController.refreshGui();
    }

    /**
     * Removes a pair of synchronized points
     * @param point The pair of synchronized points
     */
    public void triggerRemoveSyncPoint(SyncedPoints point) {
        sm.removeSyncPoint(point);
        globalController.refreshGui();
    }

    /**
     * Add a point to the end of the cut order
     * @param point The point to add
     */
    public void triggerAddCutOrderPoint(SyncedPoints point) {
        sm.addCutOrderPoint(point);
        globalController.refreshGui();
    }

    /**
     * Remove a point from the cut order
     * @param index Index of the point to remove
     */
    public void triggerRemoveCutOrderPoint(int index) {
        sm.removeCutOrderPoint(index);
        globalController.refreshGui();
    }

    /**
     * Move a point up within the cut order
     * @param index Index of the point to move up
     */
    public void triggerMoveUpCutOrderPoint(int index) {
        sm.moveUpCutOrderPoint(index);
        globalController.refreshGui();
    }

    /**
     * Move a point down within the cut order
     * @param index Index of the point to move down
     */
    public void triggerMoveDownCutOrderPoint(int index) {
        sm.moveDownCutOrderPoint(index);
        globalController.refreshGui();
    }

}

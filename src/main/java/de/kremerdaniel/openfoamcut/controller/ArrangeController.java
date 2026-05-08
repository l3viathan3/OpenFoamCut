package de.kremerdaniel.openfoamcut.controller;

import de.kremerdaniel.openfoamcut.Logger;
import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.gui.ArrangePanel;
import de.kremerdaniel.openfoamcut.gui.MoveOutlineActionListener.MoveOutlineAction;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import lombok.Setter;

/**
 * Controller for arranging outlines
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class ArrangeController {

    private static final Logger logger = new Logger(ArrangeController.class);

    private static ArrangeController instance;

    /**
     * Gets the instance of ArrangeController
     * @param globalController The global controller
     * @return The instance
     */
    public static synchronized ArrangeController getInstance(GlobalController globalController) {
        if(instance == null) {
            instance = new ArrangeController(globalController);
        }
        return instance;
    }

    /**
     * Constructor for ArrangeController
     * @param globalController The global controller
     */
    private ArrangeController(GlobalController globalController) {
        this.globalController = globalController;
    }

    // Controller
    private final GlobalController globalController;

    // Models
    private final StateManager sm = StateManager.getInstance();
    private final RuntimeModelManager rmm = RuntimeModelManager.getInstance();

    // View
    @Setter
    private ArrangePanel arrangePanel;


    /**
     * Refreshes the view belonging to this controller
     */
    public void refreshGui() {
        changeTheme(sm.getTheme());

        arrangePanel.setOutlineFilePath(Side.LEFT, sm.getLeftOutlineFilePath());
        arrangePanel.setOutlineFilePath(Side.RIGHT, sm.getRightOutlineFilePath());

        arrangePanel.setFoamConfig(
            sm.getFoamDepth(),
            sm.getFoamHeight(),
            sm.getFoamWidth(),
            sm.getFoamOffsetX(),
            sm.getFoamOffsetLeft(),
            sm.getFoamOffsetRight());


        arrangePanel.setMainOutlineOffsetConfig(Side.LEFT, sm.getOffsetX(Side.LEFT), sm.getOffsetY(Side.LEFT));
        arrangePanel.setSecondaryOutlineOffsetConfig(Side.RIGHT, sm.getOffsetX(Side.LEFT), sm.getOffsetY(Side.LEFT));

        arrangePanel.setMainOutlineOffsetConfig(Side.RIGHT, sm.getOffsetX(Side.RIGHT), sm.getOffsetY(Side.RIGHT));
        arrangePanel.setSecondaryOutlineOffsetConfig(Side.LEFT, sm.getOffsetX(Side.RIGHT), sm.getOffsetY(Side.RIGHT));

        arrangePanel.setOutlineRotation(Side.LEFT, sm.getRotation(Side.LEFT));
        arrangePanel.setOutlineRotation(Side.RIGHT, sm.getRotation(Side.RIGHT));

        arrangePanel.setOutlineScale(Side.LEFT, sm.getScale(Side.LEFT));
        arrangePanel.setOutlineScale(Side.RIGHT, sm.getScale(Side.RIGHT));

        arrangePanel.setLockedDown(sm.isArrangeLockedDown());
    }

    /**
     * Clears the outline for the given side
     * @param side The side
     */
    public void clearOutline(Side side) {
        arrangePanel.setMainOutline(side, null);
        arrangePanel.setSecondaryOutline(side.other(), null);
    }

    /**
     * Sets the main outline for the given side
     * @param side The side
     * @param outline The outline
     */
    public void setMainOutline(Side side, CutOutline outline) {
        arrangePanel.setMainOutline(side, outline);
        // Set secondary outline on both panels: main panel gets other side's outline,
        // other panel gets this side's outline for comparison
        CutOutline otherOutline = rmm.getOutline(side.other());
        if (otherOutline != null) {
            arrangePanel.setSecondaryOutline(side, otherOutline);
        }
        arrangePanel.setSecondaryOutline(side.other(), outline);
        arrangePanel.clearFileInputError(side);
    }

    /**
     * Sets input file error for the given side
     * @param side The side
     */
    public void setInputFileError(Side side) {
        arrangePanel.setFileInputError(side);
    }

    /**
     * Performs foam fit check
     */
    public void doFoamFitCheck() {        
        if (rmm.getOutlineLeft() == null || rmm.getOutlineRight() == null) {
            arrangePanel.displayFoamFitMessage("Select outlines to check if they fit the foam", false, false);
            return;
        }
        boolean leftFits = rmm.getOutlineLeft().fitsInside(sm.getFoamDepth(), sm.getFoamHeight());
        boolean rightFits = rmm.getOutlineRight().fitsInside(sm.getFoamDepth(), sm.getFoamHeight());
        if (leftFits && rightFits) {
            arrangePanel.displayFoamFitMessage("Outlines fit the foam!", true, false);
        } else {
            arrangePanel.displayFoamFitMessage("At least one of the outlines does NOT fit the foam!", false, true);
        }
    }

    /**
     * Performs angle calculation
     */
    public void doAngleCalculation() {
        if(rmm.getOutline(Side.LEFT) == null || rmm.getOutline(Side.RIGHT) == null) {
            arrangePanel.setAngles(0, 0);
            return;
        }

        double distanceFront = Math.abs(sm.getOffsetX(Side.LEFT) - sm.getOffsetX(Side.RIGHT));
        double distanceBack = Math.abs(
            sm.getOffsetX(Side.LEFT) + rmm.getOutline(Side.LEFT).getBounds().getWidth()
            - (sm.getOffsetX(Side.RIGHT) + rmm.getOutline(Side.RIGHT).getBounds().getWidth()));

        double front = calcAngleDegree(sm.getFoamWidth(), distanceFront);
        double back = calcAngleDegree(sm.getFoamWidth(), distanceBack);
        arrangePanel.setAngles(front, back);
    }

    private double calcAngleDegree(double length, double offset) {
        double angleRad = Math.atan2(offset, length);
        return Math.toDegrees(angleRad);
    }

    /**
     * Apply the selected Theme
     * @param theme The new Theme to apply _now_
     */
    public void changeTheme(Theme theme) {
        arrangePanel.changeTheme(theme);
    }

    /**
     * Triggers selection of new file for the given side
     * @param side The side
     * @param path The file path
     */
    public void triggerSelectNewFile(Side side, String path) {        if(sm.isArrangeLockedDown()) {
            logger.info("Cannot change outline file: arrangement is locked");
            return;
        }        if(side == Side.LEFT) {
            sm.setLeftOutlineFilePath(path);
        } else {
            sm.setRightOutlineFilePath(path);
        }
        globalController.refreshGui();
    }

    /**
     * Triggers swapping left and right
     */
    public void triggerSwapLeftAndRight() {
        sm.swapLeftAndRight();
        rmm.swapLeftAndRight();
        arrangePanel.setMainOutline(Side.LEFT, rmm.getOutline(Side.LEFT));
        arrangePanel.setMainOutline(Side.RIGHT, rmm.getOutline(Side.RIGHT));
        // Update secondary outlines for proper display
        if (rmm.getOutlineRight() != null) {
            arrangePanel.setSecondaryOutline(Side.LEFT, rmm.getOutlineRight());
        }
        if (rmm.getOutlineLeft() != null) {
            arrangePanel.setSecondaryOutline(Side.RIGHT, rmm.getOutlineLeft());
        }

        globalController.refreshGui();
    }

    /**
     * Triggers foam configuration change
     * @param foamDepth Foam depth
     * @param foamHeight Foam height
     * @param foamWidth Foam width
     * @param foamXoffset Foam X offset
     * @param foamLeftOffset Foam left offset
     * @param foamRightOffset Foam right offset
     */
    public void triggerFoamChanged(String foamDepth, String foamHeight, String foamWidth, String foamXoffset, String foamLeftOffset, String foamRightOffset) {
        sm.setFoamDepth(getDoubleOrZero(foamDepth));
        sm.setFoamHeight(getDoubleOrZero(foamHeight));
        sm.setFoamWidth(getDoubleOrZero(foamWidth));

        sm.setFoamOffsetX(getDoubleOrZero(foamXoffset));
        sm.setFoamOffsetLeft(getDoubleOrZero(foamLeftOffset));
        sm.setFoamOffsetRight(getDoubleOrZero(foamRightOffset));

        globalController.refreshGui();
    }
    
    /**
     * Triggers rotation change for the given side
     * @param side The side
     * @param rotation The rotation value
     */
    public void triggerRotationChanged(Side side, String rotation) {
        if(sm.isArrangeLockedDown()) {
            logger.info("Cannot change rotation: arrangement is locked");
            return;
        }
        double rot = getDoubleOrZero(rotation);
        sm.setRotation(side, rot);
        rmm.updateRotation(side, rot);
        arrangePanel.setMainOutline(side, rmm.getOutline(side));
        // Update secondary outline on the current panel to the other side's rotated outline
        CutOutline otherOutline = rmm.getOutline(side.other());
        if (otherOutline != null) {
            arrangePanel.setSecondaryOutline(side, otherOutline);
        }
        // Update secondary outline on the other panel to this side's rotated outline
        arrangePanel.setSecondaryOutline(side.other(), rmm.getOutline(side));
        globalController.refreshGui();
    }

    /**
     * Triggers scale change for the given side
     * @param side The side
     * @param scale The scale value
     */
    public void triggerScaleChanged(Side side, String scale) {
        if(sm.isArrangeLockedDown()) {
            logger.info("Cannot change scale: arrangement is locked");
            return;
        }
        double scl = getDoubleOrZero(scale);
        sm.setScale(side, scl);
        rmm.updateScale(side, scl);
        arrangePanel.setMainOutline(side, rmm.getOutline(side));
        // Update secondary outline on the current panel to the other side's scaled outline
        CutOutline otherOutline = rmm.getOutline(side.other());
        if (otherOutline != null) {
            arrangePanel.setSecondaryOutline(side, otherOutline);
        }
        // Update secondary outline on the other panel to this side's scaled outline
        arrangePanel.setSecondaryOutline(side.other(), rmm.getOutline(side));
        globalController.refreshGui();
    }
    
    private double getDoubleOrZero(String value) {
        try {
            double d = Double.parseDouble(value);
            if(d < 0) {
                d = -d;
            }
            return d;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Triggers moving the outline
     * @param side The side
     * @param action The move action
     * @param offsetX X offset
     * @param offsetY Y offset
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    public void triggerMoveOutline(Side side, MoveOutlineAction action, String offsetX, String offsetY) {
        logger.info("At side {} action {} triggered, x={}, y={}", side, action, offsetX, offsetY);

        CutOutline outline = rmm.getOutline(side);
        CutOutline other = rmm.getOutline(side.other());

        double otherOffsetX = sm.getOffsetX(side.other());
        double otherOffsetY = sm.getOffsetY(side.other());

        double foamHeight = sm.getFoamHeight();
        double foamDepth = sm.getFoamDepth();

        double newOffsetX = sm.getOffsetX(side);
        double newOffsetY = sm.getOffsetY(side);

        switch(action) {
            case ALIGN_ABSOLUTE_BOTTOM:
                newOffsetY = 0;
                break;
            case ALIGN_ABSOLUTE_H_CENTER:
                newOffsetX = (foamDepth - outline.getBounds().getWidth()) / 2.;
                break;
            case ALIGN_ABSOLUTE_LEFT:
                newOffsetX = 0;
                break;
            case ALIGN_ABSOLUTE_RIGHT:
                newOffsetX = foamDepth - outline.getBounds().getWidth();
                break;
            case ALIGN_ABSOLUTE_TOP:
                newOffsetY = foamHeight - outline.getBounds().getHeight();
                break;
            case ALIGN_ABSOLUTE_V_CENTER:
                newOffsetY = (foamHeight - outline.getBounds().getHeight()) / 2.;
                break;
            case ALIGN_OTHER_BOTTOM:
                newOffsetY = otherOffsetY;
                break;
            case ALIGN_OTHER_H_CENTER:
                newOffsetX = otherOffsetX + (other.getBounds().getCenterX() - outline.getBounds().getCenterX());
                break;
            case ALIGN_OTHER_LEFT:
                newOffsetX = otherOffsetX;
                break;
            case ALIGN_OTHER_RIGHT:
                newOffsetX = otherOffsetX + (other.getBounds().getWidth() - outline.getBounds().getWidth());
                break;
            case ALIGN_OTHER_TOP:
                newOffsetY = otherOffsetY + (other.getBounds().getHeight() - outline.getBounds().getHeight());
                break;
            case ALIGN_OTHER_V_CENTER:
                newOffsetY = otherOffsetY + (other.getBounds().getCenterY() - outline.getBounds().getCenterY());
                break;
            case OFFSET_MANUALLY_CHANGED:
                newOffsetX = getDoubleOrZero(offsetX);
                newOffsetY = getDoubleOrZero(offsetY);
                break;
            default:
                break;

        }

        // Only update if change
        final double eps = 1e-6;
        double oldX = sm.getOffsetX(side);
        double oldY = sm.getOffsetY(side);

        boolean xChanged = Math.abs(oldX - newOffsetX) > eps;
        boolean yChanged = Math.abs(oldY - newOffsetY) > eps;

        if (xChanged || yChanged) {
            sm.updateOffsetAndMoveGeometry(side, newOffsetX, newOffsetY);
            globalController.refreshGui();
        }
    }
}

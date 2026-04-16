package de.kremerdaniel.openfoamcut.model;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import lombok.Data;
import lombok.Getter;

/**
 * Stores everything that should only be loaded for runtime and not
 * saved in our own save files to keep everything as small as possible
 */
@Data
public class RuntimeModelManager {

    static @Getter(lazy = true)
    private final RuntimeModelManager instance = new RuntimeModelManager();
    
    private CutOutline outlineLeft;
    private CutOutline outlineRight;
    private CutOutline originalOutlineLeft;
    private CutOutline originalOutlineRight;

    private GCodeResult lastGCodeResult;

    /**
     * Sets the loaded outline for a dxf file
     * @param side The side for that the file was loaded
     * @param outline The actual outline data
     */
    public void setOutline(Side side, CutOutline outline) {
        StateManager sm = StateManager.getInstance();
        CutOutline transformed = outline.copy();
        transformed.rotate(sm.getRotation(side));
        transformed.scale(sm.getScale(side));
        if(side == Side.LEFT) {
            setOriginalOutlineLeft(outline);
            setOutlineLeft(transformed);
        } else {
            setOriginalOutlineRight(outline);
            setOutlineRight(transformed);
        }
    }

    /**
     * Returns the loaded outline for a given side
     * @param side The side to get the outline for
     * @return The outline for the side
     */
    public CutOutline getOutline(Side side) {
        if(side == Side.LEFT) {
            return getOutlineLeft();
        } else {
            return getOutlineRight();
        }
    }

    /**
     * Returns the original loaded outline for a given side
     * @param side The side to get the original outline for
     * @return The original outline for the side
     */
    public CutOutline getOriginalOutline(Side side) {
        if(side == Side.LEFT) {
            return getOriginalOutlineLeft();
        } else {
            return getOriginalOutlineRight();
        }
    }

    /**
     * Updates the rotation for the given side
     * @param side The side
     * @param rotation The rotation in degrees
     */
    public void updateRotation(Side side, double rotation) {
        CutOutline original = getOriginalOutline(side);
        if (original != null) {
            CutOutline transformed = original.copy();
            transformed.rotate(rotation);
            transformed.scale(StateManager.getInstance().getScale(side));
            if (side == Side.LEFT) {
                setOutlineLeft(transformed);
            } else {
                setOutlineRight(transformed);
            }
        }
    }

    /**
     * Updates the scale for the given side
     * @param side The side
     * @param scale The scale factor
     */
    public void updateScale(Side side, double scale) {
        CutOutline original = getOriginalOutline(side);
        if (original != null) {
            CutOutline transformed = original.copy();
            transformed.rotate(StateManager.getInstance().getRotation(side));
            transformed.scale(scale);
            if (side == Side.LEFT) {
                setOutlineLeft(transformed);
            } else {
                setOutlineRight(transformed);
            }
        }
    }

    /**
     * Swaps left and right outlines and G-code result
     */
    public void swapLeftAndRight() {
        CutOutline tmp = outlineLeft;
        outlineLeft = outlineRight;
        outlineRight = tmp;

        CutOutline tmpOrig = originalOutlineLeft;
        originalOutlineLeft = originalOutlineRight;
        originalOutlineRight = tmpOrig;

        if(lastGCodeResult != null) {
            lastGCodeResult.swapLeftAndRight();
        }
    }

}

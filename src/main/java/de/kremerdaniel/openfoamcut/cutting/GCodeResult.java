package de.kremerdaniel.openfoamcut.cutting;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of a gcode calculation
 */
@Data
@NoArgsConstructor
public class GCodeResult {
    
    private String gcode;
    private CutOutline leftCut;
    private CutOutline rightCut;

    private CutOutline leftMove;
    private CutOutline rightMove;
    
    private String sanityCheckResults = "";
    private List<SanityCheckValidator.CheckResult> sanityCheckList = new ArrayList<>();

    /**
     * Constructor for G-Code generation results
     * @param gcode The generated G-code string
     * @param leftCut The cut outline for the left side
     * @param rightCut The cut outline for the right side
     * @param leftMove The movement trajectory for the left anchor
     * @param rightMove The movement trajectory for the right anchor
     */
    public GCodeResult(String gcode, CutOutline leftCut, CutOutline rightCut, 
                       CutOutline leftMove, CutOutline rightMove) {
        this.gcode = gcode;
        this.leftCut = leftCut;
        this.rightCut = rightCut;
        this.leftMove = leftMove;
        this.rightMove = rightMove;
        this.sanityCheckResults = "";
    }

    /**
     * Swaps the left and right side of the cuts, RESETS gcode
     */
    public void swapLeftAndRight() {
        CutOutline tmp = leftCut;
        leftCut = rightCut;
        rightCut = tmp;

        tmp = leftMove;
        leftMove = rightMove;
        rightMove = tmp;

        gcode = "";
    }

    /**
     * Creates a copy with a subset of CutOutlines lines
     * @param count How many lines should be in the copy
     * @return A _shallow_ copy with less lines
     */
    public GCodeResult copyWithLinesSubset(int count) {
        // TODO: this is a gigantic hack. REFACTOR
        GCodeResult g = new GCodeResult();
        g.setGcode(gcode);
        g.setLeftCut(leftCut.copyWithLinesSubset(count));
        g.setRightCut(rightCut.copyWithLinesSubset(count));
        g.setLeftMove(leftMove.copyWithLinesSubset(count));
        g.setRightMove(rightMove.copyWithLinesSubset(count));

        return g;
    }

    /**
     * Returns the topmost "point" of all the lines in the cuts in this result.
     * The _assumption_ is that that is the last cut point when simulating
     * @param side The side for which to get the latest anchor point
     * @return The latest anchor point
     */
    public Point getLastAnchorPoint(Side side) {
        CutOutline anchorOutline = side == Side.LEFT ? leftMove : rightMove;

        if(anchorOutline.getLines().isEmpty()) {
            return new Point();
        }

        return anchorOutline.getLines().get(anchorOutline.getLines().size()-1).getEnd();
    }

}

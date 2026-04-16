package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GCodeResult functionality.
 */
class GCodeResultTest {

    @Test
    void testSwapLeftAndRightCorrectlyExchangesOutlines() {
        // Create distinct outlines for left and right
        CutOutline leftOutline = createOutline("left");
        CutOutline rightOutline = createOutline("right");
        CutOutline leftMove = createOutline("leftMove");
        CutOutline rightMove = createOutline("rightMove");

        GCodeResult result = new GCodeResult("test-gcode", leftOutline, rightOutline, leftMove, rightMove);

        // Verify initial state
        assertEquals("left", getOutlineLabel(result.getLeftCut()));
        assertEquals("right", getOutlineLabel(result.getRightCut()));
        assertEquals("leftMove", getOutlineLabel(result.getLeftMove()));
        assertEquals("rightMove", getOutlineLabel(result.getRightMove()));

        // Perform swap
        result.swapLeftAndRight();

        // Verify swap exchanged the outlines correctly
        assertEquals("right", getOutlineLabel(result.getLeftCut()), 
                     "After swap, left cut should contain original right outline");
        assertEquals("left", getOutlineLabel(result.getRightCut()), 
                     "After swap, right cut should contain original left outline");
        assertEquals("rightMove", getOutlineLabel(result.getLeftMove()), 
                     "After swap, left move should contain original right move");
        assertEquals("leftMove", getOutlineLabel(result.getRightMove()), 
                     "After swap, right move should contain original left move");
        
        // Verify gcode was reset
        assertEquals("", result.getGcode(), "G-code should be reset after swap");
    }

    /**
     * Creates a test outline with a distinctive label embedded in a line
     */
    private CutOutline createOutline(String label) {
        List<Line> lines = new ArrayList<>();
        // Store label in first coordinate value for identification
        double labelValue = label.hashCode() / 1000000.0;
        lines.add(new Line(new Point(labelValue, 0), new Point(labelValue, 1)));
        CutOutline outline = new CutOutline();
        outline.setLines(lines, false);
        return outline;
    }

    /**
     * Extracts the label hash from an outline for verification
     */
    private String getOutlineLabel(CutOutline outline) {
        if (outline == null || outline.getLines().isEmpty()) {
            return "empty";
        }
        double labelValue = outline.getLines().get(0).getStart().getX();
        String[] labels = {"left", "right", "leftMove", "rightMove"};
        for (String label : labels) {
            if (Math.abs(label.hashCode() / 1000000.0 - labelValue) < 0.001) {
                return label;
            }
        }
        return "unknown";
    }
}

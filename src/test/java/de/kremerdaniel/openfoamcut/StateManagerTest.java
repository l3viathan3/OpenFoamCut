package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.model.StateManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StateManager functionality.
 */
class StateManagerTest {

    @Test
    void testSwapLeftAndRightSynchronizedPoints() {
        StateManager sm = StateManager.getInstance();

        // Clear existing
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();

        // Add a sync point
        Point left = new Point(10, 20);
        Point right = new Point(30, 40);
        SyncedPoints sp = new SyncedPoints(left, right);
        sm.getSynchronizedPoints().add(sp);

        // Swap
        sm.swapLeftAndRight();

        // Verify swapped
        assertEquals(30, sp.getLeft().getX());
        assertEquals(40, sp.getLeft().getY());
        assertEquals(10, sp.getRight().getX());
        assertEquals(20, sp.getRight().getY());
    }

    @Test
    void testSwapLeftAndRightFilePaths() {
        StateManager sm = StateManager.getInstance();

        sm.setLeftOutlineFilePath("left.dxf");
        sm.setRightOutlineFilePath("right.dxf");

        sm.swapLeftAndRight();

        assertEquals("right.dxf", sm.getLeftOutlineFilePath());
        assertEquals("left.dxf", sm.getRightOutlineFilePath());
    }

    @Test
    void testUpdateOffsetAndMoveGeometryPreservesOriginPoints() {
        StateManager sm = StateManager.getInstance();

        // Clear existing
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();
        sm.getLeftAdditionalLines().clear();

        // Setup: add a line from (0,0) to (5,5) and from (8,8) to (10,10)
        Line line1 = new Line(new Point(0, 0), new Point(5, 5));
        Line line2 = new Line(new Point(8, 8), new Point(10, 10));
        sm.addAdditionalLine(Side.LEFT, line1);
        sm.addAdditionalLine(Side.LEFT, line2);

        // Apply offset (10, 10)
        sm.updateOffsetAndMoveGeometry(Side.LEFT, 10, 10);

        // Verify
        // Offset should be updated
        assertEquals(10, sm.getOffsetX(Side.LEFT));
        assertEquals(10, sm.getOffsetY(Side.LEFT));

        // Line 1: (0,0) to (5,5) should become (0,0) to (15,15)
        // The actual lists are modified in place
        var lines = sm.getLeftAdditionalLines();
        assertEquals(2, lines.size());
        
        // First line: origin point preserved, other point moved
        Line l1 = lines.get(0);
        assertEquals(0, l1.getStart().getX());
        assertEquals(0, l1.getStart().getY());
        assertEquals(15, l1.getEnd().getX(), 0.01);
        assertEquals(15, l1.getEnd().getY(), 0.01);

        // Second line: both points moved
        Line l2 = lines.get(1);
        assertEquals(18, l2.getStart().getX(), 0.01);
        assertEquals(18, l2.getStart().getY(), 0.01);
        assertEquals(20, l2.getEnd().getX(), 0.01);
        assertEquals(20, l2.getEnd().getY(), 0.01);
    }

    @Test
    void testUpdateOffsetAndMoveGeometrySyncPoints() {
        StateManager sm = StateManager.getInstance();

        // Clear existing
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();

        // Add origin point (or clear and add)
        sm.getSynchronizedPoints().clear();
        sm.getSynchronizedPoints().add(new SyncedPoints(new Point(0, 0), new Point(0, 0)));

        // Add sync points
        SyncedPoints sp1 = new SyncedPoints(new Point(0, 0), new Point(5, 5));
        SyncedPoints sp2 = new SyncedPoints(new Point(8, 8), new Point(10, 10));
        sm.getSynchronizedPoints().add(sp1);
        sm.getSynchronizedPoints().add(sp2);

        // Apply offset (10, 10) to LEFT side
        sm.updateOffsetAndMoveGeometry(Side.LEFT, 10, 10);

        // Verify sync points on left side
        // sp1: left (0,0) stays (0,0) - origin preserved, right (5,5) should NOT be moved
        assertEquals(0, sp1.getLeft().getX());
        assertEquals(0, sp1.getLeft().getY());
        assertEquals(5, sp1.getRight().getX());  // Right side unchanged
        assertEquals(5, sp1.getRight().getY());

        // sp2: left (8,8) becomes (18,18), right stays the same (not LEFT side)
        assertEquals(18, sp2.getLeft().getX(), 0.01);
        assertEquals(18, sp2.getLeft().getY(), 0.01);
        assertEquals(10, sp2.getRight().getX());
        assertEquals(10, sp2.getRight().getY());
    }

    @Test
    void testUpdateOffsetAndMoveGeometryRightSide() {
        StateManager sm = StateManager.getInstance();

        // Clear existing
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();

        // Add origin point
        sm.getSynchronizedPoints().clear();
        sm.getSynchronizedPoints().add(new SyncedPoints(new Point(0, 0), new Point(0, 0)));

        // Add sync point with origin on right
        SyncedPoints sp = new SyncedPoints(new Point(5, 5), new Point(0, 0));
        sm.getSynchronizedPoints().add(sp);

        // Apply offset (10, 10) to RIGHT side
        sm.updateOffsetAndMoveGeometry(Side.RIGHT, 10, 10);

        // Verify: left (5,5) should stay, right (0,0) should stay at origin, but offset should update
        assertEquals(5, sp.getLeft().getX());
        assertEquals(5, sp.getLeft().getY());
        assertEquals(0, sp.getRight().getX());
        assertEquals(0, sp.getRight().getY());

        // Check offset was updated
        assertEquals(10, sm.getOffsetX(Side.RIGHT));
        assertEquals(10, sm.getOffsetY(Side.RIGHT));
    }

    @Test
    void testDeleteAllAdditionalGeometry() {
        StateManager sm = StateManager.getInstance();

        // Setup: add lines, sync points, and cut order
        sm.getLeftAdditionalLines().clear();
        sm.getRightAdditionalLines().clear();
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();

        Line line = new Line(new Point(1, 2), new Point(3, 4));
        sm.addAdditionalLine(Side.LEFT, line);

        SyncedPoints sp = new SyncedPoints(new Point(5, 6), new Point(7, 8));
        sm.addSynchronizedPoint(sp);
        sm.addCutOrderPoint(sp);

        // Verify they exist
        assertEquals(1, sm.getLeftAdditionalLines().size());
        assertFalse(sm.getSynchronizedPoints().isEmpty());
        assertEquals(1, sm.getCutOrder().size());

        // Delete all
        sm.deleteAllAdditionalGeometry();

        // Verify all deleted
        assertEquals(0, sm.getLeftAdditionalLines().size());
        assertEquals(0, sm.getRightAdditionalLines().size());
        // Origin point should remain
        assertEquals(1, sm.getSynchronizedPoints().size());
        SyncedPoints origin = sm.getSynchronizedPoints().get(0);
        assertEquals(0, origin.getLeft().getX());
        assertEquals(0, origin.getLeft().getY());
        assertEquals(0, origin.getRight().getX());
        assertEquals(0, origin.getRight().getY());
        assertEquals(0, sm.getCutOrder().size());
    }
}
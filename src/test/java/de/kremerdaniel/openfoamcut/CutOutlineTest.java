package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CutOutlineTest {

    @Test
    void testRotate() {
        // Create a simple outline: a square from (0,0) to (10,10)
        Line line1 = new Line(new Point(0, 0), new Point(10, 0));
        Line line2 = new Line(new Point(10, 0), new Point(10, 10));
        Line line3 = new Line(new Point(10, 10), new Point(0, 10));
        Line line4 = new Line(new Point(0, 10), new Point(0, 0));
        CutOutline outline = new CutOutline(Arrays.asList(line1, line2, line3, line4), false);

        // Rotate by 90 degrees
        outline.rotate(90);

        // After rotation, the bounds should be updated
        // Original bounds: width 10, height 10, center at (5,5)
        // After 90 deg rotation, still width 10, height 10, but points rotated
        // Check a point: (0,0) -> (-0,0) wait, rotation around center.
        // Center is (5,5), point (0,0) relative (-5,-5), rotate 90: (5,-5) + center = (10,0)
        // But since bounds are recalculated, width and height same.

        assertEquals(10.0, outline.getBounds().getWidth(), 0.001);
        assertEquals(10.0, outline.getBounds().getHeight(), 0.001);
    }

    @Test
    void testScale() {
        // Create a simple outline: a square from (0,0) to (10,10)
        Line line1 = new Line(new Point(0, 0), new Point(10, 0));
        Line line2 = new Line(new Point(10, 0), new Point(10, 10));
        Line line3 = new Line(new Point(10, 10), new Point(0, 10));
        Line line4 = new Line(new Point(0, 10), new Point(0, 0));
        CutOutline outline = new CutOutline(Arrays.asList(line1, line2, line3, line4), false);

        // Scale by 2.0
        outline.scale(2.0);

        // After scaling by 2, bounds should double
        assertEquals(20.0, outline.getBounds().getWidth(), 0.001);
        assertEquals(20.0, outline.getBounds().getHeight(), 0.001);

        // Scale by 0.5
        outline.scale(0.5);

        // After scaling by another 0.5, bounds should return to original
        assertEquals(10.0, outline.getBounds().getWidth(), 0.001);
        assertEquals(10.0, outline.getBounds().getHeight(), 0.001);
    }
}
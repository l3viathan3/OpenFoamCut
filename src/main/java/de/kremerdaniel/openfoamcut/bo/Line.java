package de.kremerdaniel.openfoamcut.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a line segment between two points
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Line {

    private Point start = new Point();
    private Point end = new Point();

    /**
     * Returns a canonical version of this line
     * @return Canonical line
     */
    public Line toCanonical() {
        return new Line(
            getStart().toCanonical(),
            getEnd().toCanonical()
        );
    }

    /**
     * Creates a copy of this line
     * @return A copy of this line
     */
    public Line copy() {
        return new Line(start.copy(), end.copy());
    }

    /**
     * Returns the distance from a <code>Point</code> to this line
     * segment.
     * The distance measured is the distance between the specified
     * point and the closest point between the current line's end points.
     * If the specified point intersects the line segment in between the
     * end points, this method returns 0.0.
     * @param pt the specified <code>Point</code> being measured
     *          against this line segment
     * @return a double value that is the distance from the specified
     *                          <code>Point</code> to the current line
     *                          segment.
     */
    public double ptSegDist(Point pt) {
        return ptSegDist(getStart().getX(), getStart().getY(), getEnd().getX(), getEnd().getY(), pt.getX(), pt.getY());
    }

    private static double ptSegDistSq(double x1, double y1,
            double x2, double y2,
            double px, double py) {
        // Adjust vectors relative to x1,y1
        // x2,y2 becomes relative vector from x1,y1 to end of segment
        x2 -= x1;
        y2 -= y1;
        // px,py becomes relative vector from x1,y1 to test point
        px -= x1;
        py -= y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq;
        if (dotprod <= 0.0) {
            // px,py is on the side of x1,y1 away from x2,y2
            // distance to segment is length of px,py vector
            // "length of its (clipped) projection" is now 0.0
            projlenSq = 0.0;
        } else {
            // switch to backwards vectors relative to x2,y2
            // x2,y2 are already the negative of x1,y1=>x2,y2
            // to get px,py to be the negative of px,py=>x2,y2
            // the dot product of two negated vectors is the same
            // as the dot product of the two normal vectors
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0.0) {
                // px,py is on the side of x2,y2 away from x1,y1
                // distance to segment is length of (backwards) px,py vector
                // "length of its (clipped) projection" is now 0.0
                projlenSq = 0.0;
            } else {
                // px,py is between x1,y1 and x2,y2
                // dotprod is the length of the px,py vector
                // projected on the x2,y2=>x1,y1 vector times the
                // length of the x2,y2=>x1,y1 vector
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        // Distance to line is now the length of the relative point
        // vector minus the length of its projection onto the line
        // (which is zero if the projection falls outside the range
        // of the line segment).
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    private static double ptSegDist(double x1, double y1,
            double x2, double y2,
            double px, double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }


}
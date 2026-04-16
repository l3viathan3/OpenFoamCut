package de.kremerdaniel.openfoamcut.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A point in 2D space
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {

    private static final double EPS = 0.01;

    private double x;
    private double y;

    /**
     * Creates a copy of this point
     * @return A copy of this point
     */
    public Point copy() {
        return new Point(x, y);
    }

    /**
     * Offsets this point by the given amounts
     * @param offX X offset
     * @param offY Y offset
     * @return This point
     */
    public Point offset(double offX, double offY) {
        this.x += offX;
        this.y += offY;
        return this;
    }

    /**
     * Returns a canonical version of this point
     * @return Canonical point
     */
    public Point toCanonical() {
        return new Point(
            Math.round(getX() / EPS) * EPS,
            Math.round(getY() / EPS) * EPS
        );
    }

    /**
     * Calculates the distance to another point
     * @param other The other point
     * @return Distance
     */
    public double distanceTo(Point other) {
        double dx = other.getX() - this.getX();
        double dy = other.getY() - this.getY();
        return Math.hypot(dx, dy);
    }

    /**
     * Calculates the distance to the given coordinates
     * @param x X coordinate
     * @param y Y coordinate
     * @return Distance
     */
    public double distanceTo(double x, double y) {
        double dx = x - this.getX();
        double dy = y - this.getY();
        return Math.hypot(dx, dy);
    }
}
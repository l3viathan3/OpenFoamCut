package de.kremerdaniel.openfoamcut.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents synchronized points for left and right sides
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncedPoints {

    private static final double EPS = 0.01;

    private Point left = new Point();
    private Point right = new Point();

    /**
     * Checks if this synced points is the same as another
     * @param other The other synced points
     * @return True if same
     */
    public boolean same(SyncedPoints other) {
        return this.getLeft().distanceTo(other.getLeft()) < EPS &&
            this.getRight().distanceTo(other.getRight()) < EPS;
    }

    /**
     * Swaps the left and right points
     */
    public void swapLeftAndRight() {
        Point tmp = left.copy();
        left = right.copy();
        right = tmp;
    }
}
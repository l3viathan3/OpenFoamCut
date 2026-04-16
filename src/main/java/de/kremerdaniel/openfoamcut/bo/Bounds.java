package de.kremerdaniel.openfoamcut.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents bounding box with min and max points
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bounds {
    private Point min = new Point();
    private Point max = new Point();

    /**
     * Gets the width of the bounds
     * @return Width
     */
    public double getWidth() {
        return max.getX() - min.getX();
    }

    /**
     * Gets the height of the bounds
     * @return Height
     */
    public double getHeight() {
        return max.getY() - min.getY();
    }

    /**
     * Gets the center X coordinate
     * @return Center X
     */
    public double getCenterX() {
        return min.getX() + (getWidth() / 2.);
    }

    /**
     * Gets the center Y coordinate
     * @return Center Y
     */
    public double getCenterY() {
        return min.getY() + (getHeight() / 2.);
    }


}

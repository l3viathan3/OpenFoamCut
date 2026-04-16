package de.kremerdaniel.openfoamcut.bo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.kremerdaniel.openfoamcut.controller.OutlineAnalyzer;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a cut outline consisting of lines
 */
@NoArgsConstructor
public class CutOutline {
    
    @Getter
    private List<Line> lines = new ArrayList<>();

    @Getter
    private Bounds bounds = new Bounds();

    /**
     * Constructor for CutOutline
     * @param lines List of lines
     * @param noramlize Whether to normalize
     */
    public CutOutline(List<Line> lines, boolean noramlize) {
        setLines(lines, noramlize);
    }

    /**
     * Creates a copy of this cut outline
     * @return A copy of this cut outline
     */
    public CutOutline copy() {
        return new CutOutline(lines.stream().map(Line::copy).collect(Collectors.toList()), false);
    }

    /**
     * Creates a copy with a subset of lines
     * @param count Number of lines to include
     * @return A copy with the subset
     */
    public CutOutline copyWithLinesSubset(int count) {
        CutOutline c = new CutOutline();
        c.setLines(lines.subList(0, count), false);
        return c;
    }

    /**
     * Sets the lines for this cut outline
     * @param lines List of lines
     * @param normalize Whether to normalize
     */
    public void setLines(List<Line> lines, boolean normalize) {
        this.lines = lines.stream().map(Line::copy).collect(Collectors.toList());
        this.bounds = calculateBoundsMaybeNormalize(normalize);
    }

    private Bounds calculateBoundsMaybeNormalize(boolean normalize) {
        if (lines.isEmpty()) {
            return new Bounds();
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Line line : lines) {
            minX = Math.min(minX, Math.min(line.getStart().getX(), line.getEnd().getX()));
            minY = Math.min(minY, Math.min(line.getStart().getY(), line.getEnd().getY()));
            maxX = Math.max(maxX, Math.max(line.getStart().getX(), line.getEnd().getX()));
            maxY = Math.max(maxY, Math.max(line.getStart().getY(), line.getEnd().getY()));
        }
        if(normalize) {
            applyOffset(-minX, -minY); // normalize to have the bounding corner at (0, 0)
        }
        return new Bounds(new Point(0, 0), new Point(maxX - minX, maxY - minY));
    }

    private void applyOffset(double x, double y) {
        for (Line line : lines) {
            Point sp = line.getStart();
            Point ep = line.getEnd();

            sp.setX(sp.getX() + x);
            sp.setY(sp.getY() + y);
            ep.setX(ep.getX() + x);
            ep.setY(ep.getY() + y);
        }
    }

    /**
     * Counts the number of islands in the outline
     * @return Number of islands
     */
    public int countIslands() {
        return OutlineAnalyzer.countIslands(lines);
    }

    /**
     * Checks if the outline fits inside the given foam dimensions
     * @param foamWidth Width of the foam
     * @param foamHeight Height of the foam
     * @return True if it fits
     */
    public boolean fitsInside(double foamWidth, double foamHeight) {
        return bounds.getWidth() <= foamWidth && bounds.getHeight() <= foamHeight;
    }

    /**
     * Scales the cut outline
     * @param scale The scale factor
     */
    public void scale(double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be > 0");
        }

        Point center = new Point(bounds.getCenterX(), bounds.getCenterY());
        List<Line> scaled = new ArrayList<>(lines.size());

        for (Line line : lines) {
            Point newStart = scalePoint(line.getStart(), center, scale);
            Point newEnd   = scalePoint(line.getEnd(), center, scale);
            scaled.add(new Line(newStart, newEnd));
        }

        setLines(scaled, true);
    }

    private static Point scalePoint(Point p, Point center, double scale) {
        double x = center.getX() + (p.getX() - center.getX()) * scale;
        double y = center.getY() + (p.getY() - center.getY()) * scale;
        return new Point(x, y);
    }

    /**
     * Rotates the cut outline
     * @param degrees The rotation angle in degrees
     */
    public void rotate(double degrees) {
        double radians = Math.toRadians(degrees);
        Point center = new Point(bounds.getCenterX(), bounds.getCenterY());
        List<Line> rotated = new ArrayList<>(lines.size());

        for (Line line : lines) {
            Point newStart = rotatePoint(line.getStart(), center, radians);
            Point newEnd   = rotatePoint(line.getEnd(), center, radians);
            rotated.add(new Line(newStart, newEnd));
        }

        setLines(rotated, true);
    }

    private static Point rotatePoint(Point p, Point center, double radians) {
        double x = p.getX() - center.getX();
        double y = p.getY() - center.getY();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double newX = x * cos - y * sin + center.getX();
        double newY = x * sin + y * cos + center.getY();
        return new Point(newX, newY);
    }

}

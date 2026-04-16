package de.kremerdaniel.openfoamcut.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;

/**
 * Manages the application state
 */
@Data
@XmlRootElement(name = "state")
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings({"PMD.AvoidUsingVolatile", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public final class StateManager {

    static @Getter(lazy = true)
    private final StateManager instance = new StateManager();

    private StateManager() {
        // Always keep the origin as a synchronized point
        synchronizedPoints.add(new SyncedPoints(new Point(0, 0), new Point(0, 0)));
    }

    private Theme theme = Theme.DARK;

    private String leftOutlineFilePath = "";
    private String rightOutlineFilePath = "";

    private volatile double foamHeight = 50;
    private volatile double foamWidth = 1000;
    private volatile double foamDepth = 500;

    private volatile double foamOffsetX = 10;
    private volatile double foamOffsetLeft = 20;
    private volatile double foamOffsetRight = 20;

    private volatile double leftOffsetX = 0;
    private volatile double leftOffsetY = 0;

    private volatile double rightOffsetX = 0;
    private volatile double rightOffsetY = 0;

    private volatile double leftRotation = 0;
    private volatile double rightRotation = 0;

    private volatile double leftScale = 1.0;
    private volatile double rightScale = 1.0;

    private volatile boolean arrangeLockedDown = false;

    private List<Line> leftAdditionalLines = new ArrayList<>();
    private List<Line> rightAdditionalLines = new ArrayList<>();

    private List<SyncedPoints> synchronizedPoints = new ArrayList<>();
    private List<SyncedPoints> cutOrder = new ArrayList<>();

    /**
     * Swaps left and right configurations
     */
    public void swapLeftAndRight() {
        String tmpS = getLeftOutlineFilePath();
        setLeftOutlineFilePath(getRightOutlineFilePath());
        setRightOutlineFilePath(tmpS);

        double tmpD = getLeftOffsetX();
        setLeftOffsetX(getRightOffsetX());
        setRightOffsetX(tmpD);

        tmpD = getLeftOffsetY();
        setLeftOffsetY(getRightOffsetY());
        setRightOffsetY(tmpD);

        tmpD = getLeftRotation();
        setLeftRotation(getRightRotation());
        setRightRotation(tmpD);

        tmpD = getLeftScale();
        setLeftScale(getRightScale());
        setRightScale(tmpD);

        tmpD = getFoamOffsetLeft();
        setFoamOffsetLeft(getFoamOffsetRight());
        setFoamOffsetRight(tmpD);

        List<Line> tmpL = getLeftAdditionalLines();
        setLeftAdditionalLines(getRightAdditionalLines());
        setRightAdditionalLines(tmpL);

        synchronizedPoints.forEach(SyncedPoints::swapLeftAndRight);
    }

    /**
     * Loads state from file
     * @param path File path
     * @throws FileNotFoundException If file not found
     * @throws JAXBException If JAXB error
     */
    public void loadFrom(String path) throws FileNotFoundException, JAXBException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }

        JAXBContext context = JAXBContext.newInstance(StateManager.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StateManager loaded = (StateManager) unmarshaller.unmarshal(file);

        // copy values into singleton

        this.leftOutlineFilePath = loaded.leftOutlineFilePath;
        this.rightOutlineFilePath = loaded.rightOutlineFilePath;

        this.foamHeight = loaded.foamHeight;
        this.foamWidth = loaded.foamWidth;
        this.foamDepth = loaded.foamDepth;

        this.foamOffsetX = loaded.foamOffsetX;
        this.foamOffsetLeft = loaded.foamOffsetLeft;
        this.foamOffsetRight = loaded.foamOffsetRight;

        this.leftOffsetX = loaded.leftOffsetX;
        this.leftOffsetY = loaded.leftOffsetY;

        this.rightOffsetX = loaded.rightOffsetX;
        this.rightOffsetY = loaded.rightOffsetY;

        this.leftRotation = loaded.leftRotation;
        this.rightRotation = loaded.rightRotation;

        this.leftScale = loaded.leftScale;
        this.rightScale = loaded.rightScale;

        this.leftAdditionalLines = loaded.leftAdditionalLines;
        this.rightAdditionalLines = loaded.rightAdditionalLines;

        this.synchronizedPoints = loaded.synchronizedPoints;
        if(this.synchronizedPoints.isEmpty()) {
            // Always keep the origin as a synchronized point
            synchronizedPoints.add(new SyncedPoints(new Point(0, 0), new Point(0, 0)));
        }
        this.cutOrder = findFittingSynchronizedPoints(loaded.cutOrder);

        this.theme = loaded.theme;

        this.arrangeLockedDown = loaded.arrangeLockedDown;
    }

    /**
     * Saves state to file
     * @param path File path
     * @throws JAXBException If JAXB error
     */
    public void saveTo(String path) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(StateManager.class);
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(this, new File(path));
    }

    /**
     * Gets the offset X for the given side
     * @param side The side
     * @return Offset X
     */
    public double getOffsetX(Side side) {
        if(side == Side.LEFT) {
            return getLeftOffsetX();
        } else {
            return getRightOffsetX();
        }
    }

    /**
     * Sets the offset X for the given side
     * @param side The side
     * @param value The value
     */
    public void setOffsetX(Side side, double value) {
        if(side == Side.LEFT) {
            setLeftOffsetX(value);
        } else {
            setRightOffsetX(value);
        }
    }

    /**
     * Gets the offset Y for the given side
     * @param side The side
     * @return Offset Y
     */
    public double getOffsetY(Side side) {
        if(side == Side.LEFT) {
            return getLeftOffsetY();
        } else {
            return getRightOffsetY();
        }
    }

    /**
     * Sets the offset Y for the given side
     * @param side The side
     * @param value The value
     */
    public void setOffsetY(Side side, double value) {
        if(side == Side.LEFT) {
            setLeftOffsetY(value);
        } else {
            setRightOffsetY(value);
        }
    }

    /**
     * Gets the rotation for the given side
     * @param side The side
     * @return Rotation in degrees
     */
    public double getRotation(Side side) {
        if(side == Side.LEFT) {
            return getLeftRotation();
        } else {
            return getRightRotation();
        }
    }

    /**
     * Sets the rotation for the given side
     * @param side The side
     * @param value The rotation in degrees
     */
    public void setRotation(Side side, double value) {
        if(side == Side.LEFT) {
            setLeftRotation(value);
        } else {
            setRightRotation(value);
        }
    }

    /**
     * Gets the scale factor for the given side
     * @param side The side
     * @return Scale factor
     */
    public double getScale(Side side) {
        if(side == Side.LEFT) {
            return getLeftScale();
        } else {
            return getRightScale();
        }
    }

    /**
     * Sets the scale factor for the given side
     * @param side The side
     * @param value The scale factor
     */
    public void setScale(Side side, double value) {
        if(side == Side.LEFT) {
            setLeftScale(value);
        } else {
            setRightScale(value);
        }
    }

    /**
     * Adds an additional line to the given side
     * @param side The side
     * @param line The line to add
     */
    public void addAdditionalLine(Side side, Line line) {
        if(side == Side.LEFT) {
            leftAdditionalLines.add(line);
        } else {
            rightAdditionalLines.add(line);
        }
    }

    /**
     * Removes an additional line from a given side
     * @param side Left/Right
     * @param line The line to remove
     */
    public void removeAdditionalLine(Side side, Line line) {
        if(side == Side.LEFT) {
            leftAdditionalLines.remove(line);
        } else {
            rightAdditionalLines.remove(line);
        }
    }

    /**
     * Adds a synchronized point
     * @param sp The synchronized point
     */
    public void addSynchronizedPoint(SyncedPoints sp) {
        if(sp.getLeft().getX() == 0 
            && sp.getLeft().getY() == 0
            && sp.getRight().getX() == 0
            && sp.getRight().getY() == 0) {
            // Never add another origin sync point
            return;
        }
        synchronizedPoints.add(sp);
    }

    /**
     * Removes a pair of synchronized points, also from the cut order if present
     * @param sp The pair of synchronized points
     */
    @SuppressWarnings("PMD.EmptyControlStatement")
    public void removeSyncPoint(SyncedPoints sp) {
        if(sp == null) {
            return;
        }
        if(sp.getLeft().getX() == 0 
            && sp.getLeft().getY() == 0
            && sp.getRight().getX() == 0
            && sp.getRight().getY() == 0) {
            // Never remove the origin sync point
            return;
        }
        synchronizedPoints.remove(sp);
        while(cutOrder.remove(sp)) {
            // remove returns false as soon as it can not remove any more sp
        }
    }

    private List<SyncedPoints> findFittingSynchronizedPoints(List<SyncedPoints> co) {
        List<SyncedPoints> list = new ArrayList<>();
        for(SyncedPoints p : co) {
            int index = this.synchronizedPoints.indexOf(p);
            if(index < 0) {
                throw new UserErrorException("Error loading cut order points from file.\nSave file corrupted.", null);
            }
            list.add(this.synchronizedPoints.get(index));
        }
        return list;
    }

    /**
     * Add a point to the end of the cut order
     * @param sp The point to add
     */
    public void addCutOrderPoint(SyncedPoints sp) {
        cutOrder.add(sp);
    }

    /**
     * Remove a point from the cut order
     * @param index Index of the point to remove
     */
    public void removeCutOrderPoint(int index) {
        cutOrder.remove(index);
    }

    /**
     * Move a point up within the cut order
     * @param index Index of the point to move up
     */
    public void moveUpCutOrderPoint(int index) {
        if(index > 0) {
            SyncedPoints point = cutOrder.get(index);
            cutOrder.remove(index);
            cutOrder.add(index-1, point);
        }
    }

    /**
     * Move a point down within the cut order
     * @param index Index of the point to move down
     */
    public void moveDownCutOrderPoint(int index) {
        if(index >= 0 && index < cutOrder.size()-1) {
            SyncedPoints point = cutOrder.get(index);
            cutOrder.remove(index);
            cutOrder.add(index+1, point);
        }
    }

    /**
     * Get all additional lines for a selected side
     * @param side Left/Right
     * @return The lines added to that side
     */
    public List<Line> getAdditionalLines(Side side) {
        List<Line> list;
        if(side == Side.LEFT) {
            list = leftAdditionalLines;
        } else {
            list = rightAdditionalLines;
        }

        return list.stream().map(Line::copy).collect(Collectors.toList());
    }

    /**
     * Updates offset for the given side and moves all associated additional lines and sync points.
     * Points at (0,0) are not moved (origin is preserved), other points are translated by the offset delta.
     * @param side The side
     * @param newOffsetX The new offset X
     * @param newOffsetY The new offset Y
     */
    public void updateOffsetAndMoveGeometry(Side side, double newOffsetX, double newOffsetY) {
        // Calculate delta
        double oldOffsetX = getOffsetX(side);
        double oldOffsetY = getOffsetY(side);
        double deltaX = newOffsetX - oldOffsetX;
        double deltaY = newOffsetY - oldOffsetY;

        // Update the offset
        setOffsetX(side, newOffsetX);
        setOffsetY(side, newOffsetY);

        // Move all additional lines for this side, preserving origin points
        List<Line> linesToMove = (side == Side.LEFT) ? leftAdditionalLines : rightAdditionalLines;
        for (Line line : linesToMove) {
            movePointIfNotOrigin(line.getStart(), deltaX, deltaY);
            movePointIfNotOrigin(line.getEnd(), deltaX, deltaY);
        }

        // Move all synchronized points for this side, preserving origin points
        for (SyncedPoints sp : synchronizedPoints) {
            Point pointToMove = (side == Side.LEFT) ? sp.getLeft() : sp.getRight();
            movePointIfNotOrigin(pointToMove, deltaX, deltaY);
        }
    }

    /**
     * Helper method to move a point if it's not at the origin (0,0)
     * @param point The point to move
     * @param deltaX The X offset
     * @param deltaY The Y offset
     */
    private void movePointIfNotOrigin(Point point, double deltaX, double deltaY) {
        if (point.getX() != 0 || point.getY() != 0) {
            point.offset(deltaX, deltaY);
        }
    }

    /**
     * Deletes all additional lines, synchronized points, and cut order entries.
     * Called when unlocking the arrangement.
     */
    public void deleteAllAdditionalGeometry() {
        leftAdditionalLines.clear();
        rightAdditionalLines.clear();
        synchronizedPoints.clear();
        // Always keep the origin as a synchronized point
        synchronizedPoints.add(new SyncedPoints(new Point(0, 0), new Point(0, 0)));
        cutOrder.clear();
    }
}
package de.kremerdaniel.openfoamcut.gui;

import javax.swing.*;


import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Back-Buffered outline drawing JPanel,
 * custom-tailored for OpenFoamCut. Not intended for anything else,
 * don't blame me if you break it. The API is Not Good (tm)
 * 
 * NOTE:
 * This JPanel is implicitly Serializable via Swing inheritance,
 * but all state is UI-only and intentionally non-serializable.
 *
 */
public class OutlinePanel extends JPanel {

    private static final int FOAM_BUBBLE_COUNT = 120;
    private static final double FOAM_BUBBLE_MIN_RADIUS = 1.5;
    private static final double FOAM_BUBBLE_MAX_RADIUS = 4.0;
    // Point selection render and tolerances
    private static final double POINT_RADIUS_PX = 4.0;
    private static final double POINT_PICK_RADIUS_PX = 6;
    private static final double LINE_PICK_DISTANCE_PX = 5;
    
    private transient BufferedImage backBuffer;

    // Viewport state
    private double viewScale = 1.0;
    private double viewTranslateX = 0.0;
    private double viewTranslateY = 0.0;

    // Mouse interaction
    private java.awt.Point lastMouse;

    private Color foamColor = Color.LIGHT_GRAY;
    private Color primaryColor = Color.BLACK;
    private Color secondaryColor = new Color(50, 80, 140);
    private Color additionalColor = Color.PINK;
    private Color backgroundColor = Color.WHITE;
    private Color syncPointColor = Color.BLUE;
    private Color anchorPointColor = Color.BLUE;

    private transient CutOutline mainOutline = new CutOutline();
    private transient CutOutline secondaryOutline = new CutOutline();

    private transient List<ColoredLine> mainLines = new ArrayList<>();
    private transient List<ColoredLine> secondaryLines = new ArrayList<>();
    private transient List<ColoredLine> foamLines = new ArrayList<>();
    private transient List<ColoredLine> additionalLines = new ArrayList<>();
    private transient List<Point> syncPoints = new ArrayList<>();
    private transient Point highlightSyncPoint;

    private double foamWidth;
    private double foamHeight;
    private double foamOffsetX;

    private final List<Ellipse2D.Double> foamBubbles = new ArrayList<>();
    private final Random random = new Random();

    @Setter
    private boolean flipHorizontally = false;
    private double mainOffsetX;
    private double mainOffsetY;
    private double secondaryOffsetX;
    private double secondaryOffsetY;

    private boolean pickingEnabled = false;
    private boolean freeHandAllowed = false;
    private transient final List<PointSelectionListener> pointListeners = new ArrayList<>();
    private transient final List<LineSelectionListener> lineListeners = new ArrayList<>();

    private String currentHint = "";

    private boolean drawMainCenterMarker = true;
    private boolean drawSecondaryCenterMarker = true;
    private transient Point anchorPoint;

    // Stored delays for restoring when mouse exits OutlinePanel
    private static final int STANDARD_INITIAL_DELAY = 750;
    private static final int STANDARD_RESHOW_DELAY = 500;

    /**
     * Constructor for OutlinePanel
     */
    public OutlinePanel() {
        super();
        setOpaque(true);
        setDoubleBuffered(false);

        setToolTipText(""); // Enable tooltip generation for this component

        backBuffer = new BufferedImage(
            1,
            1,
            BufferedImage.TYPE_INT_ARGB
        );

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                backBuffer = new BufferedImage(
                    getWidth(),
                    getHeight(),
                    BufferedImage.TYPE_INT_ARGB
                );
                rebuildBackBuffer();
                repaint();
            }
        });

        // Zoom / pan
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    lastMouse = e.getPoint();
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMouse != null) {
                    java.awt.Point p = e.getPoint();
                    viewTranslateX += p.x - lastMouse.x;
                    viewTranslateY += p.y - lastMouse.y;
                    lastMouse = p;

                    rebuildBackBuffer();
                    repaint();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                lastMouse = null;
            }
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(e.getPoint(), e.getPreciseWheelRotation());
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    resetView();
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    handlePicking(e, false);
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    handlePicking(e, true);
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                // When mouse enters OutlinePanel, set tooltip delay to 0 for instant display
                ToolTipManager.sharedInstance().setInitialDelay(0);
                ToolTipManager.sharedInstance().setReshowDelay(0);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                // When mouse exits OutlinePanel, restore standard tooltip delays
                ToolTipManager.sharedInstance().setInitialDelay(STANDARD_INITIAL_DELAY);
                ToolTipManager.sharedInstance().setReshowDelay(STANDARD_RESHOW_DELAY);
                ToolTipManager.sharedInstance().mouseExited(e);
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                // Let standard ToolTipManager handle tooltip display with current delays
                ToolTipManager.sharedInstance().mouseMoved(e);
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void readObject(ObjectInputStream objectInputStream)
            throws IOException, ClassNotFoundException {
        throw new NotSerializableException(getClass().getName());
    }

    private void rebuildBackBuffer() {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        Graphics2D g2 = backBuffer.createGraphics();

        // Quality settings
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Clear background
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        renderScene(g2);

        g2.dispose();
    }

    private void zoomAt(java.awt.Point mouse, double wheelRotation) {
        double zoomFactor = Math.pow(1.1, -wheelRotation);

        double oldScale = viewScale;
        viewScale *= zoomFactor;
        viewScale = Math.max(0.1, Math.min(viewScale, 50.0)); // clamp

        // Adjust translation so zoom happens around cursor
        double scaleChange = viewScale / oldScale;
        viewTranslateX = mouse.getX() - scaleChange * (mouse.getX() - viewTranslateX);
        viewTranslateY = mouse.getY() - scaleChange * (mouse.getY() - viewTranslateY);

        rebuildBackBuffer();
        repaint();
    }

    private void resetView() {
        viewScale = 1.0;
        viewTranslateX = 0.0;
        viewTranslateY = 0.0;

        rebuildBackBuffer();
        repaint();
    }

    /**
     * Sets the main outline to display
     * @param outline The cut outline to set
     */
    public void setMainOutline(CutOutline outline) {
        if(outline == null) {
            return;
        }
        mainOutline = outline.copy();
        mainLines.clear();
        addOutline(mainLines, outline, LineRole.PRIMARY);
    }

    /**
     * Sets the secondary outline to display
     * @param outline The cut outline to set
     */
    public void setSecondaryOutline(CutOutline outline) {
        if(outline == null) {
            return;
        }
        secondaryOutline = outline.copy();
        secondaryLines.clear();
        addOutline(secondaryLines, secondaryOutline, LineRole.SECONDARY);
    }

    private void addOutline(List<ColoredLine> targetList, CutOutline outline, LineRole role) {
        SwingUtilities.invokeLater(() -> {
            targetList.addAll(outline.getLines().stream().map(l -> {
                Point start = l.getStart();
                Point end   = l.getEnd();
                Line drawLine = new Line(
                    new Point(start.getX() + foamOffsetX, start.getY()),
                    new Point(end.getX() + foamOffsetX,   end.getY())
                );
                return new ColoredLine(drawLine, role);
            }).collect(Collectors.toList()));

            setBackground(backgroundColor);
            rebuildBackBuffer();
            repaint();    // trigger redraw
        });
    }

    /**
     * Sets the foam outline dimensions
     * @param width Width of the foam
     * @param height Height of the foam
     * @param offsetX X offset of the foam
     */
    public void setFoamOutline(double width, double height, double offsetX) {
        SwingUtilities.invokeLater(() -> {
            foamWidth = width;
            foamHeight = height;
            foamOffsetX = offsetX;

            // Add foam outline
            foamLines.clear();
            // UP
            Line drawLine = new Line(
                new Point(offsetX, 0),
                new Point(offsetX, height)
            );
            this.foamLines.add(new ColoredLine(drawLine, LineRole.FOAM));
            // RIGHT
            drawLine = new Line(
                new Point(offsetX, height),
                new Point(width + offsetX, height)
            );
            this.foamLines.add(new ColoredLine(drawLine, LineRole.FOAM));
            // DOWN
            drawLine = new Line(
                new Point(width + offsetX, height),
                new Point(width + offsetX, 0)
            );
            this.foamLines.add(new ColoredLine(drawLine, LineRole.FOAM));
            // LEFT
            drawLine = new Line(
                new Point(width + offsetX, 0),
                new Point(offsetX, 0)
            );
            this.foamLines.add(new ColoredLine(drawLine, LineRole.FOAM));

            // Add bubbles to foam
            foamBubbles.clear();

            for (int i = 0; i < FOAM_BUBBLE_COUNT; i++) {
                double radius = FOAM_BUBBLE_MIN_RADIUS +
                        random.nextDouble() * (FOAM_BUBBLE_MAX_RADIUS - FOAM_BUBBLE_MIN_RADIUS);

                double x = offsetX + radius +
                        random.nextDouble() * (width - 2 * radius);

                double y = radius +
                        random.nextDouble() * (height - 2 * radius);

                foamBubbles.add(new Ellipse2D.Double(
                        x - radius,
                        y - radius,
                        radius * 2,
                        radius * 2
                ));
            }  

            rebuildBackBuffer();
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.drawImage(backBuffer, 0, 0, null);
    }

    private void renderScene(Graphics2D g2) {
        // WARNING: When modifying screen transform logic, also change screenToModel(Point)
        double actualWidth = foamWidth + foamOffsetX;
        // Compute scale + translation to fit the panel (centered)
        double scaleX = getWidth() / actualWidth;
        double scaleY = getHeight() / foamHeight;
        double scale = Math.min(scaleX, scaleY) * 0.95;   // 95% to leave a small margin

        AffineTransform at = new AffineTransform();

        // 4. Viewport transform (SCREEN space)
        at.translate(viewTranslateX, viewTranslateY);
        at.scale(viewScale, viewScale);

        // 3. Center in panel (SCREEN space)
        double cx = (getWidth()  - actualWidth  * scale) / 2.0;
        double cy = (getHeight() - foamHeight * scale) / 2.0;
        at.translate(cx, cy);

        // 2. Scale to fit
        at.scale((flipHorizontally ? -1 : 1) * scale, -scale); // Flip on the Y axis (second argument), optionally flip on X axis

        // 2b. Move flipped model back into view
        at.translate(flipHorizontally ? -actualWidth : 0, -foamHeight);

        // 1. Move model to origin (MODEL space)
        at.translate(0, 0);

        AffineTransform old = g2.getTransform();
        g2.setTransform(at);

        // ─── Draw with constant **screen** thickness ───────────────
        // Undo scaling effect on stroke
        float screenStrokeWidth = 1.5f;
        Stroke originalStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(
            screenStrokeWidth / (float) scale,   // ← key line
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND
        ));
        
        // TABLE
        drawTable(g2);

        // FOAM BLOCK
        drawFoam(g2);

        // SECONDARY LINES
        drawLinesWithOffset(secondaryLines, g2, secondaryOffsetX, secondaryOffsetY);
        // draw center marker
        if(drawSecondaryCenterMarker) {
            drawMarker(secondaryOutline.getBounds().getWidth() / 2 + foamOffsetX + secondaryOffsetX,
                    secondaryOutline.getBounds().getHeight() / 2 + secondaryOffsetY,
                    secondaryColor,
                    g2);
        }
        // MAIN LINES
        drawLinesWithOffset(mainLines, g2, mainOffsetX, mainOffsetY);
        // draw center marker
        if(drawMainCenterMarker) {
            drawMarker(mainOutline.getBounds().getWidth() / 2 + foamOffsetX + mainOffsetX,
                   mainOutline.getBounds().getHeight() / 2 + mainOffsetY,
                   primaryColor,
                   g2);
        }

        // ORIGIN
        g2.setColor(Color.RED);
        g2.drawOval(-3, -3, 6, 6);

        // ADDITIONAL LINES
        drawLines(additionalLines, g2);

        // PICKING POINTS around line start/end
        if (pickingEnabled) {
            double totalScale = scale * viewScale;
            drawLineEndpointsWithOffset(mainLines, g2, mainOffsetX, mainOffsetY, totalScale);
            drawLineEndpointsWithOffset(additionalLines, g2, 0, 0, totalScale);
        }

        // SYNCHRONIZED POINTS
        drawSyncPointMarker(g2);

        // DRAW CURRENT ANCHOR POINT IF SET
        drawAnchorPoint(g2);

        g2.setStroke(originalStroke);
        g2.setTransform(old);
    }

    private void drawAnchorPoint(Graphics2D g2) {
        if(this.anchorPoint != null) {
            final int radius = 2;
            g2.setColor(resolveColor(LineRole.SYNC_POINT));
            Ellipse2D.Double marker = new Ellipse2D.Double(
                    this.anchorPoint.getX() - radius,
                    this.anchorPoint.getY() - radius,
                    radius * 2,
                    radius * 2
            );
            g2.draw(marker);
        }
    }

    private void drawSyncPointMarker(Graphics2D g2) {
        final int radius = 2;
        for (Point p : syncPoints) {
            g2.setColor(resolveColor(LineRole.SYNC_POINT));
            Ellipse2D.Double marker = new Ellipse2D.Double(
                    p.getX() - radius,
                    p.getY() - radius,
                    radius * 2,
                    radius * 2
            );
            if(p.equals(highlightSyncPoint)) {
                g2.fill(marker);
            } else {
                g2.draw(marker);
            }
        }
    }

    private void drawLines(List<ColoredLine> lines, Graphics2D g2) {
        for (ColoredLine line : lines) {
            g2.setColor(resolveColor(line.getRole()));
            g2.draw(new Line2D.Double(
                line.getLine().getStart().getX(),
                line.getLine().getStart().getY(),
                line.getLine().getEnd().getX(),
                line.getLine().getEnd().getY()
            ));
        }
    }

    private void drawLinesWithOffset(List<ColoredLine> lines, Graphics2D g2, double x, double y) {
        AffineTransform old = g2.getTransform(); // save
        g2.translate(x, y);

        for (ColoredLine line : lines) {
            g2.setColor(resolveColor(line.getRole()));
            g2.draw(new Line2D.Double(
                line.getLine().getStart().getX(),
                line.getLine().getStart().getY(),
                line.getLine().getEnd().getX(),
                line.getLine().getEnd().getY()
            ));
        }

        g2.setTransform(old); // restore
    }

    private void drawFoam(Graphics2D g2) {
        drawLines(foamLines, g2);

        g2.setColor(foamColor);
        for (Ellipse2D bubble : foamBubbles) {
            g2.fill(bubble);
        }
    }

    private void drawMarker(double x, double y, Color color, Graphics2D g2) {
        final int markerHalfLength = 3;
        g2.setColor(color);

        // horizontal line
        g2.draw(new Line2D.Double(
                x - markerHalfLength, y,
                x + markerHalfLength, y
        ));

        // vertical line
        g2.draw(new Line2D.Double(
                x, y - markerHalfLength,
                x, y + markerHalfLength
        ));

    }

    private void drawLineEndpointsWithOffset(
            List<ColoredLine> lines,
            Graphics2D g2,
            double ox,
            double oy,
            double totalScale
    ) {
        AffineTransform old = g2.getTransform();
        g2.translate(ox, oy);

        g2.setColor(Color.RED);
        for (ColoredLine cl : lines) {
            Line l = cl.getLine();
            drawPoint(l.getStart().getX(), l.getStart().getY(), totalScale, g2);
            drawPoint(l.getEnd().getX(), l.getEnd().getY(), totalScale, g2);
        }

        g2.setTransform(old);
    }

    private void drawPoint(double x, double y, double totalScale, Graphics2D g2) {
        double r = POINT_RADIUS_PX / totalScale;

        g2.fill(new Ellipse2D.Double(
            x - r,
            y - r,
            r * 2,
            r * 2
        ));
    }

    private void drawTable(Graphics2D g2) {
        final int tableStart = -50;
        final int tableY = -1;
        int tableEnd = (int) (foamWidth + foamOffsetX + 50);
        g2.setColor(Color.ORANGE);
        g2.draw(new Line2D.Double(tableStart, tableY, tableEnd, tableY)); // Table top

        // Diagonal lines under table
        final int step = 25;
        for(int currentPos = tableStart; currentPos < tableEnd; currentPos += step) {
            g2.draw(new Line2D.Double(currentPos, tableY, currentPos-step, -step + tableY)); // Table top
        }
    }

    @Data
    @AllArgsConstructor
    private static class ColoredLine {
        Line line;
        LineRole role;
    }
    private enum LineRole {
        PRIMARY,
        SECONDARY,
        ADDITIONAL,
        FOAM,
        SYNC_POINT,
        ANCHOR_POINT
    }
    private Color resolveColor(LineRole role) {
        return switch (role) {
            case PRIMARY   -> primaryColor;
            case SECONDARY -> secondaryColor;
            case ADDITIONAL -> additionalColor;
            case FOAM      -> foamColor;
            case SYNC_POINT -> syncPointColor;
            case ANCHOR_POINT -> anchorPointColor;
        };
    }

    /**
     * Returns the preferred size of the panel
     * @return The preferred dimension
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 300);
    }

    /**
     * Sets the offset for the main outline
     * @param x X offset
     * @param y Y offset
     */
    public void setMainOutlineOffset(double x, double y) {
        SwingUtilities.invokeLater(() -> {
            this.mainOffsetX = x;
            this.mainOffsetY = y;

            rebuildBackBuffer();
            repaint();
        });
    }

    /**
     * Sets the offset for the secondary outline
     * @param x X offset
     * @param y Y offset
     */
    public void setSecondaryOutlineOffset(double x, double y) {
        SwingUtilities.invokeLater(() -> {
            this.secondaryOffsetX = x;
            this.secondaryOffsetY = y;

            rebuildBackBuffer();
            repaint();
        });
    }

    /**
     * Changes the theme of the panel
     * @param theme The new theme
     */
    public void changeTheme(Theme theme) {
        switch (theme) {
            case DARK -> {
                backgroundColor = new Color(43, 43, 43);
                foamColor = new Color(70, 70, 70);
                primaryColor = Color.WHITE;
                secondaryColor = new Color(100, 140, 220);
            }
            case LIGHT -> {
                backgroundColor = Color.WHITE;
                foamColor = Color.LIGHT_GRAY;
                primaryColor = Color.BLACK;
                secondaryColor = new Color(50, 80, 140);
            }
        }

        setBackground(backgroundColor);

        rebuildBackBuffer();
        repaint();
    }

    /**
     * Listener for point selection events
     */
    public interface PointSelectionListener {
        /**
         * Called when a point is selected
         * @param x X coordinate
         * @param y Y coordinate
         */
        void pointSelected(double x, double y);
    }

    /**
     * Listener for line selection events
     */
    public interface LineSelectionListener {
        /**
         * Called when a line is selected
         * @param line The selected line
         */
        void lineSelected(Line line);
    }

    /**
     * Adds a point selection listener
     * @param l The listener to add
     */
    public void addPointSelectionListener(PointSelectionListener l) {
        pointListeners.add(l);
    }

    /**
     * Adds a line selection listener
     * @param l The listener to add
     */
    public void addLineSelectionListener(LineSelectionListener l) {
        lineListeners.add(l);
    }

    private Point2D screenToModel(java.awt.Point screenPoint) {
        try {
            AffineTransform at = new AffineTransform();

            double actualWidth = foamWidth + foamOffsetX;
            double scaleX = getWidth() / actualWidth;
            double scaleY = getHeight() / foamHeight;
            double scale = Math.min(scaleX, scaleY) * 0.95;

            double cx = (getWidth()  - actualWidth * scale) / 2.0;
            double cy = (getHeight() - foamHeight   * scale) / 2.0;

            at.translate(viewTranslateX, viewTranslateY);
            at.scale(viewScale, viewScale);
            at.translate(cx, cy);
            at.scale((flipHorizontally ? -1 : 1) * scale, -scale);
            at.translate(flipHorizontally ? -actualWidth : 0, -foamHeight);

            return at.createInverse().transform(screenPoint, null);
        } catch (NoninvertibleTransformException ex) {
            return null;
        }
    }

    private double pixelToModel(double px) {
        return px / viewScale; // model space before render scaling
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private void handlePicking(MouseEvent e, boolean free) {
        if (!pickingEnabled || !this.isEnabled()) {
            return;
        }

        Point2D model = screenToModel(e.getPoint());
        if (model == null) {
            return;
        }

        // Convert into main-outline local space
        double mx = model.getX() - mainOffsetX;
        double my = model.getY() - mainOffsetY;
        Point local = new Point(mx, my);

        double pointTol = pixelToModel(POINT_PICK_RADIUS_PX);
        double lineTol  = pixelToModel(LINE_PICK_DISTANCE_PX);

        // Always allow picking origin (0,0)
        if (local.distanceTo(-mainOffsetX, -mainOffsetY) <= pointTol) {
            pointListeners.forEach(li -> li.pointSelected(0.0, 0.0));
            return;
        }

        // Point picking
        for (ColoredLine cl : mainLines) {
            Line l = cl.getLine();

            if (local.distanceTo(l.getStart()) <= pointTol) {
                pointListeners.forEach(li -> li.pointSelected(l.getStart().getX() + mainOffsetX, l.getStart().getY() + mainOffsetY));
                return;
            }
            if (local.distanceTo(l.getEnd()) <= pointTol) {
                pointListeners.forEach(li -> li.pointSelected(l.getEnd().getX() + mainOffsetX, l.getEnd().getY() + mainOffsetY));
                return;
            }
        }
        for (ColoredLine cl : additionalLines) {
            Line l = cl.getLine();

            if (local.distanceTo(l.getStart().getX() - mainOffsetX, l.getStart().getY() - mainOffsetY) <= pointTol) {
                pointListeners.forEach(li -> li.pointSelected(l.getStart().getX(), l.getStart().getY()));
                return;
            }
            if (local.distanceTo(l.getEnd().getX() - mainOffsetX, l.getEnd().getY() - mainOffsetY) <= pointTol) {
                pointListeners.forEach(li -> li.pointSelected(l.getEnd().getX(), l.getEnd().getY()));
                return;
            }
        }

        // Line picking
        for (ColoredLine cl : mainLines) {
            if (cl.getLine().ptSegDist(local) <= lineTol) {
                lineListeners.forEach(li -> li.lineSelected(cl.getLine()));
                return;
            }
        }

        // Free point picking
        if (free && freeHandAllowed) {
            pointListeners.forEach(li ->
                li.pointSelected(
                    local.getX() + mainOffsetX,
                    local.getY() + mainOffsetY
                )
            );
        }
    }

    /**
     * Enables or disables picking mode
     * @param enabled True to enable picking
     * @param freeHandAllowed True to allow free hand selection
     */
    public void setPickingEnabled(boolean enabled, boolean freeHandAllowed) {
        setPickingEnabled(enabled, freeHandAllowed, "");
    }

    /**
     * Enables or disables picking mode with an optional hint message
     * @param enabled True to enable picking
     * @param freeHandAllowed True to allow free hand selection
     * @param hint Hint message to display to the user (e.g., "Select start point")
     */
    public void setPickingEnabled(boolean enabled, boolean freeHandAllowed, String hint) {
        this.pickingEnabled = enabled;
        this.freeHandAllowed = freeHandAllowed;
        this.currentHint = enabled ? hint : "";
        rebuildBackBuffer();
        repaint();
    }

    /**
     * Provides dynamic tooltip text based on mouse event
     * Called by ToolTipManager to display cursor-following tooltips with coordinates
     * @param event Mouse event
     * @return Hint text combined with coordinates, or just coordinates if not picking
     */
    @Override
    public String getToolTipText(MouseEvent event) {
        Point2D model = screenToModel(event.getPoint());
        if (model == null) {
            return null;
        }
        
        double x = model.getX();
        double y = model.getY();
        String coordinates = String.format("x = %.2f, y = %.2f", x, y);
        
        if (pickingEnabled && !currentHint.isEmpty()) {
            return currentHint + " | " + coordinates;
        }
        return coordinates;
    }

    /**
     * Sets the additional lines to display
     * @param lines List of lines to set
     */
    public void setAdditionalLines(List<Line> lines) {
        additionalLines.clear();
        for(Line l : lines) {
            ColoredLine cLine = new ColoredLine(l, LineRole.ADDITIONAL);
            additionalLines.add(cLine);
        }
        rebuildBackBuffer();
        repaint();    // trigger redraw
    }

    /**
     * Sets the sync points to display
     * @param syncPoints List of sync points
     */
    public void setSyncPoints(List<Point> syncPoints) {
        this.syncPoints = syncPoints.stream().map(Point::copy).collect(Collectors.toList());
        rebuildBackBuffer();
        repaint();    // trigger redraw
    }

    /**
     * Sets the sync point to highlight
     * @param p The point to highlight
     */
    public void setHighlightSyncPoint(Point p) {
        this.highlightSyncPoint = p.copy();
        rebuildBackBuffer();
        repaint();    // trigger redraw
    }

    /**
     * Sets whether to draw center markers for main and secondary outlines
     * @param main True to draw main center marker
     * @param secondary True to draw secondary center marker
     */
    public void setDrawCenterMarkers(boolean main, boolean secondary) {
        this.drawMainCenterMarker = main;
        this.drawSecondaryCenterMarker = secondary;
        rebuildBackBuffer();
        repaint();
    }

    /**
     * Sets the anchor point
     * @param anchorPoint The anchor point
     */
    public void setAnchorPoint(Point anchorPoint) {
        this.anchorPoint = anchorPoint.copy();
    }
}
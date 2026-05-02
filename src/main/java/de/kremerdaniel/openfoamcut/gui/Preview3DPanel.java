package de.kremerdaniel.openfoamcut.gui;

import de.kremerdaniel.openfoamcut.controller.Preview3DController;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.preview.Preview3DScene;
import lombok.Getter;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Panel for the interactive 3D preview.
 */
@SuppressWarnings({"PMD.SingularField", "PMD.TooManyMethods"})
public class Preview3DPanel {

    private static final String CARD_CANVAS = "canvas";
    private static final String CARD_ERROR = "error";

    @Getter
    private JPanel panel;

    private JComboBox<RenderMode> modeSelector;
    private JSpinner meshStepSpinner;
    private PreviewCanvas previewCanvas;
    private JTextArea errorTextArea;
    private JPanel contentPanel;
    private CardLayout contentLayout;

    /**
     * Constructor.
     */
    public Preview3DPanel() {
        initializeUi();
    }

    /**
     * Displays a generated preview scene.
     * @param scene Scene to display
     */
    public void displayScene(Preview3DScene scene) {
        SwingUtilities.invokeLater(() -> {
            previewCanvas.setScene(scene);
            errorTextArea.setText("");
            contentLayout.show(contentPanel, CARD_CANVAS);
        });
    }

    /**
     * Displays an error instead of the preview.
     * @param message Error message
     */
    public void displayError(String message) {
        SwingUtilities.invokeLater(() -> {
            previewCanvas.setScene(null);
            errorTextArea.setText(message);
            contentLayout.show(contentPanel, CARD_ERROR);
        });
    }

    /**
     * Applies the selected theme.
     * @param theme Theme to apply
     */
    public void changeTheme(Theme theme) {
        previewCanvas.setTheme(theme);

        Color panelBackground = theme == Theme.DARK ? new Color(43, 43, 43) : new Color(245, 245, 245);
        Color foreground = theme == Theme.DARK ? new Color(225, 225, 225) : new Color(35, 35, 35);
        Color border = theme == Theme.DARK ? new Color(90, 90, 90) : new Color(180, 180, 180);

        panel.setBackground(panelBackground);
        contentPanel.setBackground(panelBackground);
        errorTextArea.setBackground(panelBackground);
        errorTextArea.setForeground(new Color(190, 60, 60));
        errorTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        modeSelector.setBackground(panelBackground);
        modeSelector.setForeground(foreground);
        meshStepSpinner.getEditor().getComponent(0).setBackground(panelBackground);
        meshStepSpinner.getEditor().getComponent(0).setForeground(foreground);
    }

    /**
     * Returns the selected mesh sampling step.
     * @return Mesh sampling step in model units
     */
    public double getMeshStep() {
        return ((Number) meshStepSpinner.getValue()).doubleValue();
    }

    private void initializeUi() {
        panel = new JPanel(new BorderLayout(8, 8));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JLabel label = new JLabel("Geometry:", SwingConstants.LEFT);
        modeSelector = new JComboBox<>(RenderMode.values());
        modeSelector.addActionListener(actionEvent ->
            previewCanvas.setRenderMode((RenderMode) modeSelector.getSelectedItem())
        );
        JLabel meshStepLabel = new JLabel("Mesh step:", SwingConstants.LEFT);
        meshStepSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 100.0, 0.5));
        meshStepSpinner.addChangeListener(changeEvent -> Preview3DController.getInstance().refreshPreview());
        toolbar.add(label);
        toolbar.add(modeSelector);
        toolbar.add(meshStepLabel);
        toolbar.add(meshStepSpinner);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        previewCanvas = new PreviewCanvas();
        previewCanvas.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        contentPanel.add(previewCanvas, CARD_CANVAS);

        errorTextArea = new JTextArea();
        errorTextArea.setEditable(false);
        errorTextArea.setLineWrap(true);
        errorTextArea.setWrapStyleWord(true);
        errorTextArea.setOpaque(true);
        JScrollPane errorScrollPane = new JScrollPane(errorTextArea);
        errorScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        contentPanel.add(errorScrollPane, CARD_ERROR);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        changeTheme(Theme.DARK);
        contentLayout.show(contentPanel, CARD_ERROR);
        errorTextArea.setText("Open this tab with a valid cut configuration to render the 3D preview.");
    }

    private enum RenderMode {
        APPROXIMATE_MESH("Approximate Mesh"),
        CLOSED_SOLID("Closed Solid");

        private final String label;

        RenderMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class PreviewCanvas extends JPanel {

        private static final double MIN_PITCH = Math.toRadians(-85);
        private static final double MAX_PITCH = Math.toRadians(85);

        private transient Preview3DScene scene;
        private RenderMode renderMode = RenderMode.APPROXIMATE_MESH;
        private Theme theme = Theme.DARK;

        private double yaw;
        private double pitch;
        private double zoom;
        private Point lastMouse;

        private PreviewCanvas() {
            setPreferredSize(new Dimension(700, 500));
            setOpaque(true);
            resetView();

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        lastMouse = e.getPoint();
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (lastMouse == null) {
                        return;
                    }

                    Point current = e.getPoint();
                    yaw += (current.x - lastMouse.x) * 0.005;
                    pitch += (current.y - lastMouse.y) * 0.005;
                    pitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
                    lastMouse = current;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    lastMouse = null;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        resetView();
                    }
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    zoom *= Math.pow(1.1, -e.getPreciseWheelRotation());
                    zoom = Math.max(0.25, Math.min(zoom, 8.0));
                    repaint();
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            addMouseWheelListener(mouseAdapter);
        }

        private void setScene(Preview3DScene scene) {
            this.scene = scene;
            if (scene != null) {
                resetView();
            } else {
                repaint();
            }
        }

        private void setRenderMode(RenderMode renderMode) {
            this.renderMode = renderMode;
            repaint();
        }

        private void setTheme(Theme theme) {
            this.theme = theme;
            setBackground(theme == Theme.DARK ? new Color(30, 30, 30) : new Color(252, 252, 252));
            repaint();
        }

        private void resetView() {
            yaw = Math.toRadians(-35);
            pitch = Math.toRadians(28);
            zoom = 1.0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (scene == null) {
                g2.dispose();
                return;
            }

            ViewState viewState = new ViewState(scene.getBounds(), getWidth(), getHeight(), yaw, pitch, zoom);

            if (renderMode == RenderMode.CLOSED_SOLID) {
                paintSolid(g2, viewState);
            }

            paintEdges(g2, scene.getTableEdges(), viewState, tableColor());
            paintEdges(g2, scene.getFoamEdges(), viewState, foamOutlineColor());
            if (renderMode == RenderMode.APPROXIMATE_MESH) {
                paintEdges(g2, scene.getMeshEdges(), viewState, cutEdgeColor());
            }

            g2.dispose();
        }

        private void paintSolid(Graphics2D g2, ViewState viewState) {
            List<PaintedTriangle> triangles = new ArrayList<>();
            for (Preview3DScene.Triangle3D triangle : scene.getSolidFaces()) {
                ProjectedPoint a = project(triangle.getA(), viewState);
                ProjectedPoint b = project(triangle.getB(), viewState);
                ProjectedPoint c = project(triangle.getC(), viewState);
                triangles.add(new PaintedTriangle(a, b, c, triangle.getSurfaceType()));
            }

            triangles.sort(Comparator.comparingDouble(PaintedTriangle::averageDepth));
            for (PaintedTriangle triangle : triangles) {
                Path2D path = new Path2D.Double();
                path.moveTo(triangle.a.screenX, triangle.a.screenY);
                path.lineTo(triangle.b.screenX, triangle.b.screenY);
                path.lineTo(triangle.c.screenX, triangle.c.screenY);
                path.closePath();

                g2.setColor(shadedColor(triangle));
                g2.fill(path);
            }
        }

        private void paintEdges(Graphics2D g2,
                                List<Preview3DScene.Edge3D> edges,
                                ViewState viewState,
                                Color color) {
            Stroke originalStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.6f));
            g2.setColor(color);

            for (Preview3DScene.Edge3D edge : edges) {
                ProjectedPoint start = project(edge.getStart(), viewState);
                ProjectedPoint end = project(edge.getEnd(), viewState);
                g2.drawLine(
                    (int) Math.round(start.screenX),
                    (int) Math.round(start.screenY),
                    (int) Math.round(end.screenX),
                    (int) Math.round(end.screenY)
                );
            }

            g2.setStroke(originalStroke);
        }

        private ProjectedPoint project(Preview3DScene.Vec3 point, ViewState viewState) {
            double centeredX = point.getX() - viewState.centerX;
            double centeredY = point.getY() - viewState.centerY;
            double centeredZ = point.getZ() - viewState.centerZ;

            double yawCos = Math.cos(viewState.yaw);
            double yawSin = Math.sin(viewState.yaw);
            double x1 = centeredX * yawCos - centeredY * yawSin;
            double y1 = centeredX * yawSin + centeredY * yawCos;
            double z1 = centeredZ;

            double pitchCos = Math.cos(viewState.pitch);
            double pitchSin = Math.sin(viewState.pitch);
            double x2 = x1;
            double y2 = y1 * pitchCos - z1 * pitchSin;
            double z2 = y1 * pitchSin + z1 * pitchCos;

            double screenX = viewState.panelWidth / 2.0 + x2 * viewState.scale;
            double screenY = viewState.panelHeight / 2.0 - z2 * viewState.scale;
            return new ProjectedPoint(screenX, screenY, x2, y2, z2);
        }

        private Color shadedColor(PaintedTriangle triangle) {
            Color baseColor = switch (triangle.surfaceType) {
                case SIDE -> theme == Theme.DARK ? new Color(110, 160, 210) : new Color(120, 165, 215);
                case LEFT_CAP -> theme == Theme.DARK ? new Color(140, 200, 170) : new Color(150, 205, 175);
                case RIGHT_CAP -> theme == Theme.DARK ? new Color(220, 170, 120) : new Color(225, 180, 130);
            };

            double[] u = new double[]{
                triangle.b.worldX - triangle.a.worldX,
                triangle.b.worldY - triangle.a.worldY,
                triangle.b.worldZ - triangle.a.worldZ
            };
            double[] v = new double[]{
                triangle.c.worldX - triangle.a.worldX,
                triangle.c.worldY - triangle.a.worldY,
                triangle.c.worldZ - triangle.a.worldZ
            };
            double nx = u[1] * v[2] - u[2] * v[1];
            double ny = u[2] * v[0] - u[0] * v[2];
            double nz = u[0] * v[1] - u[1] * v[0];
            double norm = Math.sqrt(nx * nx + ny * ny + nz * nz);

            double lightFactor = 0.65;
            if (norm > 0) {
                nx /= norm;
                ny /= norm;
                nz /= norm;
                lightFactor = Math.max(0.25, 0.45 + 0.55 * Math.abs(nx * 0.4 + ny * -0.7 + nz * 0.6));
            }

            return scaleColor(baseColor, lightFactor);
        }

        private Color scaleColor(Color color, double factor) {
            int red = (int) Math.min(255, Math.round(color.getRed() * factor));
            int green = (int) Math.min(255, Math.round(color.getGreen() * factor));
            int blue = (int) Math.min(255, Math.round(color.getBlue() * factor));
            return new Color(red, green, blue, 210);
        }

        private Color tableColor() {
            return theme == Theme.DARK ? new Color(196, 144, 78) : new Color(168, 118, 50);
        }

        private Color foamOutlineColor() {
            return theme == Theme.DARK ? new Color(210, 210, 210) : new Color(90, 90, 90);
        }

        private Color cutEdgeColor() {
            return theme == Theme.DARK ? new Color(126, 196, 245) : new Color(38, 109, 168);
        }
    }

    private static final class ViewState {
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final double scale;
        private final double yaw;
        private final double pitch;
        private final int panelWidth;
        private final int panelHeight;

        private ViewState(Preview3DScene.Bounds3D bounds,
                          int panelWidth,
                          int panelHeight,
                          double yaw,
                          double pitch,
                          double zoom) {
            this.centerX = bounds.getCenterX();
            this.centerY = bounds.getCenterY();
            this.centerZ = bounds.getCenterZ();
            this.yaw = yaw;
            this.pitch = pitch;
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;

            double maxSpan = Math.max(bounds.getSpanX(), Math.max(bounds.getSpanY(), bounds.getSpanZ()));
            if (maxSpan < 1.0) {
                maxSpan = 1.0;
            }
            this.scale = 0.72 * Math.min(panelWidth, panelHeight) / maxSpan * zoom;
        }
    }

    private static final class ProjectedPoint {
        private final double screenX;
        private final double screenY;
        private final double worldX;
        private final double worldY;
        private final double worldZ;

        private ProjectedPoint(double screenX, double screenY, double worldX, double worldY, double worldZ) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
        }
    }

    private static final class PaintedTriangle {
        private final ProjectedPoint a;
        private final ProjectedPoint b;
        private final ProjectedPoint c;
        private final Preview3DScene.SurfaceType surfaceType;

        private PaintedTriangle(ProjectedPoint a,
                                ProjectedPoint b,
                                ProjectedPoint c,
                                Preview3DScene.SurfaceType surfaceType) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.surfaceType = surfaceType;
        }

        private double averageDepth() {
            return (a.worldY + b.worldY + c.worldY) / 3.0;
        }
    }
}
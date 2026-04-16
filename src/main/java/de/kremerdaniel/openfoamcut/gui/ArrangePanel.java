package de.kremerdaniel.openfoamcut.gui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import de.kremerdaniel.openfoamcut.bo.Bounds;
import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.controller.ArrangeController;
import de.kremerdaniel.openfoamcut.controller.GlobalController;
import de.kremerdaniel.openfoamcut.gui.helper.GuiHelper;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Objects;

/**
 * Panel to load outline files, enter foam parameters and move outlines around
 */
@SuppressWarnings({"PMD.SingularField"})
public class ArrangePanel {

    private ArrangeController controller = ArrangeController.getInstance(GlobalController.getInstance());

    @Getter
    private JPanel panel;
    private JTextField tfFileInputLeft;
    private JTextField tfFileInputRight;
    private JButton bSelectLeftOutline;
    private JButton bSelectRightOutline;
    private JButton bSwapLeftRight;
    private JPanel leftDisplayPanel;
    private JPanel rightDisplayPanel;
    private JTextField tfFoamHeight;
    private JTextField tfFoamWidth;
    private JTextField tfFoamDepth;
    private JTextField tfLeftOutlineWidth;
    private JTextField tfRightOutlineWidth;
    private JTextField tfLeftOutlineHeight;
    private JTextField tfRightOutlineHeight;
    private JLabel lOutlineFitFoamMessage;
    private JTextField tfFoamXoffset;
    private JTextField tfFoamLeftOffset;
    private JTextField tfFoamRightOffset;
    private JPanel panelMoveLeftOutline;
    private JPanel panelMoveRightOutline;
    private JTextField tfFrontEdgeAngle;
    private JTextField tfBackEdgeAngle;
    private JTextField tfLeftRotation;
    private JTextField tfRightRotation;
    private JTextField tfLeftScale;
    private JTextField tfRightScale;

    private MoveOutlinePanel moveLeftOutlineActual;
    private MoveOutlinePanel moveRightOutlineActual;

    private File lastDirectory;

    /**
     * Creates a new, fully initialized ArrangedPanel
     */
    public ArrangePanel() {
        initializeUI();
        bSelectLeftOutline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openFileChooser(Side.LEFT);
            }
        });
        bSelectRightOutline.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                openFileChooser(Side.RIGHT);
            }
        });
        bSwapLeftRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.triggerSwapLeftAndRight();
            }
        });

        FocusAdapter foamChanged = getFoamFieldsFocusAdapter();
        tfFoamHeight.addFocusListener(foamChanged);
        tfFoamWidth.addFocusListener(foamChanged);
        tfFoamDepth.addFocusListener(foamChanged);
        tfFoamXoffset.addFocusListener(foamChanged);
        tfFoamLeftOffset.addFocusListener(foamChanged);
        tfFoamRightOffset.addFocusListener(foamChanged);

        FocusAdapter rotationChanged = getRotationFieldsFocusAdapter();
        tfLeftRotation.addFocusListener(rotationChanged);
        tfRightRotation.addFocusListener(rotationChanged);

        FocusAdapter scaleChanged = getScaleFieldsFocusAdapter();
        tfLeftScale.addFocusListener(scaleChanged);
        tfRightScale.addFocusListener(scaleChanged);

        moveLeftOutlineActual.setMoveOutlineActionListener(new MoveOutlineActionListener() {
            @Override
            public void actionPerformed(MoveOutlineAction action, MoveOutlinePanel panel) {
                controller.triggerMoveOutline(Side.LEFT, action, panel.getTfOffsetX().getText(), panel.getTfOffsetY().getText());
            }
        });
        moveRightOutlineActual.setMoveOutlineActionListener(new MoveOutlineActionListener() {
            @Override
            public void actionPerformed(MoveOutlineAction action, MoveOutlinePanel panel) {
                controller.triggerMoveOutline(Side.RIGHT, action, panel.getTfOffsetX().getText(), panel.getTfOffsetY().getText());
            }
        });
    }

    private FocusAdapter getFoamFieldsFocusAdapter() {
        return new FocusAdapter() {

            private String oldValue;

            @Override
            public void focusGained(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                oldValue = field.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                String newValue = field.getText();

                if (!Objects.equals(oldValue, newValue)) {
                    controller.triggerFoamChanged(
                            tfFoamDepth.getText(),
                            tfFoamHeight.getText(),
                            tfFoamWidth.getText(),
                            tfFoamXoffset.getText(),
                            tfFoamLeftOffset.getText(),
                            tfFoamRightOffset.getText()
                    );
                }
            }
        };
    }

    private FocusAdapter getRotationFieldsFocusAdapter() {
        return new FocusAdapter() {

            private String oldValue;

            @Override
            public void focusGained(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                oldValue = field.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                String newValue = field.getText();

                if (!Objects.equals(oldValue, newValue)) {
                    Side side = field.equals(tfLeftRotation) ? Side.LEFT : Side.RIGHT;
                    controller.triggerRotationChanged(side, newValue);
                }
            }
        };
    }

    private FocusAdapter getScaleFieldsFocusAdapter() {
        return new FocusAdapter() {

            private String oldValue;

            @Override
            public void focusGained(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                oldValue = field.getText();
            }

            @Override
            public void focusLost(FocusEvent e) {
                JTextField field = (JTextField) e.getComponent();
                String newValue = field.getText();

                if (!Objects.equals(oldValue, newValue)) {
                    Side side = field.equals(tfLeftScale) ? Side.LEFT : Side.RIGHT;
                    controller.triggerScaleChanged(side, newValue);
                }
            }
        };
    }

    private void openFileChooser(Side side) {
        JFileChooser chooser = (lastDirectory != null)
                ? new JFileChooser(lastDirectory)
                : new JFileChooser();

        chooser.setFileFilter(
                new FileNameExtensionFilter("Outline Files (*.dxf, *.dat)", "dxf", "dat")
        );

        if (chooser.showOpenDialog(bSelectRightOutline) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            controller.triggerSelectNewFile(side, file.getAbsolutePath());
        }
    }

    private void createUIComponents() {
        leftDisplayPanel = new OutlinePanel();
        rightDisplayPanel = new OutlinePanel();
        ((OutlinePanel) leftDisplayPanel).setFlipHorizontally(true);

        moveLeftOutlineActual = new MoveOutlinePanel();
        moveRightOutlineActual = new MoveOutlinePanel();

        panelMoveLeftOutline = moveLeftOutlineActual.getPanel();
        panelMoveRightOutline = moveRightOutlineActual.getPanel();
    }

    /**
     * Sets the actual absolute File path of selected Files to dislpay
     * @param side The side for which the path is displayed
     * @param path The path
     */
    public void setOutlineFilePath(Side side, String path) {
        if(side == Side.LEFT) {
                tfFileInputLeft.setText(path);
        } else {
                tfFileInputRight.setText(path);
        }
    }

    /**
     * Sets the foam configuration
     * @param foamDepth Depth of the foam - when standing in front of the machine, away from you
     * @param foamHeight Height of the foam when laying flat on the table
     * @param foamWidth Width of the foam from left to right side of the machine
     * @param foamOffsetX Offset of foam away from the origin
     * @param foamOffsetLeft Offset from left wire anchor point
     * @param foamOffsetRight Offset from right wire anchor point
     */
    public void setFoamConfig(double foamDepth, double foamHeight, double foamWidth, double foamOffsetX,
                              double foamOffsetLeft, double foamOffsetRight) {

        tfFoamDepth.setText(String.format("%.2f",foamDepth));
        tfFoamHeight.setText(String.format("%.2f",foamHeight));
        tfFoamWidth.setText(String.format("%.2f",foamWidth));

        tfFoamXoffset.setText(String.format("%.2f",foamOffsetX));
        tfFoamLeftOffset.setText(String.format("%.2f",foamOffsetLeft));
        tfFoamRightOffset.setText(String.format("%.2f",foamOffsetRight));

        ((OutlinePanel) leftDisplayPanel).setFoamOutline(foamDepth, foamHeight, foamOffsetX);
        ((OutlinePanel) rightDisplayPanel).setFoamOutline(foamDepth, foamHeight, foamOffsetX);
    }

    /**
     * Sets the main outline to be displayed
     * @param side Left/Right
     * @param outline The outline to display
     */
    public void setMainOutline(Side side, CutOutline outline) {
        OutlinePanel panel = null;
        JTextComponent tfHeight = null;
        JTextComponent tfWidth = null;

        if (side == Side.LEFT) {
            panel = (OutlinePanel) leftDisplayPanel;
            tfHeight = tfLeftOutlineHeight;
            tfWidth = tfLeftOutlineWidth;
        } else {
            panel = (OutlinePanel) rightDisplayPanel;
            tfHeight = tfRightOutlineHeight;
            tfWidth = tfRightOutlineWidth;
        }

        panel.setMainOutline(outline);
        Bounds b = outline != null ? outline.getBounds() : null;
        tfHeight.setText(String.format("%.2f", b != null ? b.getHeight() : 0));
        tfWidth.setText(String.format("%.2f", b != null ? b.getWidth() : 0));
    }

    /**
     * Set the secondary outline to be displayed
     * @param side Left/Right
     * @param outline The outline to display
     */
    public void setSecondaryOutline(Side side, CutOutline outline) {
        OutlinePanel panel = null;

        if (side == Side.LEFT) {
            panel = (OutlinePanel) leftDisplayPanel;
        } else {
            panel = (OutlinePanel) rightDisplayPanel;
        }

        panel.setSecondaryOutline(outline);
    }

    /**
     * Clears displayed file input errors
     * @param side Left/Right file input
     */
    public void clearFileInputError(Side side) {
        if (side == Side.LEFT) {
            tfFileInputLeft.setBackground(null);
        } else {
            tfFileInputRight.setBackground(null);
        }
    }

    /**
     * Displays file input error hints
     * @param side The side with input error
     */
    public void setFileInputError(Side side) {
        JTextField tfFileInput;
        JTextField tfHeight;
        JTextField tfWidth;
        if (side == Side.LEFT) {
            tfFileInput = tfFileInputLeft;
            tfHeight = tfLeftOutlineHeight;
            tfWidth = tfLeftOutlineWidth;
        } else {
            tfFileInput = tfFileInputRight;
            tfHeight = tfRightOutlineHeight;
            tfWidth = tfRightOutlineWidth;
        }
        tfFileInput.setBackground(Color.YELLOW);
        tfHeight.setText("");
        tfWidth.setText("");
    }

    /**
     * Displays a message about how the outlines fit the currently configured foam
     * @param message The actual message to display
     * @param success Was the fitment check successful? False if you can't tell
     * @param error Is this an error? False if you can't tell
     */
    public void displayFoamFitMessage(String message, boolean success, boolean error) {
        lOutlineFitFoamMessage.setText(message);
        lOutlineFitFoamMessage.setForeground(error ? Color.RED : success ? GuiHelper.DARK_GREEN : null);
    }

    /**
     * Sets the current main outline offset
     * @param side The side to set
     * @param x X offset
     * @param y Y offset
     */
    public void setMainOutlineOffsetConfig(Side side, double x, double y) {
        OutlinePanel drawPanel = null;
        MoveOutlinePanel movePanel = null;

        if (side == Side.LEFT) {
            drawPanel = (OutlinePanel) leftDisplayPanel;
            movePanel = moveLeftOutlineActual;
        } else {
            drawPanel = (OutlinePanel) rightDisplayPanel;
            movePanel = moveRightOutlineActual;
        }

        movePanel.getTfOffsetX().setText(String.format("%.2f", x));
        movePanel.getTfOffsetY().setText(String.format("%.2f", y));
        drawPanel.setMainOutlineOffset(x, y);
    }

    /**
     * Sets the current secondary outline offset
     * @param side The side to set
     * @param x X offset
     * @param y Y offset
     */
    public void setSecondaryOutlineOffsetConfig(Side side, double x, double y) {
        OutlinePanel drawPanel = null;

        if (side == Side.LEFT) {
            drawPanel = (OutlinePanel) leftDisplayPanel;
        } else {
            drawPanel = (OutlinePanel) rightDisplayPanel;
        }

        drawPanel.setSecondaryOutlineOffset(x, y);
    }

    /**
     * Sets the current outline rotation
     * @param side The side to set
     * @param rotation Rotation in degrees
     */
    public void setOutlineRotation(Side side, double rotation) {
        JTextField tfRotation = (side == Side.LEFT) ? tfLeftRotation : tfRightRotation;
        tfRotation.setText(String.format("%.2f", rotation));
    }

    /**
     * Sets the current outline scale
     * @param side The side to set
     * @param scale Scale factor
     */
    public void setOutlineScale(Side side, double scale) {
        JTextField tfScale = (side == Side.LEFT) ? tfLeftScale : tfRightScale;
        tfScale.setText(String.format("%.4f", scale));
    }

    /**
     * Sets the calculated angles the front/back edge of the cut body would have
     * @param front Front angle (in deg)
     * @param back Back angle (in deg)
     */
    public void setAngles(double front, double back) {
        tfFrontEdgeAngle.setText(String.format("%.2f", front));
        tfBackEdgeAngle.setText(String.format("%.2f", back));
    }

    /**
     * Changes the current theme
     * @param theme The new theme
     */
    public void changeTheme(Theme theme) {
        ((OutlinePanel) leftDisplayPanel).changeTheme(theme);
        ((OutlinePanel) rightDisplayPanel).changeTheme(theme);
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        createUIComponents();
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        panel1.setMinimumSize(new Dimension(600, 156));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "A. Outline Files", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        tfFileInputLeft = new JTextField();
        tfFileInputLeft.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(tfFileInputLeft, gbc);
        tfFileInputRight = new JTextField();
        tfFileInputRight.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(tfFileInputRight, gbc);
        final JLabel label1 = new JLabel();
        label1.setHorizontalTextPosition(2);
        label1.setText("Left File:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 10);
        panel1.add(label1, gbc);
        final JLabel label2 = new JLabel();
        label2.setHorizontalAlignment(2);
        label2.setText("Right File:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 10);
        panel1.add(label2, gbc);
        bSelectLeftOutline = new JButton();
        bSelectLeftOutline.setText("...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel1.add(bSelectLeftOutline, gbc);
        bSelectRightOutline = new JButton();
        bSelectRightOutline.setText("...");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel1.add(bSelectRightOutline, gbc);
        bSwapLeftRight = new JButton();
        bSwapLeftRight.setText("<> Swap");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel1.add(bSwapLeftRight, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel2, gbc);
        final JLabel label3 = new JLabel();
        label3.setText("L. Width:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel2.add(label3, gbc);
        tfLeftOutlineWidth = new JTextField();
        tfLeftOutlineWidth.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tfLeftOutlineWidth, gbc);
        final JLabel label4 = new JLabel();
        label4.setText("L. Height:");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel2.add(label4, gbc);
        tfLeftOutlineHeight = new JTextField();
        tfLeftOutlineHeight.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tfLeftOutlineHeight, gbc);
        final JLabel label5 = new JLabel();
        label5.setText("R. Width:");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel2.add(label5, gbc);
        tfRightOutlineWidth = new JTextField();
        tfRightOutlineWidth.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tfRightOutlineWidth, gbc);
        final JLabel label6 = new JLabel();
        label6.setText("R. Height:");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel2.add(label6, gbc);
        tfRightOutlineHeight = new JTextField();
        tfRightOutlineHeight.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(tfRightOutlineHeight, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel3, gbc);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "B. Foam Material", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(panel4, gbc);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "I. Foam Block Dimensions", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label7 = new JLabel();
        label7.setText("Height:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel4.add(label7, gbc);
        final JLabel label8 = new JLabel();
        label8.setText("Width:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel4.add(label8, gbc);
        final JLabel label9 = new JLabel();
        label9.setText("Depth: ");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel4.add(label9, gbc);
        tfFoamHeight = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel4.add(tfFoamHeight, gbc);
        tfFoamWidth = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel4.add(tfFoamWidth, gbc);
        tfFoamDepth = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel4.add(tfFoamDepth, gbc);
        lOutlineFitFoamMessage = new JLabel();
        lOutlineFitFoamMessage.setHorizontalAlignment(0);
        lOutlineFitFoamMessage.setHorizontalTextPosition(0);
        lOutlineFitFoamMessage.setText("Select outlines to check if they fit the foam");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel4.add(lOutlineFitFoamMessage, gbc);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(panel5, gbc);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "II. Foam Position", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        tfFoamXoffset = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel5.add(tfFoamXoffset, gbc);
        final JLabel label10 = new JLabel();
        label10.setText("X offset from origin:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel5.add(label10, gbc);
        final JLabel label11 = new JLabel();
        label11.setText("Offset from left wire anchor:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel5.add(label11, gbc);
        tfFoamLeftOffset = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel5.add(tfFoamLeftOffset, gbc);
        final JLabel label12 = new JLabel();
        label12.setText("Offset from right wire anchor:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel5.add(label12, gbc);
        tfFoamRightOffset = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel5.add(tfFoamRightOffset, gbc);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel6, gbc);
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Outline and Material Positioning Preview", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel6.add(leftDisplayPanel, gbc);
        leftDisplayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Left outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel6.add(rightDisplayPanel, gbc);
        rightDisplayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Right Outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label13 = new JLabel();
        label13.setHorizontalAlignment(0);
        label13.setHorizontalTextPosition(0);
        label13.setText("(Front displayed towards the center, red circle is origin)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel6.add(label13, gbc);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel7, gbc);
        panel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "C. Outline Placement", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        panel7.add(panelMoveLeftOutline, gbc);
        panelMoveLeftOutline.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "I. Left Outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        panel7.add(panelMoveRightOutline, gbc);
        panelMoveRightOutline.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "II. Right Outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panelBottomWrapper = new JPanel();
        panelBottomWrapper.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel7.add(panelBottomWrapper, gbc);
        panelBottomWrapper.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 0)));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.BOTH;
        panelBottomWrapper.add(panel8, gbc);
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "III. Outline Angles", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label14 = new JLabel();
        label14.setText("Front Edge (deg):");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel8.add(label14, gbc);
        tfFrontEdgeAngle = new JTextField();
        tfFrontEdgeAngle.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel8.add(tfFrontEdgeAngle, gbc);
        final JLabel label15 = new JLabel();
        label15.setText("Back Edge (deg):");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel8.add(label15, gbc);
        tfBackEdgeAngle = new JTextField();
        tfBackEdgeAngle.setEditable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel8.add(tfBackEdgeAngle, gbc);
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.BOTH;
        panelBottomWrapper.add(panel9, gbc);
        panel9.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "IV. Outline Rotation", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label17 = new JLabel();
        label17.setText("Left Rotation (deg):");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel9.add(label17, gbc);
        tfLeftRotation = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel9.add(tfLeftRotation, gbc);
        final JLabel label18 = new JLabel();
        label18.setText("Right Rotation (deg):");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel9.add(label18, gbc);
        tfRightRotation = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel9.add(tfRightRotation, gbc);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.BOTH;
        panelBottomWrapper.add(panel10, gbc);
        panel10.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "V. Outline Scale", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label19 = new JLabel();
        label19.setText("Left Scale:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel10.add(label19, gbc);
        tfLeftScale = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel10.add(tfLeftScale, gbc);
        final JLabel label20 = new JLabel();
        label20.setText("Right Scale:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel10.add(label20, gbc);
        tfRightScale = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel10.add(tfRightScale, gbc);
        final JLabel label16 = new JLabel();
        label16.setHorizontalAlignment(0);
        label16.setText("(Hover over buttons for a tooltip description)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel7.add(label16, gbc);
        
        // Set tooltips
        tfFileInputLeft.setToolTipText("Path to the left outline file");
        tfFileInputRight.setToolTipText("Path to the right outline file");
        bSelectLeftOutline.setToolTipText("Browse and select the left outline file");
        bSelectRightOutline.setToolTipText("Browse and select the right outline file");
        bSwapLeftRight.setToolTipText("Swap the left and right outline files");
        tfFoamHeight.setToolTipText("Height of the foam block");
        tfFoamWidth.setToolTipText("Width of the foam block");
        tfFoamDepth.setToolTipText("Depth of the foam block");
        tfLeftOutlineWidth.setToolTipText("Width of the left outline");
        tfRightOutlineWidth.setToolTipText("Width of the right outline");
        tfLeftOutlineHeight.setToolTipText("Height of the left outline");
        tfRightOutlineHeight.setToolTipText("Height of the right outline");
        tfFrontEdgeAngle.setToolTipText("Angle of the cutting blade at the front edge");
        tfBackEdgeAngle.setToolTipText("Angle of the cutting blade at the back edge");
        tfLeftRotation.setToolTipText("Rotation angle for the left outline");
        tfRightRotation.setToolTipText("Rotation angle for the right outline");
        tfLeftScale.setToolTipText("Scale factor for the left outline");
        tfRightScale.setToolTipText("Scale factor for the right outline");
        tfFoamXoffset.setToolTipText("X position of the foam block");
        tfFoamLeftOffset.setToolTipText("Offset of the left wire anchor from the left edge");
        tfFoamRightOffset.setToolTipText("Offset of the right wire anchor from the right edge");
    }

    /**
     * Lock down the arrange panel GUI (disable only file selection, rotation, scale)
     * Allows foam configuration, swap, and movement when locked
     * @param locked Should this Panel be locked?
     */
    public void setLockedDown(boolean locked) {
        // Disable file selection buttons and fields when locked
        bSelectLeftOutline.setEnabled(!locked);
        bSelectRightOutline.setEnabled(!locked);
        tfFileInputLeft.setEnabled(!locked);
        tfFileInputRight.setEnabled(!locked);
        
        // Disable rotation and scale fields when locked
        tfLeftRotation.setEnabled(!locked);
        tfRightRotation.setEnabled(!locked);
        tfLeftScale.setEnabled(!locked);
        tfRightScale.setEnabled(!locked);
        
        // Keep foam configuration, swap, and movement enabled at all times
        // These components remain accessible regardless of lock state
    }

}

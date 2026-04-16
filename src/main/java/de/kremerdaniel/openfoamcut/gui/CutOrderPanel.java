package de.kremerdaniel.openfoamcut.gui;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.controller.CutOrderController;
import de.kremerdaniel.openfoamcut.controller.GlobalController;
import de.kremerdaniel.openfoamcut.gui.helper.GuiHelper;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Panel to add additional lines, set sync points and determine cut order
 */
@SuppressWarnings({"PMD.SingularField"})
public class CutOrderPanel {

    private CutOrderController controller = CutOrderController.getInstance(GlobalController.getInstance());

    private JCheckBox lockDownAFileCheckBox;
    private boolean lockToggleTempDisabled = false;

    @Getter
    private JPanel panel;
    private JPanel synchronizePointsPanel;
    private JPanel cutOrderPanel;
    private JPanel leftDisplayPanel;
    private JPanel rightDisplayPanel;
    private JPanel addLeftLinesPanel;
    private JPanel addRightLinesPanel;
    private JPanel addLinesPanel;
    private JButton addSyncPointButton;
    private JButton removeSyncPointButton;
    private JList<SyncedPoints> listSyncPoints;
    private JList<SyncedPoints> listCutOrder;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton addSelectedFromBButton;
    private JButton removeFromCutOrderButton;
    private AdditionalLinesPanel addLeftLinesPanelParent;
    private AdditionalLinesPanel addRightLinesPanelParent;

    private PointSelectionMode leftSelectionMode;
    private PointSelectionMode rightSelectionMode;
    private boolean addingLeftLine = false;
    private boolean addingRightLine = false;
    private boolean addingSyncPoint = false;

    private double currentLineX1;
    private double currentLineY1;
    private double currentLineX2;
    private double currentLineY2;
    private double currentLeftPointX;
    private double currentLeftPointY;

    private DefaultListModel<SyncedPoints> syncPointModel = new DefaultListModel<>();
    private DefaultListModel<SyncedPoints> cutOrderModel = new DefaultListModel<>();

    /**
     * Creates a new Cut Order Panel
     */
    public CutOrderPanel() {
        initializeUI();
        lockDownAFileCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (!lockToggleTempDisabled) {
                    controller.triggerLockDownArrange(lockDownAFileCheckBox.isSelected());
                }
            }
        });
        // selectLeftPointButton and selectRightPointButton listeners removed - they are now unused
        addSyncPointButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (addingSyncPoint) {
                    // Cancel current sync point addition
                    addingSyncPoint = false;
                    ((OutlinePanel) leftDisplayPanel).setPickingEnabled(false, false);
                    ((OutlinePanel) rightDisplayPanel).setPickingEnabled(false, false);
                    leftSelectionMode = null;
                    rightSelectionMode = null;
                    return;
                }
                // Start sync point addition - begin with left panel selection
                addingSyncPoint = true;
                ((OutlinePanel) leftDisplayPanel).setPickingEnabled(true, false, "Select left point");
                leftSelectionMode = PointSelectionMode.LEFT;
            }
        });
        removeSyncPointButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.triggerRemoveSyncPoint(listSyncPoints.getSelectedValue());
            }
        });
        addSelectedFromBButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controller.triggerAddCutOrderPoint(listSyncPoints.getSelectedValue());
            }
        });
        removeFromCutOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int index = listCutOrder.getSelectedIndex();
                if (index < 0) {
                    return;
                }
                controller.triggerRemoveCutOrderPoint(index);
            }
        });
        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int index = listCutOrder.getSelectedIndex();
                if (index < 0) {
                    return;
                }
                controller.triggerMoveUpCutOrderPoint(index);
            }
        });
        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int index = listCutOrder.getSelectedIndex();
                if (index < 0) {
                    return;
                }
                controller.triggerMoveDownCutOrderPoint(index);
            }
        });
        listSyncPoints.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                SyncedPoints selected = listSyncPoints.getSelectedValue();
                ((OutlinePanel) leftDisplayPanel).setHighlightSyncPoint(selected != null ? selected.getLeft() : new de.kremerdaniel.openfoamcut.bo.Point());
                ((OutlinePanel) rightDisplayPanel).setHighlightSyncPoint(selected != null ? selected.getRight() : new de.kremerdaniel.openfoamcut.bo.Point());
            }
        });
        listCutOrder.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                SyncedPoints selected = listCutOrder.getSelectedValue();
                ((OutlinePanel) leftDisplayPanel).setHighlightSyncPoint(selected != null ? selected.getLeft() : new de.kremerdaniel.openfoamcut.bo.Point());
                ((OutlinePanel) rightDisplayPanel).setHighlightSyncPoint(selected != null ? selected.getRight() : new de.kremerdaniel.openfoamcut.bo.Point());
            }
        });
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private void createUIComponents() {
        leftDisplayPanel = new OutlinePanel();
        rightDisplayPanel = new OutlinePanel();
        ((OutlinePanel) leftDisplayPanel).setFlipHorizontally(true);
        ((OutlinePanel) leftDisplayPanel).setDrawCenterMarkers(true, false);
        ((OutlinePanel) rightDisplayPanel).setDrawCenterMarkers(true, false);

        addLeftLinesPanelParent = new AdditionalLinesPanel();
        addRightLinesPanelParent = new AdditionalLinesPanel();

        registerPointSelection(
                (OutlinePanel) leftDisplayPanel,
                () -> leftSelectionMode,
                mode -> leftSelectionMode = mode,
                (x, y) -> {},
                (x, y) -> {},
                (x, y) -> {}
        );

        registerPointSelection(
                (OutlinePanel) rightDisplayPanel,
                () -> rightSelectionMode,
                mode -> rightSelectionMode = mode,
                (x, y) -> {},
                (x, y) -> {},
                (x, y) -> {}
        );

        addLineCreationLogic();

        addLeftLinesPanel = addLeftLinesPanelParent.getPanel();
        addRightLinesPanel = addRightLinesPanelParent.getPanel();
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void registerPointSelection(
            OutlinePanel panel,
            Supplier<PointSelectionMode> modeGetter,
            Consumer<PointSelectionMode> modeResetter,
            BiConsumer<Double, Double> firstAddPointHandler,
            BiConsumer<Double, Double> secondAddPointHandler,
            BiConsumer<Double, Double> syncPointHandler
    ) {
        panel.addPointSelectionListener((x, y) -> {
            PointSelectionMode mode = modeGetter.get();
            if (mode == null) {
                return;
            }

            switch (mode) {
                case FIRST:
                    handleFirstPointSelection(x, y, panel, modeResetter, firstAddPointHandler);
                    break;
                case SECOND:
                    handleSecondPointSelection(x, y, panel, modeResetter, secondAddPointHandler);
                    break;
                case LEFT:
                case RIGHT:
                    handleSyncPointSelection(x, y, panel, modeResetter, syncPointHandler);
                    break;
            }
        });
    }

    private void handleFirstPointSelection(double x, double y, OutlinePanel panel,
                                           Consumer<PointSelectionMode> modeResetter,
                                           @SuppressWarnings("PMD.UnusedFormalParameter") BiConsumer<Double, Double> firstAddPointHandler) {
        boolean isLeftPanel = isLeftPanel(panel);
        boolean isAddingLine = isLeftPanel ? addingLeftLine : addingRightLine;
        if (isAddingLine) {
            // Store coordinates in instance variable for later use
            currentLineX1 = x;
            currentLineY1 = y;
            // Keep picking enabled and transition to SECOND mode
            panel.setPickingEnabled(true, true, "Select end point");
            modeResetter.accept(PointSelectionMode.SECOND);
        } else {
            // Reset mode and disable picking
            modeResetter.accept(null);
            panel.setPickingEnabled(false, false);
        }
    }

    private void handleSecondPointSelection(double x, double y, OutlinePanel panel,
                                            Consumer<PointSelectionMode> modeResetter,
                                            @SuppressWarnings("PMD.UnusedFormalParameter") BiConsumer<Double, Double> secondAddPointHandler) {
        boolean isLeftPanel = isLeftPanel(panel);
        boolean isAddingLine = isLeftPanel ? addingLeftLine : addingRightLine;
        if (isAddingLine) {
            // Store coordinates in instance variable
            currentLineX2 = x;
            currentLineY2 = y;
            // In "add line" mode after second point: immediately add the line
            performLineAddition(isLeftPanel ? Side.LEFT : Side.RIGHT);
            // Clear the flag, reset mode, and disable picking
            if (isLeftPanel) {
                addingLeftLine = false;
            } else {
                addingRightLine = false;
            }
            modeResetter.accept(null);
            panel.setPickingEnabled(false, false);
        } else {
            // Reset mode and disable picking
            modeResetter.accept(null);
            panel.setPickingEnabled(false, false);
        }
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private boolean isLeftPanel(OutlinePanel panel) {
        return panel == leftDisplayPanel;
    }

    private void handleSyncPointSelection(double x, double y, OutlinePanel panel,
                                          @SuppressWarnings("PMD.UnusedFormalParameter") Consumer<PointSelectionMode> modeResetter,
                                          @SuppressWarnings("PMD.UnusedFormalParameter") BiConsumer<Double, Double> syncPointHandler) {
        if (addingSyncPoint) {
            boolean isLeft = isLeftPanel(panel);
            if (isLeft) {
                // Store left point coordinates
                currentLeftPointX = x;
                currentLeftPointY = y;
                // Left point selected, now enable right panel for second selection
                ((OutlinePanel) leftDisplayPanel).setPickingEnabled(false, false);
                ((OutlinePanel) rightDisplayPanel).setPickingEnabled(true, false, "Select right point");
                rightSelectionMode = PointSelectionMode.LEFT;
            } else {
                // Right point selected and auto-add the sync point
                ((OutlinePanel) rightDisplayPanel).setPickingEnabled(false, false);
                rightSelectionMode = null;
                addingSyncPoint = false;
                
                controller.triggerAddSyncPoint(currentLeftPointX, currentLeftPointY, x, y);
            }
        }
    }

    private void addLineCreationLogic() {
        // BEGIN Line add logic
        addLeftLinesPanelParent.getAddLineButton().addActionListener(e -> {
            addingLeftLine = true;
            ((OutlinePanel) leftDisplayPanel).setPickingEnabled(true, true, "Select start point");
            leftSelectionMode = PointSelectionMode.FIRST;
        });
        addRightLinesPanelParent.getAddLineButton().addActionListener(e -> {
            addingRightLine = true;
            ((OutlinePanel) rightDisplayPanel).setPickingEnabled(true, true, "Select start point");
            rightSelectionMode = PointSelectionMode.FIRST;
        });
        // END Line add logic

        // BEGIN Line remove logic
        addLeftLinesPanelParent.getRemoveLineButton().addActionListener(e -> {
            controller.triggerRemoveLine(Side.LEFT, addLeftLinesPanelParent.getListAdditionalPoints().getSelectedValue());
        });
        addRightLinesPanelParent.getRemoveLineButton().addActionListener(e -> {
            controller.triggerRemoveLine(Side.RIGHT, addRightLinesPanelParent.getListAdditionalPoints().getSelectedValue());
        });
        // END Line remove logic
    }

    /**
     * Performs the line addition after both points have been selected.
     * @param side The side (LEFT or RIGHT) to add the line to
     */
    private void performLineAddition(Side side) {
        controller.triggerAddLine(side, currentLineX1, currentLineY1, currentLineX2, currentLineY2);
    }

    /**
     * Changes the active theme
     * @param theme The theme to activate
     */
    public void changeTheme(Theme theme) {
        ((OutlinePanel) leftDisplayPanel).changeTheme(theme);
        ((OutlinePanel) rightDisplayPanel).changeTheme(theme);
    }

    /**
     * Displays a warning when locking down the Arrange Panel to active this one
     * @return true if the user is ok with locking down
     */
    public boolean displayLockingDownWarning() {
        return showAcceptAbortWarning(panel, "Locking Step 1 is necessary for generating G-Code.\n\n" +
                "When locked, you will:\n" +
                "  • NOT be able to change outline files, rotation, or scale\n" +
                "  • BE able to move outlines (with associated geometry moving)\n" +
                "  • BE able to configure foam and swap outlines\n" +
                "  • BE able to add and edit additional lines, sync points, and cut order\n\n" +
                "If you unlock it later, ALL additional geometry (lines, sync points, cut order) will be DELETED.\n" +
                "\n" +
                "Proceed with locking?");
    }

    /**
     * Displays a warning what will happen when unlocking the Arrange Panel and lock this one
     * @return true if the user is ok with unlocking consequences
     */
    public boolean displayUnlockWarning() {
        return showAcceptAbortWarning(panel, "Unlocking will DELETE ALL entries in this Step:\n" +
                "  • All additional lines\n" +
                "  • All sync points\n" +
                "  • All cut order entries\n\n" +
                "You will be able to edit Step A again (outline files, rotation, scale, movement).\n\n" +
                "Proceed with unlocking?");
    }

    /**
     * Sets if the Arrange Panel is locked down
     * @param lock if true, unlock this panel
     */
    public void setArrangeLockedDown(boolean lock) {
        lockToggleTempDisabled = true;

        lockDownAFileCheckBox.setSelected(lock);
        // enable us if arrange is locked down
        GuiHelper.setEnabledRecursive(addLinesPanel, lock);
        GuiHelper.setEnabledRecursive(synchronizePointsPanel, lock);
        GuiHelper.setEnabledRecursive(cutOrderPanel, lock);

        lockToggleTempDisabled = false;
    }

    private static boolean showAcceptAbortWarning(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(
                parent,
                message,
                "Warning",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return result == JOptionPane.OK_OPTION;
    }

    /**
     * Sets the main outline to display
     * @param side Left/Right outline
     * @param outline The outline to set
     */
    public void setMainOutline(Side side, CutOutline outline) {
        OutlinePanel panel = null;

        if (side == Side.LEFT) {
            panel = (OutlinePanel) leftDisplayPanel;
        } else {
            panel = (OutlinePanel) rightDisplayPanel;
        }

        panel.setMainOutline(outline);
    }

    /**
     * Sets the main outline offset
     * @param side Left/Right outline offset
     * @param x X offset
     * @param y Y offset
     */
    public void setMainOutlineOffset(Side side, double x, double y) {
        OutlinePanel drawPanel = null;

        if (side == Side.LEFT) {
            drawPanel = (OutlinePanel) leftDisplayPanel;
        } else {
            drawPanel = (OutlinePanel) rightDisplayPanel;
        }

        drawPanel.setMainOutlineOffset(x, y);
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
        ((OutlinePanel) leftDisplayPanel).setFoamOutline(foamDepth, foamHeight, foamOffsetX);
        ((OutlinePanel) rightDisplayPanel).setFoamOutline(foamDepth, foamHeight, foamOffsetX);
    }

    /**
     * Initializes the UI components
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initializeUI() {
        createUIComponents();
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        lockDownAFileCheckBox = new JCheckBox();
        lockDownAFileCheckBox.setHorizontalAlignment(0);
        lockDownAFileCheckBox.setText("Lock down \"1. File Select And Arrange\"");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(lockDownAFileCheckBox, gbc);
        synchronizePointsPanel = new JPanel();
        synchronizePointsPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(synchronizePointsPanel, gbc);
        synchronizePointsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "B. Synchronize Points", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        synchronizePointsPanel.add(scrollPane1, gbc);
        listSyncPoints = new JList();
        scrollPane1.setViewportView(listSyncPoints);
        addSyncPointButton = new JButton();
        addSyncPointButton.setText("Add Sync Point");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        synchronizePointsPanel.add(addSyncPointButton, gbc);
        removeSyncPointButton = new JButton();
        removeSyncPointButton.setText("Remove Sync Point");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        synchronizePointsPanel.add(removeSyncPointButton, gbc);
        cutOrderPanel = new JPanel();
        cutOrderPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(cutOrderPanel, gbc);
        cutOrderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "C. Cut Order", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        cutOrderPanel.add(scrollPane2, gbc);
        listCutOrder = new JList();
        scrollPane2.setViewportView(listCutOrder);
        moveUpButton = new JButton();
        moveUpButton.setText("Move Up");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        cutOrderPanel.add(moveUpButton, gbc);
        moveDownButton = new JButton();
        moveDownButton.setText("Move Down");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        cutOrderPanel.add(moveDownButton, gbc);
        addSelectedFromBButton = new JButton();
        addSelectedFromBButton.setText("Add Selected from B");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        cutOrderPanel.add(addSelectedFromBButton, gbc);
        removeFromCutOrderButton = new JButton();
        removeFromCutOrderButton.setText("Remove from Cut Order");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        cutOrderPanel.add(removeFromCutOrderButton, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "D. Workbench", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(leftDisplayPanel, gbc);
        leftDisplayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Left Outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(rightDisplayPanel, gbc);
        rightDisplayPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Right Outline", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label5 = new JLabel();
        label5.setHorizontalAlignment(0);
        label5.setText("(Left click to select any point, right click to \"create\" a new point / select a point in empty space)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(label5, gbc);
        addLinesPanel = new JPanel();
        addLinesPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(addLinesPanel, gbc);
        addLinesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "A. Add Lines", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        addLinesPanel.add(addLeftLinesPanel, gbc);
        addLeftLinesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "I. Additional Lines Left Cut", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        addLinesPanel.add(addRightLinesPanel, gbc);
        addRightLinesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "II. Additional Lines Right Cut", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        
        // Set tooltips
        addSyncPointButton.setToolTipText("Add a new synchronization point");
        removeSyncPointButton.setToolTipText("Remove selected synchronization point");
        listSyncPoints.setToolTipText("List of synchronization points");
        listCutOrder.setToolTipText("Order of cut items");
        moveUpButton.setToolTipText("Move selected item up in the cut order");
        moveDownButton.setToolTipText("Move selected item down in the cut order");
        addSelectedFromBButton.setToolTipText("Add selected item to cut order");
        removeFromCutOrderButton.setToolTipText("Remove selected item from cut order");
    }

    private enum PointSelectionMode {
        FIRST,
        SECOND,
        LEFT,
        RIGHT
    }

    /**
     * Sets the additional lines to display in addition to the actual outlines
     * @param side Left/Right side to add lines to
     * @param additionalLines List of additional lines to draw
     */
    public void setAdditionalLinesOf(Side side, List<Line> additionalLines) {
        OutlinePanel display;
        AdditionalLinesPanel linesPanel;
        if (side == Side.LEFT) {
            display = (OutlinePanel) leftDisplayPanel;
            linesPanel = addLeftLinesPanelParent;
        } else {
            display = (OutlinePanel) rightDisplayPanel;
            linesPanel = addRightLinesPanelParent;
        }
        display.setAdditionalLines(additionalLines);
        linesPanel.setAdditionalLines(additionalLines);
    }

    /**
     * Sets the sync points to display
     * @param synchronizedPoints List of sync points
     */
    public void setSynchronizedPoints(List<SyncedPoints> synchronizedPoints) {
        syncPointModel.clear();
        listSyncPoints.setModel(syncPointModel);
        installListLineRenderer();
        for (SyncedPoints p : synchronizedPoints) {
            syncPointModel.addElement(p);
        }

        ((OutlinePanel) leftDisplayPanel).setSyncPoints(synchronizedPoints.stream().map(SyncedPoints::getLeft).collect(Collectors.toList()));
        ((OutlinePanel) rightDisplayPanel).setSyncPoints(synchronizedPoints.stream().map(SyncedPoints::getRight).collect(Collectors.toList()));
    }

    private void installListLineRenderer() {
        DefaultListCellRenderer renderer = new LineListCellRenderer();
        listSyncPoints.setCellRenderer(renderer);
        listCutOrder.setCellRenderer(renderer);
    }

    private static final class LineListCellRenderer extends DefaultListCellRenderer { @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof SyncedPoints p) {
                setText(String.format(
                        "(%.2f, %.2f) <---> (%.2f, %.2f)",
                        p.getLeft().getX(), p.getLeft().getY(), p.getRight().getX(), p.getRight().getY()
                ));
            }

            return this;
        }
    }

    /**
     * Sets the point of the cut order
     * @param cutOrder List of cut order points
     */
    public void setCutOrderPoints(List<SyncedPoints> cutOrder) {
        cutOrderModel.clear();
        listCutOrder.setModel(cutOrderModel);
        installListLineRenderer();
        for (SyncedPoints p : cutOrder) {
            cutOrderModel.addElement(p);
        }
    }

}

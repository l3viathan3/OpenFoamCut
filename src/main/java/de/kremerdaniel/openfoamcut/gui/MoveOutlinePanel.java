package de.kremerdaniel.openfoamcut.gui;

import javax.swing.*;

import de.kremerdaniel.openfoamcut.gui.MoveOutlineActionListener.MoveOutlineAction;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Objects;

/**
 * Panel with all controls needed to move a outline around
 */
@SuppressWarnings({"PMD.SingularField"})
public class MoveOutlinePanel {

    private static final class DefaultMoveOutlineActionListener implements MoveOutlineActionListener {
    }

    private JButton btnAlignLeft;
    private JButton btnAlignToOtherLeft;
    private JButton btnAlignHOtherCenter;
    private JButton btnAlignToOtherRight;
    private JButton btnAlignRight;
    private JButton btnAlignToOtherTop;
    private JButton btnAlignTop;
    private JButton btnAlignToOtherBottom;
    private JButton btnAlignBottom;

    @Getter
    private JTextField tfOffsetX;
    @Getter
    private JTextField tfOffsetY;

    @Getter
    private JPanel panel;
    private JButton btnAlignHCenter;
    private JButton btnAlignOtherVOtherCenter;
    private JButton btnAlignVCenter;

    @Setter
    private MoveOutlineActionListener moveOutlineActionListener = new DefaultMoveOutlineActionListener();

    /**
     * Creates a Panel to allow the user to move a outline around (offset)
     */
    public MoveOutlinePanel() {
        initializeUI();
        btnAlignLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_LEFT, MoveOutlinePanel.this);
            }
        });
        btnAlignToOtherLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_LEFT, MoveOutlinePanel.this);
            }
        });
        btnAlignHOtherCenter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_H_CENTER, MoveOutlinePanel.this);
            }
        });
        btnAlignHCenter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_H_CENTER, MoveOutlinePanel.this);
            }
        });
        btnAlignToOtherRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_RIGHT, MoveOutlinePanel.this);
            }
        });
        btnAlignRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_RIGHT, MoveOutlinePanel.this);
            }
        });
        btnAlignTop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_TOP, MoveOutlinePanel.this);
            }
        });
        btnAlignToOtherTop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_TOP, MoveOutlinePanel.this);
            }
        });
        btnAlignOtherVOtherCenter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_V_CENTER, MoveOutlinePanel.this);
            }
        });
        btnAlignVCenter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_V_CENTER, MoveOutlinePanel.this);
            }
        });
        btnAlignToOtherBottom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_OTHER_BOTTOM, MoveOutlinePanel.this);
            }
        });
        btnAlignBottom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                moveOutlineActionListener.actionPerformed(MoveOutlineAction.ALIGN_ABSOLUTE_BOTTOM, MoveOutlinePanel.this);
            }
        });
        FocusAdapter focusAdapterManualChange = getManualChangeFocusAdapter();
        tfOffsetX.addFocusListener(focusAdapterManualChange);
        tfOffsetY.addFocusListener(focusAdapterManualChange);
    }

    private FocusAdapter getManualChangeFocusAdapter() {
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
                    moveOutlineActionListener.actionPerformed(MoveOutlineAction.OFFSET_MANUALLY_CHANGED, MoveOutlinePanel.this);
                }
            }
        };
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel1, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Offset X:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 10, 0, 5);
        panel1.add(label1, gbc);
        tfOffsetX = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel1.add(tfOffsetX, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Offset Y:");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel1.add(label2, gbc);
        tfOffsetY = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel1.add(tfOffsetY, gbc);
        btnAlignLeft = new JButton();
        btnAlignLeft.setText("<<");
        btnAlignLeft.setToolTipText("Align to front of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        panel.add(btnAlignLeft, gbc);
        btnAlignToOtherLeft = new JButton();
        btnAlignToOtherLeft.setText("< ");
        btnAlignToOtherLeft.setToolTipText("Align to front of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignToOtherLeft, gbc);
        btnAlignHOtherCenter = new JButton();
        btnAlignHOtherCenter.setText("X ");
        btnAlignHOtherCenter.setToolTipText("Horizontally align center to center of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignHOtherCenter, gbc);
        btnAlignToOtherRight = new JButton();
        btnAlignToOtherRight.setText("> ");
        btnAlignToOtherRight.setToolTipText("Align to back of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignToOtherRight, gbc);
        btnAlignRight = new JButton();
        btnAlignRight.setText(">>");
        btnAlignRight.setToolTipText("Align to back of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignRight, gbc);
        btnAlignTop = new JButton();
        btnAlignTop.setText("^^");
        btnAlignTop.setToolTipText("Align to top of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        panel.add(btnAlignTop, gbc);
        btnAlignToOtherTop = new JButton();
        btnAlignToOtherTop.setText("^ ");
        btnAlignToOtherTop.setToolTipText("Align to top of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignToOtherTop, gbc);
        btnAlignToOtherBottom = new JButton();
        btnAlignToOtherBottom.setText("V ");
        btnAlignToOtherBottom.setToolTipText("Align to bottom of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignToOtherBottom, gbc);
        btnAlignBottom = new JButton();
        btnAlignBottom.setText("VV");
        btnAlignBottom.setToolTipText("Align to bottom of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignBottom, gbc);
        btnAlignHCenter = new JButton();
        btnAlignHCenter.setText("XX");
        btnAlignHCenter.setToolTipText("Horizontally align to center of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignHCenter, gbc);
        btnAlignOtherVOtherCenter = new JButton();
        btnAlignOtherVOtherCenter.setText("X ");
        btnAlignOtherVOtherCenter.setToolTipText("Vertically align center to center of other outline");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignOtherVOtherCenter, gbc);
        btnAlignVCenter = new JButton();
        btnAlignVCenter.setText("XX");
        btnAlignVCenter.setToolTipText("Vertically align to center of foam block");
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        panel.add(btnAlignVCenter, gbc);
        
        // Set tooltips
        tfOffsetX.setToolTipText("X offset to move the outline");
        tfOffsetY.setToolTipText("Y offset to move the outline");
    }
}

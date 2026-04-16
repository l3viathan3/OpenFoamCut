package de.kremerdaniel.openfoamcut.gui;

import javax.swing.*;

import de.kremerdaniel.openfoamcut.bo.Line;
import lombok.Getter;

import java.awt.*;
import java.util.List;

/**
 * Panel for managing additional lines in the geometry
 */
@Getter
public class AdditionalLinesPanel {

    private JPanel additionalGeometryPanel;

    private JPanel panel;

    private JButton addLineButton;
    private JButton removeLineButton;
    private JList<Line> listAdditionalPoints;

    private DefaultListModel<Line> additionalLinesModel = new DefaultListModel<>();

    /**
     * Sets the list of additional lines to display
     * @param additionalLines List of lines to display
     */
    public void setAdditionalLines(List<Line> additionalLines) {
        additionalLinesModel.clear();
        listAdditionalPoints.setModel(additionalLinesModel);
        installListLineRenderer();
        for (Line l : additionalLines) {
            additionalLinesModel.addElement(l);        
        }
    }

    private void installListLineRenderer() {
        listAdditionalPoints.setCellRenderer(new LineListCellRenderer());
    }

    private static final class LineListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof Line l) {
                setText(String.format(
                    "(%.2f, %.2f) --- (%.2f, %.2f)",
                    l.getStart().getX(), l.getStart().getY(),
                    l.getEnd().getX(), l.getEnd().getY()
                ));
            }
            return this;
        }
    }

    /**
     * Creates a panel for managing additional lines
     */
    public AdditionalLinesPanel() {
        initializeUI();
    }

    /**
     * Initializes the UI components
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initializeUI() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        additionalGeometryPanel = new JPanel();
        additionalGeometryPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(additionalGeometryPanel, gbc);
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 8;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        additionalGeometryPanel.add(scrollPane1, gbc);
        listAdditionalPoints = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        listAdditionalPoints.setModel(defaultListModel1);
        scrollPane1.setViewportView(listAdditionalPoints);
        addLineButton = new JButton();
        addLineButton.setText("Add Line");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 5, 5);
        additionalGeometryPanel.add(addLineButton, gbc);
        removeLineButton = new JButton();
        removeLineButton.setText("Remove Line");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 5);
        additionalGeometryPanel.add(removeLineButton, gbc);
        
        // Set tooltips
        addLineButton.setToolTipText("Add a new line to the additional geometry");
        removeLineButton.setToolTipText("Remove selected line from additional geometry");
        listAdditionalPoints.setToolTipText("List of additional geometric lines");
    }
}

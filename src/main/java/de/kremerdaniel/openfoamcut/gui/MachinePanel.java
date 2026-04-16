package de.kremerdaniel.openfoamcut.gui;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.kremerdaniel.openfoamcut.controller.MachineConfigController;

import java.awt.*;

/**
 * Panel to configure the machine, such as G-Code templates
 */
@SuppressWarnings({"PMD.SingularField"})
public class MachinePanel {

    private MachineConfigController controller = MachineConfigController.getInstance();

    @Getter
    private JPanel panel;
    private JTextPane tpHints;
    private JTextArea taStartCode;
    private JTextArea taLineCode;
    private JTextArea taEndCode;
    private JComboBox<String> cbProfiles;
    private JButton btnLoadProfile;
    private JTextField tfProfileName;
    private JButton btnSaveProfile;
    private JButton btnDeleteProfile;

    /**
     * Creates the Machine Panel to configure mostly gcode generation settings
     */
    public MachinePanel() {
        initializeUI();

        attachListener(taStartCode);
        attachListener(taLineCode);
        attachListener(taEndCode);

        btnLoadProfile.addActionListener(e -> loadProfile());
        btnSaveProfile.addActionListener(e -> saveProfile());
        btnDeleteProfile.addActionListener(e -> deleteProfile());

        updateProfilesCombo();
    }

    private void attachListener(JTextArea ta) {
        ta.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                codesChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                codesChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                codesChanged();
            }
        });
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        final JPanel profilePanel = new JPanel();
        profilePanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(profilePanel, gbc);
        profilePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "A. Profiles"),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "B. G-Code Templates", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 0.33;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel2, gbc);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Hints", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(scrollPane1, gbc);
        tpHints = new JTextPane();
        tpHints.setText("================================================\nAvailable Placeholders:\n================================================\n{x1}, {y1}, {x2}, {y2} : x/y of the (1) left point (2) right point\n\n================================================\nG-Code reference:\n================================================\nG00 X Y Z : Rapid positioning (non-cutting move) | G00 X10 Y5\nG01 X Y Z F : Linear move at feed rate | G01 X20 Y10 F300\nG02 X Y I J F : Clockwise arc move (XY plane) | G02 X10 Y0 I5 J0 F200\nG03 X Y I J F : Counterclockwise arc move (XY plane) | G03 X0 Y10 I0 J5 F200\n\nG04 P : Dwell for time in seconds | G04 P1.5\n\nG17 : Select XY plane | G17\nG18 : Select XZ plane | G18\nG19 : Select YZ plane | G19\n\nG20 : Set units to inches | G20\nG21 : Set units to millimeters | G21\n\nG28 : Go to machine home (via homing cycle) | G28\nG28.1 : Set machine home position | G28.1\nG30 : Go to secondary home position | G30\nG30.1 : Set secondary home position | G30.1\n\nG38.2 X Y Z F : Probe toward workpiece (fail on no contact) | G38.2 Z-20 F100\nG38.3 X Y Z F : Probe toward workpiece (no fail) | G38.3 Z-20 F100\nG38.4 X Y Z F : Probe away from workpiece (fail on contact) | G38.4 Z5 F100\nG38.5 X Y Z F : Probe away from workpiece (no fail) | G38.5 Z5 F100\n\nG40 : Cancel cutter radius compensation | G40\n\nG43.1 Z : Apply tool length offset | G43.1 Z-12.5\nG49 : Cancel tool length offset | G49\n\nG53 X Y Z : Move in machine coordinate system | G53 G00 Z0\n\nG54 : Select work coordinate system 1 | G54\nG55 : Select work coordinate system 2 | G55\nG56 : Select work coordinate system 3 | G56\nG57 : Select work coordinate system 4 | G57\nG58 : Select work coordinate system 5 | G58\nG59 : Select work coordinate system 6 | G59\n\nG61 : Exact stop mode | G61\nG64 : Continuous mode (default) | G64\n\nG80 : Cancel motion mode | G80\n\nG90 : Absolute positioning mode | G90\nG91 : Incremental positioning mode | G91\n\nG92 X Y Z : Set current position offset | G92 X0 Y0 Z0\nG92.1 : Clear G92 offsets | G92.1\nG92.2 : Suspend G92 offsets | G92.2\nG92.3 : Resume G92 offsets | G92.3\n\nG93 : Inverse time feed rate mode | G93\nG94 : Feed rate per minute | G94");
        scrollPane1.setViewportView(tpHints);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        panel3.setPreferredSize(new Dimension(11, 300));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel3, gbc);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "I. G-Code Start Template", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(scrollPane2, gbc);
        taStartCode = new JTextArea();
        scrollPane2.setViewportView(taStartCode);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel4.setPreferredSize(new Dimension(11, 300));
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel4, gbc);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "II. G-Code Line Template", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane3 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(scrollPane3, gbc);
        taLineCode = new JTextArea();
        scrollPane3.setViewportView(taLineCode);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        panel5.setPreferredSize(new Dimension(11, 300));
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(panel5, gbc);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "III. G-Code End Template", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane4 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel5.add(scrollPane4, gbc);
        taEndCode = new JTextArea();
        scrollPane4.setViewportView(taEndCode);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.weightx = 0.33;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(spacer2, gbc);
        
        // Profile components with spacers for padding
        final JPanel leftSpacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(leftSpacer1, gbc);
        
        cbProfiles = new JComboBox<>();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(cbProfiles, gbc);
        
        btnLoadProfile = new JButton("Load");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        profilePanel.add(btnLoadProfile, gbc);
        
        final JPanel rightSpacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(rightSpacer1, gbc);
        
        final JPanel leftSpacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(leftSpacer2, gbc);
        
        tfProfileName = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(tfProfileName, gbc);
        
        btnSaveProfile = new JButton("Save As");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        profilePanel.add(btnSaveProfile, gbc);
        
        final JPanel rightSpacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(rightSpacer2, gbc);
        
        final JPanel leftSpacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(leftSpacer3, gbc);
        
        btnDeleteProfile = new JButton("Delete");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 2;
        profilePanel.add(btnDeleteProfile, gbc);
        
        final JPanel rightSpacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profilePanel.add(rightSpacer3, gbc);
        
        // Set tooltips
        taStartCode.setToolTipText("G-code to run at the start of cutting");
        taLineCode.setToolTipText("G-code to run at the start of each line");
        taEndCode.setToolTipText("G-code to run at the end of cutting");
        btnLoadProfile.setToolTipText("Load a saved machine profile");
        tfProfileName.setToolTipText("Name of the current machine profile");
        btnSaveProfile.setToolTipText("Save the current machine configuration");
        btnDeleteProfile.setToolTipText("Delete the selected machine profile");
    }

    /**
     * Sets the configured g code to display
     * @param startCode Code to add only once at program start
     * @param lineCode Code to add for each line that should be cut
     * @param endCode Code to add only once at program end
     */
    public void setCode(String startCode, String lineCode, String endCode) {
        taStartCode.setText(startCode);
        taLineCode.setText(lineCode);
        taEndCode.setText(endCode);
    }

    private void codesChanged() {
        controller.triggerCodesChanged(taStartCode.getText(), taLineCode.getText(), taEndCode.getText());
    }

    private void loadProfile() {
        String selected = (String) cbProfiles.getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            try {
                controller.loadProfile(selected);
                cbProfiles.setSelectedItem(selected);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(panel, "Profile not found: " + selected, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveProfile() {
        String name = tfProfileName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(panel, "Please enter a profile name.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        controller.saveProfile(name);
        updateProfilesCombo();
        cbProfiles.setSelectedItem(name);
        tfProfileName.setText("");
    }

    private void deleteProfile() {
        String selected = (String) cbProfiles.getSelectedItem();
        if (selected != null && !selected.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(panel, "Delete profile '" + selected + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                controller.removeProfile(selected);
                updateProfilesCombo();
            }
        }
    }

    private void updateProfilesCombo() {
        cbProfiles.removeAllItems();
        for (String name : controller.getProfileNames()) {
            cbProfiles.addItem(name);
        }
    }

    /**
     * Refreshes the profile combo box
     */
    public void refreshProfiles() {
        updateProfilesCombo();
    }
}

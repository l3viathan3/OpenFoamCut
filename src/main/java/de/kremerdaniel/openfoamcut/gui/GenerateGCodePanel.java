package de.kremerdaniel.openfoamcut.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.controller.GenerateGCodeController;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.cutting.SanityCheckValidator;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import lombok.Getter;

/**
 * Panel for generating and displaying G-code
 */
@SuppressWarnings({"PMD.SingularField"})
public class GenerateGCodePanel {

    @Getter
    private JPanel panel;
    private JButton generateButton;
    private JButton generateSaveButton;
    private JTextPane sanityCheckTextPane;
    private JTextArea taGCodeOutput;
    private JPanel previewPanelLeft;
    private JPanel previewPanelRight;
    private JButton btnPlayPause;

    private static final class GenerateActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            GenerateGCodeController.getInstance().triggerGCodeGeneration();
        }
    }

    private static final class PlayPauseActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            GenerateGCodeController.getInstance().triggerPlaySimulation();
        }
    }

    private final class GenerateSaveActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            GenerateGCodeController.getInstance().triggerGCodeGeneration();
            GCodeResult result = RuntimeModelManager.getInstance().getLastGCodeResult();
            if (result == null) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(generateSaveButton), "No G-Code generated.");
                return;
            }
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("output.gc"));
            if (chooser.showSaveDialog(SwingUtilities.getWindowAncestor(generateSaveButton)) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    Files.writeString(file.toPath(), result.getGcode());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(generateSaveButton), "Error saving file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Constructor for GenerateGCodePanel
     */
    public GenerateGCodePanel() {
        initializeUI();
        generateButton.addActionListener(new GenerateActionListener());
        btnPlayPause.addActionListener(new PlayPauseActionListener());
        generateSaveButton.addActionListener(new GenerateSaveActionListener());
    }

    /**
     * Displays the generated G-code result
     * @param gcodeResult The G-code result to display
     */
    public void displayGCodeResult(GCodeResult gcodeResult) {
        taGCodeOutput.setText(gcodeResult.getGcode());
        ((OutlinePanel) previewPanelLeft).setAnchorPoint(gcodeResult.getLastAnchorPoint(Side.LEFT));
        ((OutlinePanel) previewPanelRight).setAnchorPoint(gcodeResult.getLastAnchorPoint(Side.RIGHT));
        ((OutlinePanel) previewPanelLeft).setMainOutline(gcodeResult.getLeftCut());
        ((OutlinePanel) previewPanelRight).setMainOutline(gcodeResult.getRightCut());
    }

    /**
     * Displays the sanity check results
     * @param checkResults The list of sanity check results
     */
    public void displaySanityCheckResults(List<SanityCheckValidator.CheckResult> checkResults) {
        StyledDocument doc = sanityCheckTextPane.getStyledDocument();
        try {
            // Clear existing text
            doc.remove(0, doc.getLength());
            
            // Create styles for each severity level
            SimpleAttributeSet okStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(okStyle, new Color(0, 128, 0)); // Green
            
            SimpleAttributeSet warningStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(warningStyle, new Color(255, 140, 0)); // Orange
            
            SimpleAttributeSet errorStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(errorStyle, Color.RED);
            
            // Add each message as a line with appropriate color
            for (SanityCheckValidator.CheckResult result : checkResults) {
                SimpleAttributeSet style = switch (result.severity) {
                    case OK -> okStyle;
                    case WARNING -> warningStyle;
                    case ERROR -> errorStyle;
                };
                
                String prefix = getStatusPrefix(result.severity);
                doc.insertString(doc.getLength(), prefix + result.message + "\n", style);
            }
            
            // Force revalidation of parent hierarchy to update layout
            SwingUtilities.invokeLater(() -> {
                Container parent = sanityCheckTextPane.getParent();
                while (parent != null) {
                    parent.revalidate();
                    parent = parent.getParent();
                }
                sanityCheckTextPane.repaint();
            });
        } catch (BadLocationException e) {
            // Should not happen
            e.printStackTrace();
        }
    }

    private String getStatusPrefix(SanityCheckValidator.CheckSeverity severity) {
        return switch (severity) {
            case OK -> "[OK]      ";
            case WARNING -> "[WARNING] ";
            case ERROR -> "[ERROR]   ";
        };
    }

    /**
     * Changes the theme of the panel
     * @param theme The new theme to apply
     */
    public void changeTheme(Theme theme) {
        ((OutlinePanel) previewPanelLeft).changeTheme(theme);
        ((OutlinePanel) previewPanelRight).changeTheme(theme);
    }

    /**
     * Sets whether the G-Code generation buttons should be enabled
     * G-Code generation is only allowed when the arrangement is locked
     * @param enabled Whether the buttons should be enabled
     */
    public void setGenerateButtonsEnabled(boolean enabled) {
        generateButton.setEnabled(enabled);
        generateSaveButton.setEnabled(enabled);
    }

    private void createUIComponents() {
        previewPanelLeft = new OutlinePanel();
        previewPanelRight = new OutlinePanel();
        ((OutlinePanel) previewPanelLeft).setFlipHorizontally(true);

        ((OutlinePanel) previewPanelLeft).setDrawCenterMarkers(false, false);
        ((OutlinePanel) previewPanelRight).setDrawCenterMarkers(false, false);
    }

    /**
     * Sets the foam configuration parameters
     * @param foamDepth Depth of the foam
     * @param foamHeight Height of the foam
     * @param foamWidth Width of the foam
     * @param foamOffsetX X offset of the foam
     * @param foamOffsetLeft Left offset
     * @param foamOffsetRight Right offset
     */
    public void setFoamConfig(double foamDepth, double foamHeight, double foamWidth, double foamOffsetX,
                              double foamOffsetLeft, double foamOffsetRight) {
        ((OutlinePanel) previewPanelLeft).setFoamOutline(foamDepth, foamHeight, foamOffsetX);
        ((OutlinePanel) previewPanelRight).setFoamOutline(foamDepth, foamHeight, foamOffsetX);

        ((OutlinePanel) previewPanelLeft).setMainOutlineOffset(-foamOffsetX, 0);
        ((OutlinePanel) previewPanelLeft).setSecondaryOutlineOffset(-foamOffsetX, 0);

        ((OutlinePanel) previewPanelRight).setMainOutlineOffset(-foamOffsetX, 0);
        ((OutlinePanel) previewPanelRight).setSecondaryOutlineOffset(-foamOffsetX, 0);
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
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 0.3;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel1, gbc);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "A. Sanity Checks", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPaneSanityChecks = new JScrollPane();
        scrollPaneSanityChecks.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPaneSanityChecks.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(scrollPaneSanityChecks, gbc);
        
        // Create a JTextPane to display sanity check messages with colors and text selection
        final SanityCheckTextPane sanityCheckTextPane = new SanityCheckTextPane();
        sanityCheckTextPane.setEditable(false);
        sanityCheckTextPane.setMargin(new Insets(5, 5, 5, 5));
        scrollPaneSanityChecks.setViewportView(sanityCheckTextPane);
        
        // Store reference for updating
        this.sanityCheckTextPane = sanityCheckTextPane;
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.33;
        gbc.weighty = 0.35;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel2, gbc);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "B. Generate G-Code", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        generateButton = new JButton();
        generateButton.setText("Generate");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel2.add(generateButton, gbc);
        generateSaveButton = new JButton();
        generateSaveButton.setText("Generate + Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(generateSaveButton, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.67;
        gbc.weighty = 0.35;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel3, gbc);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "C. G-Code Output", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JScrollPane scrollPane1 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(scrollPane1, gbc);
        taGCodeOutput = new JTextArea();
        scrollPane1.setViewportView(taGCodeOutput);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 0.35;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(panel4, gbc);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "D. Cut Preview", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(panel5, gbc);
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "I. Preview Controls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        btnPlayPause = new JButton();
        btnPlayPause.setText(">||");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel5.add(btnPlayPause, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(previewPanelLeft, gbc);
        previewPanelLeft.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "II. Preview Left Side", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(previewPanelRight, gbc);
        previewPanelRight.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "III. Preview Right Side", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        
        // Set tooltips
        generateButton.setToolTipText("Generate G-code for the current configuration");
        generateSaveButton.setToolTipText("Generate and save G-code to a file");
        taGCodeOutput.setToolTipText("Generated G-code output display");
        btnPlayPause.setToolTipText("Play or pause the cutting simulation");
    }

    /**
     * Enables or disables the play/pause button
     * @param enabled True to enable, false to disable
     */
    public void setPlayPauseEnabled(boolean enabled) {
        btnPlayPause.setEnabled(enabled);
    }

    /**
     * Custom JTextPane that calculates preferred size based on content width
     * to allow proper panel expansion without line wrapping
     */
    private static final class SanityCheckTextPane extends JTextPane {
        @Override
        public Dimension getPreferredSize() {
            // Measure content at a very wide width to get natural content dimensions
            setSize(10000, 1);
            return super.getPreferredSize();
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // Return false to prevent the viewport from constraining our width
            // This allows the component to expand to its preferred size
            return false;
        }

        @Override
        public Dimension getMaximumSize() {
            // Return the same as preferred size to prevent compression
            return getPreferredSize();
        }
    }

}

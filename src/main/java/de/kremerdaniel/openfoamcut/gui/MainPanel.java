package de.kremerdaniel.openfoamcut.gui;

import javax.swing.*;

import de.kremerdaniel.openfoamcut.Logger;
import de.kremerdaniel.openfoamcut.controller.ArrangeController;
import de.kremerdaniel.openfoamcut.controller.CutOrderController;
import de.kremerdaniel.openfoamcut.controller.GenerateGCodeController;
import de.kremerdaniel.openfoamcut.controller.GlobalController;
import de.kremerdaniel.openfoamcut.controller.MachineConfigController;
import de.kremerdaniel.openfoamcut.controller.Preview3DController;
import lombok.Getter;

import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * Top level panel holding all sub-panels
 */
@SuppressWarnings({"PMD.SingularField"})
public class MainPanel {
    private static final Logger logger = new Logger(MainPanel.class);
    
    private JTabbedPane tabbedPane1;

    @Getter
    private JPanel panel;
    private JPanel fileSelectionAndArrangePanel;
    private JPanel cutOrderPanel;
    private JPanel machineSetupPanel;
    private JPanel generateGCodePanel;
    private JPanel preview3DPanel;
    private JPanel helpPanel;

    private ArrangePanel fileSelectionAndArrangePanelParent;
    private CutOrderPanel cutOrderPanelParent;
    private MachinePanel machineSetupPanelParent;
    private GenerateGCodePanel generateGCodePanelParent;
    private Preview3DPanel preview3DPanelParent;

    private void createUIComponents() {
        fileSelectionAndArrangePanelParent = new ArrangePanel();
        fileSelectionAndArrangePanel = fileSelectionAndArrangePanelParent.getPanel();
        cutOrderPanelParent = new CutOrderPanel();
        cutOrderPanel = cutOrderPanelParent.getPanel();
        machineSetupPanelParent = new MachinePanel();
        machineSetupPanel = machineSetupPanelParent.getPanel();
        generateGCodePanelParent = new GenerateGCodePanel();
        generateGCodePanel = generateGCodePanelParent.getPanel();
        preview3DPanelParent = new Preview3DPanel();
        preview3DPanel = preview3DPanelParent.getPanel();

        // HELP PANEL START

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");

        // Load from resources
        URL helpURL = MainPanel.class.getResource("/help/help.html");
        if (helpURL != null) {
            try {
                editorPane.setPage(helpURL);
            } catch (IOException e) {
                logger.error("Failed to load help page", e);
                editorPane.setText("<html><body><h1>Error loading help page.</h1></body></html>");
            }
        } else {
            editorPane.setText("<html><body><h1>Help page not found.</h1></body></html>");
        }

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        helpPanel = new JPanel(new BorderLayout());
        helpPanel.add(scrollPane, BorderLayout.CENTER);

        // HELP PANEL END

        ArrangeController.getInstance(GlobalController.getInstance()).setArrangePanel(fileSelectionAndArrangePanelParent);
        CutOrderController.getInstance(GlobalController.getInstance()).setCutOrderPanel(cutOrderPanelParent);
        MachineConfigController.getInstance().setMachinePanel(machineSetupPanelParent);
        GenerateGCodeController.getInstance().setGCodePanel(generateGCodePanelParent);
        Preview3DController.getInstance().setPreview3DPanel(preview3DPanelParent);
    }

    /**
     * Creates the Main Panel
     */
    public MainPanel() {
        initializeUI();
    }

    /**
     * Initializes the UI components
     */
    private void initializeUI() {
        createUIComponents();
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setAutoscrolls(true);
        tabbedPane1 = new JTabbedPane();
        tabbedPane1.setAlignmentX(1.0f);
        tabbedPane1.setAlignmentY(1.0f);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(tabbedPane1, gbc);
        fileSelectionAndArrangePanel.setAlignmentX(1.0f);
        fileSelectionAndArrangePanel.setAlignmentY(1.0f);
        tabbedPane1.addTab("1. File Select & Arrange", fileSelectionAndArrangePanel);
        tabbedPane1.addTab("2. Additional Geometry & Cut Order", cutOrderPanel);
        tabbedPane1.addTab("3. Machine Setup", machineSetupPanel);
        tabbedPane1.addTab("4. Generate G-Code", generateGCodePanel);
        tabbedPane1.addTab("5. 3D Preview", preview3DPanel);
        tabbedPane1.addTab("Help", helpPanel);
        tabbedPane1.addChangeListener(changeEvent -> {
            int selectedIndex = tabbedPane1.getSelectedIndex();
            if (selectedIndex >= 0 && "5. 3D Preview".equals(tabbedPane1.getTitleAt(selectedIndex))) {
                Preview3DController.getInstance().refreshPreview();
            }
        });
    }
}

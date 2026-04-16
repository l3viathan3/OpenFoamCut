package de.kremerdaniel.openfoamcut.gui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import de.kremerdaniel.openfoamcut.OpenFoamCut;
import de.kremerdaniel.openfoamcut.controller.GlobalController;
import de.kremerdaniel.openfoamcut.gui.helper.Documentation;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.model.StateManager;
import jakarta.xml.bind.JAXBException;
import lombok.NoArgsConstructor;

/**
 * The main frame holding the MainPanel and main menu
 */
@NoArgsConstructor
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private transient MainPanel mainPanel = new MainPanel();

    /**
     * Initializes the GUI by adding menus, the app icon and creation of the main panel
     */
    public void initGui() {
        setTitle("OpenFoamCut " + OpenFoamCut.VERSION);
        setSize(1600, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Initialize standard tooltip delays for all GUI elements
        // (OutlinePanel will use its own custom manager for 0 delay)
        ToolTipManager.sharedInstance().setInitialDelay(750);
        ToolTipManager.sharedInstance().setReshowDelay(500);
        ToolTipManager.sharedInstance().setDismissDelay(4000);

        // Set App icon
        List<Image> icons = List.of(
            new ImageIcon(MainFrame.class.getResource("/icons/openfoamcut-16.png")).getImage(),
            new ImageIcon(MainFrame.class.getResource("/icons/openfoamcut-32.png")).getImage(),
            new ImageIcon(MainFrame.class.getResource("/icons/openfoamcut-48.png")).getImage(),
            new ImageIcon(MainFrame.class.getResource("/icons/openfoamcut-64.png")).getImage()
        );

        setIconImages(icons);

        add(mainPanel.getPanel());

        // Add Menu Bar
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);  // Alt+F opens it

        // Menu Items
        JMenuItem openItem = new JMenuItem("Open...", KeyEvent.VK_O);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openItem.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("OpenFoamCut Project (*.ofc)", "ofc");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    StateManager.getInstance().loadFrom(chooser.getSelectedFile().getAbsolutePath());
                    GlobalController.getInstance().refreshGui();
                    invalidate();
                    validate();
                    repaint();
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(this, "Project file not found.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (JAXBException ex) {
                    JOptionPane.showMessageDialog(this, "Error loading project: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JMenuItem saveItem = new JMenuItem("Save", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveItem.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("OpenFoamCut Project (*.ofc)", "ofc");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".ofc")) {
                    file = new File(file.getAbsolutePath() + ".ofc");
                }
                try {
                    StateManager.getInstance().saveTo(file.getAbsolutePath());
                } catch (JAXBException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving project: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        exitItem.addActionListener(e ->
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))
        );

        // Add items to File menu (with separator)
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Add File menu to menu bar
        menuBar.add(fileMenu);

        // Add View
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenuItem lightTheme = new JMenuItem("Light Theme");
        lightTheme.addActionListener(e -> {
            GlobalController.getInstance().triggerThemeChange(Theme.LIGHT);
        });

        JMenuItem darkTheme = new JMenuItem("Dark Theme");
        darkTheme.addActionListener(e -> {
            GlobalController.getInstance().triggerThemeChange(Theme.DARK);
        });

        viewMenu.add(lightTheme);
        viewMenu.add(darkTheme);
        menuBar.add(viewMenu);

        // Add Help
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> about());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        // Attach the menu bar to the frame
        setJMenuBar(menuBar);
    }

    private void about() {
        Documentation.showAboutDialog(this);
    }

}

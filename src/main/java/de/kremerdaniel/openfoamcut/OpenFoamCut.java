package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import de.kremerdaniel.openfoamcut.controller.GlobalController;
import de.kremerdaniel.openfoamcut.gui.MainFrame;
import de.kremerdaniel.openfoamcut.gui.helper.Documentation;
import de.kremerdaniel.openfoamcut.gui.helper.Theme;
import de.kremerdaniel.openfoamcut.gui.helper.ThemeManager;
import de.kremerdaniel.openfoamcut.model.MachineManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/**
 * Main entry and global function configuration class
 */
public final class OpenFoamCut {

    private static final Logger logger = LoggerFactory.getLogger(OpenFoamCut.class);

    /** The current version string, used in title bar and such */
    public static final String VERSION = "1.1";
    private static MainFrame frame;

    /**
     * Main entry point
     * @param args Ignored for now
     * @throws InvocationTargetException Error delegating further
     * @throws InterruptedException No idea how this would even happen
     */
    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        logger.info("Starting OpenFoamCut version {}", VERSION);
        ThemeManager.setTheme(Theme.DARK); // absolute default if everything else fails.

        // Catch user facing errors here. There should not be a higher authority.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof UserErrorException) {
                logger.warn("User error: {}", throwable.getMessage());
                showUserErrorDialog(throwable.getMessage());
            } else {
                logger.error("Uncaught exception", throwable);
            }
        });

        // Add hook in case application crashes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                saveState();
            } catch (Exception e) {
                // Avoid dialogs here — JVM is shutting down
                logger.error("Failed to save state during shutdown", e);
            }
        }));
        
        frame = new MainFrame();

        // Exit gracefully
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveState();
                frame.dispose();
                System.exit(0);
            }
        });

        // Start GUI
        SwingUtilities.invokeLater(() -> {
            try {
                frame.initGui();
            } catch (UserErrorException e) {
                showUserErrorDialog(e.getMessage());
            }
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    // Load last application state
                    loadState();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        GlobalController.getInstance().refreshGui();

                        frame.invalidate();
                        frame.validate();
                        //frame.pack();
                        frame.repaint();
                    } catch (UserErrorException e) {
                        showUserErrorDialog(e.getMessage());
                    } catch (Exception e) {
                        logger.error("Failed to initialize GUI after loading state", e);
                    }
                }
            };
            worker.execute();
        });
    }

    private static void showUserErrorDialog(String message) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                null,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }

    private static void loadState() {
        try {
            MachineManager.getInstance().loadFrom("machine.xml");
            logger.info("Machine configuration loaded successfully");
        } catch (FileNotFoundException | JAXBException e) {
            logger.warn("Failed to load machine configuration", e);
            Documentation.showCorruptMachineStateDialog(frame);
        }

        try {
            StateManager.getInstance().loadFrom("last_state.xml");
            logger.info("Application state loaded successfully");
        } catch (FileNotFoundException e) {
            logger.info("No previous state file found, starting fresh");
            Documentation.showFirstStartDialog(frame);
        } catch (JAXBException e) {
            logger.warn("Failed to load application state", e);
            Documentation.showCorruptSavedStateDialog(frame);
        }
    }
    
    private static void saveState() {
        try {
            StateManager.getInstance().saveTo("last_state.xml");
            MachineManager.getInstance().saveTo("machine.xml");
            logger.info("Application state saved successfully");
        } catch (JAXBException e) {
            logger.error("Failed to save application state", e);
            Documentation.showCouldNotSaveDialog(frame);
        }
    }

    private OpenFoamCut() {

    }

}
package de.kremerdaniel.openfoamcut.gui.helper;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import de.kremerdaniel.openfoamcut.OpenFoamCut;

/**
 * Utility class for showing documentation and dialogs
 */
public final class Documentation {

    /**
     * Shows the first start dialog
     * @param frame The parent frame
     */
    public static void showFirstStartDialog(JFrame frame) {
        runOnEdt(() ->
            JOptionPane.showMessageDialog(
                frame,
                """
                Welcome to OpenFoamCut!

                It looks like this is your first start, default settings will be applied.

                Please have a look at the Help tab!
                Expect that you have to touch EVERYTHING at least once.
                """,
                "Welcome",
                JOptionPane.INFORMATION_MESSAGE
            )
        );
    }

    /**
     * Shows the corrupt saved state dialog
     * @param frame The parent frame
     */
    public static void showCorruptSavedStateDialog(JFrame frame) {
        runOnEdt(() ->
            JOptionPane.showMessageDialog(
                frame,
                """
                The saved application state could not be loaded.

                It may be corrupted or from an incompatible version.
                Default settings have been restored.
                """,
                "Corrupt State",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }

    /**
     * Shows the could not save dialog
     * @param frame The parent frame
     */
    public static void showCouldNotSaveDialog(JFrame frame) {
        runOnEdt(() ->
            JOptionPane.showMessageDialog(
                frame,
                """
                The application state could not be saved.

                Your changes may be lost.
                Please check file permissions or disk space.
                """,
                "Save Failed",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }
    
    /**
     * Shows the about dialog
     * @param frame The parent frame
     */
    public static void showAboutDialog(JFrame frame) {
        runOnEdt(() ->
            JOptionPane.showMessageDialog(
                frame,
                "OpenFoamCut " + OpenFoamCut.VERSION + "\n\r"
                + "\n" + //
                                "\n" + //
                                "Copyright 2026 Daniel Kremer\n" + //
                                "App Icon by icons8.de\n" + //
                                "\n" + //
                                "Permission is hereby granted, free of charge, to any person obtaining\n" + //
                                "a copy of this software and associated documentation files (the “Software”),\n" + //
                                "to deal in the Software without restriction, including without limitation\n" + //
                                "the rights to use, copy, modify, merge, publish, distribute, sublicense,\n" + //
                                "and/or sell copies of the Software, and to permit persons to whom the\n" + //
                                "Software is furnished to do so, subject to the following conditions:\n" + //
                                "\n" + //
                                "The above copyright notice and this permission notice shall be\n" + //
                                "included in all copies or substantial portions of the Software.\n" + //
                                "\n" + //
                                "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS\n" + //
                                "OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" + //
                                "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" + //
                                "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" + //
                                "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING\n" + //
                                "FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS \n" + //
                                "IN THE SOFTWARE.\n" + //
                                "",
                "About & Licence",
                JOptionPane.INFORMATION_MESSAGE,
                new ImageIcon(Documentation.class.getResource("/icons/openfoamcut-64.png"))
            )
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    /**
     * Shows the corrupt machine state dialog
     * @param frame The parent frame
     */
    public static void showCorruptMachineStateDialog(JFrame frame) {
        runOnEdt(() ->
            JOptionPane.showMessageDialog(
                frame,
                """
                The machine configuration could not be loaded.

                It may be corrupted or from an incompatible version.
                Default settings have been restored.
                """,
                "Corrupt Machine Configuration",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }

    private Documentation() {

    }

}
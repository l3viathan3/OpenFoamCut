package de.kremerdaniel.openfoamcut.controller;

import de.kremerdaniel.openfoamcut.gui.MachinePanel;
import de.kremerdaniel.openfoamcut.model.MachineManager;
import de.kremerdaniel.openfoamcut.model.MachineProfile;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Controller for the Machine Configuration Panel, Singleton
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class MachineConfigController {

    static @Getter(lazy = true)
    private final MachineConfigController instance = new MachineConfigController();

    private MachineConfigController() {

    }

    // Models
    private final MachineManager mm = MachineManager.getInstance();

    // View
    @Setter
    private MachinePanel machinePanel;

    /**
     * Refreshes the view belonging to this controller
     */
    public void refreshGui() {
        machinePanel.setCode(mm.getStartCode(), mm.getLineCode(), mm.getEndCode());
        refreshProfiles();
    }

    /**
     * Refreshes the profile list in the GUI
     */
    public void refreshProfiles() {
        if (machinePanel != null) {
            machinePanel.refreshProfiles();
        }
    }

    /**
     * Triggered when something changes the G-Code templates
     * @param start Template for start of program
     * @param line Template for every movement line
     * @param end Template for end of program
     */
    public void triggerCodesChanged(String start, String line, String end) {
        mm.codesChanged(start, line, end);
    }

    /**
     * Saves current settings as a profile
     * @param name the profile name
     */
    public void saveProfile(String name) {
        mm.saveAsProfile(name);
        refreshProfiles();
    }

    /**
     * Loads a profile
     * @param name the profile name
     * @throws IllegalArgumentException if not found
     */
    public void loadProfile(String name) {
        mm.loadProfile(name);
        refreshGui();
    }

    /**
     * Gets list of profile names
     * @return list of names
     */
    public List<String> getProfileNames() {
        return mm.getProfiles().stream().map(MachineProfile::getName).toList();
    }

    /**
     * Removes a profile
     * @param name the profile name
     * @return true if removed
     */
    public boolean removeProfile(String name) {
        boolean removed = mm.removeProfile(name);
        if (removed) {
            refreshProfiles();
        }
        return removed;
    }
}

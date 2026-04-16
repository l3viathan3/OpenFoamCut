package de.kremerdaniel.openfoamcut.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Central machine configuration management
 */
@Data
@XmlRootElement(name = "state")
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass")
public final class MachineManager {

    static @Getter(lazy = true)
    private final MachineManager instance = new MachineManager();

    private String startCode = "G90 ; Absolute positioning\n" + //
                "G21 ; Units in mm\n" + //
                "F200 ; Set feed rate";

    private String lineCode = "G1 X{x1} Y{y1} Z{x2} A{y2}";

    private String endCode = "M2 ; End program";

    private List<MachineProfile> profiles = new ArrayList<>();

    private MachineManager() {
    }

    /**
     * Load a saved Application State from file
     * @param path The absolute file path to read from
     * @throws FileNotFoundException If the file was not found
     * @throws JAXBException If parsing of save file fails
     */
    public void loadFrom(String path) throws FileNotFoundException, JAXBException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }

        JAXBContext context = JAXBContext.newInstance(MachineManager.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        MachineManager loaded = (MachineManager) unmarshaller.unmarshal(file);

        // copy values into singleton
        this.startCode = loaded.startCode;
        this.lineCode = loaded.lineCode;
        this.endCode = loaded.endCode;
        this.profiles = loaded.profiles != null ? new ArrayList<>(loaded.profiles) : new ArrayList<>();
    }

    /**
     * Save the current Machine Configuration State to a file
     * @param path The absolute path to write to
     * @throws JAXBException If anything goes wrong with serialization
     */
    public void saveTo(String path) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(MachineManager.class);
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(this, new File(path));
    }


    /**
     * Updates the G-Code templates
     * @param start Start G-Code template
     * @param line G-code template for every line segment to be generated
     * @param end End G-Code template
     */
    public void codesChanged(String start, String line, String end) {
        this.startCode = start;
        this.lineCode = line;
        this.endCode = end;
    }

    /**
     * Adds a new profile
     * @param profile the profile to add
     */
    public void addProfile(MachineProfile profile) {
        profiles.add(profile);
    }

    /**
     * Removes a profile by name
     * @param name the name of the profile to remove
     * @return true if removed, false if not found
     */
    public boolean removeProfile(String name) {
        return profiles.removeIf(p -> p.getName().equals(name));
    }

    /**
     * Gets a profile by name
     * @param name the name
     * @return the profile or null if not found
     */
    public MachineProfile getProfile(String name) {
        return profiles.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Gets all profiles
     * @return list of profiles
     */
    public List<MachineProfile> getProfiles() {
        return new ArrayList<>(profiles);
    }

    /**
     * Loads a profile by name, applying it to current settings
     * @param name the profile name
     * @throws IllegalArgumentException if profile not found
     */
    public void loadProfile(String name) {
        MachineProfile profile = getProfile(name);
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found: " + name);
        }
        profile.applyTo(this);
    }

    /**
     * Saves current settings as a new profile
     * @param name the profile name
     */
    public void saveAsProfile(String name) {
        MachineProfile profile = MachineProfile.fromCurrent(this, name);
        // Remove existing with same name
        removeProfile(name);
        addProfile(profile);
    }

}
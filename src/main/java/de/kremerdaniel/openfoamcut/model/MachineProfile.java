package de.kremerdaniel.openfoamcut.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a machine profile preset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "profile")
@XmlAccessorType(XmlAccessType.FIELD)
public class MachineProfile {

    private String name;
    private String startCode;
    private String lineCode;
    private String endCode;

    /**
     * Creates a profile from current machine manager state
     * @param mm the machine manager
     * @param name the profile name
     * @return the new profile
     */
    public static MachineProfile fromCurrent(MachineManager mm, String name) {
        return new MachineProfile(name, mm.getStartCode(), mm.getLineCode(), mm.getEndCode());
    }

    /**
     * Applies this profile to the machine manager
     * @param mm the machine manager to apply to
     */
    public void applyTo(MachineManager mm) {
        mm.codesChanged(startCode, lineCode, endCode);
    }
}
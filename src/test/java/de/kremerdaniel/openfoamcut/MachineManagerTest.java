package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.model.MachineManager;
import de.kremerdaniel.openfoamcut.model.MachineProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MachineManager functionality.
 */
class MachineManagerTest {

    private MachineManager mm;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Reset singleton for each test
        // Since it's singleton, we need to be careful, but for tests it's ok
        mm = MachineManager.getInstance();
        mm.getProfiles().clear();
        mm.codesChanged("G90", "G1 X{x1} Y{y1}", "M2");
    }

    @Test
    void testSaveAndLoadProfile() {
        // Save current as profile
        mm.saveAsProfile("TestProfile");

        // Change settings
        mm.codesChanged("G91", "G0 X{x1} Y{y1}", "M30");

        // Load profile
        mm.loadProfile("TestProfile");

        // Check restored
        assertEquals("G90", mm.getStartCode());
        assertEquals("G1 X{x1} Y{y1}", mm.getLineCode());
        assertEquals("M2", mm.getEndCode());
    }

    @Test
    void testAddAndRemoveProfile() {
        MachineProfile profile = new MachineProfile("Manual", "G90", "G1", "M2");
        mm.addProfile(profile);

        assertNotNull(mm.getProfile("Manual"));
        assertTrue(mm.removeProfile("Manual"));
        assertNull(mm.getProfile("Manual"));
        assertFalse(mm.removeProfile("NonExistent"));
    }

    @Test
    void testLoadNonExistentProfile() {
        assertThrows(IllegalArgumentException.class, () -> mm.loadProfile("NonExistent"));
    }

    @Test
    void testSaveAndLoadStateWithProfiles() throws JAXBException, IOException {
        // Add a profile
        mm.saveAsProfile("Profile1");

        // Save to file
        Path file = tempDir.resolve("test.xml");
        mm.saveTo(file.toString());

        // Change settings and add another profile
        mm.codesChanged("G91", "G0", "M30");
        mm.saveAsProfile("Profile2");

        // Load from file
        mm.loadFrom(file.toString());

        // Check current settings restored
        assertEquals("G90", mm.getStartCode());
        assertEquals("G1 X{x1} Y{y1}", mm.getLineCode());
        assertEquals("M2", mm.getEndCode());

        // Check profiles loaded
        assertNotNull(mm.getProfile("Profile1"));
        assertNull(mm.getProfile("Profile2")); // Not saved in the file
    }

    @Test
    void testInvalidXmlProfile() throws IOException {
        // Create invalid XML
        Path invalidFile = tempDir.resolve("invalid.xml");
        Files.writeString(invalidFile, "<state><startCode>G90</startCode><invalid></state>");

        assertThrows(JAXBException.class, () -> mm.loadFrom(invalidFile.toString()));
    }

    @Test
    void testMissingFile() {
        assertThrows(java.io.FileNotFoundException.class, () -> mm.loadFrom("nonexistent.xml"));
    }

    @Test
    void testProfileWithNullFields() {
        MachineProfile profile = new MachineProfile("NullProfile", null, null, null);
        mm.addProfile(profile);

        mm.loadProfile("NullProfile");

        assertNull(mm.getStartCode());
        assertNull(mm.getLineCode());
        assertNull(mm.getEndCode());
    }
}
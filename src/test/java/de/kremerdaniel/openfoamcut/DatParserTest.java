package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.parser.DatParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DatParserTest {

    @Test
    void testLoadDatFileWidth1Normalization() {
        // Load a .dat file normalized to width 1
        String datFilePath = Paths.get("src/test/resource/profile_width1.dat").toAbsolutePath().toString();
        CutOutline outline = DatParser.loadCutOutline(datFilePath);

        assertNotNull(outline);
        assertNotNull(outline.getBounds());
        
        // Verify that the outline is scaled to width 100
        double width = outline.getBounds().getWidth();
        assertEquals(100.0, width, 0.1, "Outline should be scaled to width 100");
    }

    @Test
    void testLoadDatFileWidth05Normalization() {
        // Load a .dat file normalized to width 0.5
        String datFilePath = Paths.get("src/test/resource/profile_width05.dat").toAbsolutePath().toString();
        CutOutline outline = DatParser.loadCutOutline(datFilePath);

        assertNotNull(outline);
        assertNotNull(outline.getBounds());
        
        // Verify that the outline is scaled to width 100
        // Even though this file has half the width, it should still end up at width 100
        double width = outline.getBounds().getWidth();
        assertEquals(100.0, width, 0.1, "Outline should be scaled to width 100 regardless of original normalization");
    }

    @Test
    void testLoadedOutlineHasValidBounds() {
        // Verify that loaded outlines have valid, non-zero bounds
        String datFilePath = Paths.get("src/test/resource/profile_width1.dat").toAbsolutePath().toString();
        CutOutline outline = DatParser.loadCutOutline(datFilePath);

        double width = outline.getBounds().getWidth();
        double height = outline.getBounds().getHeight();
        
        // Should have positive dimensions
        assert width > 0 : "Width should be positive";
        assert height > 0 : "Height should be positive";
        
        // Should have lines
        assert !outline.getLines().isEmpty() : "Outline should have lines";
    }
}

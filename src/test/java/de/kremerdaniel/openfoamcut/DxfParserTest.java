package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import de.kremerdaniel.openfoamcut.parser.DxfParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DXF parsing functionality.
 */
class DxfParserTest {

    @Test
    void testLoadThrushProfil() throws UserErrorException {
        String path = getClass().getClassLoader().getResource("profile1.dxf").getFile();
        CutOutline outline = DxfParser.loadCutOutline(path);
        assertNotNull(outline);
        assertTrue(outline.getBounds().getWidth() > 0);
        assertTrue(outline.getBounds().getHeight() > 0);
    }

    @Test
    void testLoadThrushProfil2() throws UserErrorException {
        String path = getClass().getClassLoader().getResource("profile2.dxf").getFile();
        CutOutline outline = DxfParser.loadCutOutline(path);
        assertNotNull(outline);
        assertTrue(outline.getBounds().getWidth() > 0);
        assertTrue(outline.getBounds().getHeight() > 0);
    }

    @Test
    void testLoadInvalidFile() {
        assertThrows(UserErrorException.class, () -> DxfParser.loadCutOutline("nonexistent.dxf"));
    }
}
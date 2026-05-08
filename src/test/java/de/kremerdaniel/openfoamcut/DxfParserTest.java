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
    void testLoadLightweightPolylineProfile() throws UserErrorException {
        String path = getClass().getClassLoader().getResource("polyline_profile.dxf").getFile();
        CutOutline outline = DxfParser.loadCutOutline(path);
        assertNotNull(outline);
        assertEquals(1682, outline.getLines().size());
        assertTrue(outline.getLines().get(10).getEnd().getX() > outline.getLines().get(10).getStart().getX());
        assertEquals(2.7146101825937876, outline.getLines().get(10).getEnd().getX(), 1e-9);
        assertEquals(27.75192678787505, outline.getLines().get(10).getEnd().getY(), 1e-9);
        assertTrue(outline.getBounds().getWidth() > 0);
        assertTrue(outline.getBounds().getHeight() > 0);
    }

    @Test
    void testLoadInvalidFile() {
        assertThrows(UserErrorException.class, () -> DxfParser.loadCutOutline("nonexistent.dxf"));
    }
}
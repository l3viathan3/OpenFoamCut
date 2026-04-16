package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.cutting.GCodeConverter;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GCodeConverter functionality.
 */
class GCodeConverterTest {

    @Test
    void testGenerateGCodeWithSyncPoints() {
        // Setup state
        StateManager sm = StateManager.getInstance();
        RuntimeModelManager rmm = RuntimeModelManager.getInstance();

        // Clear and set basic config
        sm.getSynchronizedPoints().clear();
        sm.getCutOrder().clear();
        sm.setFoamWidth(1000);
        sm.setFoamDepth(500);
        sm.setFoamHeight(50);

        // Add a sync point
        Point left = new Point(10, 20);
        Point right = new Point(30, 40);
        SyncedPoints sp = new SyncedPoints(left, right);
        sm.getSynchronizedPoints().add(sp);
        sm.getCutOrder().add(sp);

        // Create dummy outlines
        CutOutline leftOutline = new CutOutline();
        CutOutline rightOutline = new CutOutline();
        rmm.setOutlineLeft(leftOutline);
        rmm.setOutlineRight(rightOutline);

        // Generate G-code
        GCodeConverter converter = new GCodeConverter();
        GCodeResult result = converter.generateGCodeFromState();

        assertNotNull(result);
        assertNotNull(result.getGcode());
        assertFalse(result.getGcode().isEmpty());
    }
}
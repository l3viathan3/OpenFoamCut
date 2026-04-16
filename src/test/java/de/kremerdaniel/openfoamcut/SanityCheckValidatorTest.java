package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.cutting.SanityCheckValidator;
import de.kremerdaniel.openfoamcut.model.StateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SanityCheckValidator
 */
class SanityCheckValidatorTest {

    private SanityCheckValidator validator;
    private StateManager sm;

    @BeforeEach
    void setUp() {
        validator = new SanityCheckValidator();
        sm = StateManager.getInstance();
        
        // Set default foam dimensions
        sm.setFoamWidth(1000);
        sm.setFoamDepth(500);
        sm.setFoamHeight(50);
        
        // Clear additional lines to start clean
        sm.setLeftAdditionalLines(new ArrayList<>());
        sm.setRightAdditionalLines(new ArrayList<>());
    }

    @Test
    void testAllChecksPassWithValidResult() {
        // Create a valid G-Code result
        GCodeResult result = createValidGCodeResult();
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        assertNotNull(checks);
        assertFalse(checks.isEmpty());
        
        // All checks should pass
        for (SanityCheckValidator.CheckResult check : checks) {
            assertEquals(SanityCheckValidator.CheckSeverity.OK, check.severity, "Failed check: " + check.message);
        }
    }

    @Test
    void testNullResultFails() {
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(null);
        
        assertNotNull(checks);
        assertFalse(checks.isEmpty());
        
        // At least one check should fail
        boolean hasFailure = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.severity != SanityCheckValidator.CheckSeverity.OK) {
                hasFailure = true;
                break;
            }
        }
        assertTrue(hasFailure);
    }

    @Test
    void testOutlineExceedsFoamBoundaries() {
        GCodeResult result = new GCodeResult();
        
        // Create an outline that exceeds foam boundaries
        List<Line> exceedingLines = new ArrayList<>();
        exceedingLines.add(new Line(new Point(0, 0), new Point(600, 0))); // Exceeds height of 50
        CutOutline exceedingOutline = new CutOutline(exceedingLines, false);
        
        result.setLeftCut(exceedingOutline);
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(new CutOutline(new ArrayList<>(), false));
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Check that "outlines fit" check failed
        boolean fitCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("fit in foam block") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                fitCheckFailed = true;
                break;
            }
        }
        assertTrue(fitCheckFailed);
    }

    @Test
    void testInvalidFoamDimensions() {
        sm.setFoamWidth(0);
        sm.setFoamDepth(500);
        sm.setFoamHeight(50);
        
        GCodeResult result = createValidGCodeResult();
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean dimensionCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("foam dimensions") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                dimensionCheckFailed = true;
                break;
            }
        }
        assertTrue(dimensionCheckFailed);
    }

    @Test
    void testEmptyOutlines() {
        GCodeResult result = new GCodeResult();
        result.setLeftCut(new CutOutline(new ArrayList<>(), false));
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(new CutOutline(new ArrayList<>(), false));
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean emptyCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("empty") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                emptyCheckFailed = true;
                break;
            }
        }
        assertTrue(emptyCheckFailed);
    }

    @Test
    void testDisconnectedLines() {
        GCodeResult result = new GCodeResult();
        
        // Create disconnected lines
        List<Line> disconnectedLines = new ArrayList<>();
        disconnectedLines.add(new Line(new Point(0, 0), new Point(10, 0)));
        disconnectedLines.add(new Line(new Point(50, 0), new Point(60, 0))); // Gap of 40 units
        CutOutline outline = new CutOutline(disconnectedLines, false);
        
        result.setLeftCut(outline);
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(new CutOutline(new ArrayList<>(), false));
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean disconnectCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("connected") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                disconnectCheckFailed = true;
                break;
            }
        }
        assertTrue(disconnectCheckFailed);
    }

    @Test
    void testNegativeAnchorCoordinates() {
        GCodeResult result = new GCodeResult();
        
        // Create anchor moves with negative coordinates
        List<Line> negativeLines = new ArrayList<>();
        negativeLines.add(new Line(new Point(-10, 0), new Point(0, 0)));
        negativeLines.add(new Line(new Point(0, -5), new Point(10, 0)));
        CutOutline negativeOutline = new CutOutline(negativeLines, false);
        
        result.setLeftCut(new CutOutline(new ArrayList<>(), false));
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(negativeOutline);
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean negativeCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if ((check.message.contains("negative") || check.message.contains("below table") || check.message.contains("front of origin")) && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                negativeCheckFailed = true;
                break;
            }
        }
        assertTrue(negativeCheckFailed);
    }

    @Test
    void testFormatCheckResultsWithHtml() {
        List<SanityCheckValidator.CheckResult> checks = new ArrayList<>();
        checks.add(new SanityCheckValidator.CheckResult("Test OK", SanityCheckValidator.CheckSeverity.OK));
        checks.add(new SanityCheckValidator.CheckResult("Test Error", SanityCheckValidator.CheckSeverity.ERROR));
        checks.add(new SanityCheckValidator.CheckResult("Test Warning", SanityCheckValidator.CheckSeverity.WARNING));
        
        String formatted = SanityCheckValidator.formatCheckResults(checks);
        
        assertNotNull(formatted);
        assertTrue(formatted.contains("<html>"));
        assertTrue(formatted.contains("[OK]"));
        assertTrue(formatted.contains("[ERROR]"));
        assertTrue(formatted.contains("[WARNING]"));
        assertTrue(formatted.contains("color: green"));
        assertTrue(formatted.contains("color: red"));
        assertTrue(formatted.contains("color: #FF8C00"));  // Orange/yellow
    }

    @Test
    void testUnconnectedAdditionalLines() {
        GCodeResult result = new GCodeResult();
        
        // Create main outline
        List<Line> mainLines = new ArrayList<>();
        mainLines.add(new Line(new Point(10, 10), new Point(20, 10)));
        mainLines.add(new Line(new Point(20, 10), new Point(20, 20)));
        CutOutline mainOutline = new CutOutline(mainLines, false);
        
        result.setLeftCut(mainOutline);
        result.setRightCut(mainOutline.copy());
        result.setLeftMove(mainOutline.copy());
        result.setRightMove(mainOutline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10");
        
        // Set unconnected additional lines
        List<Line> unconnectedLines = new ArrayList<>();
        unconnectedLines.add(new Line(new Point(50, 50), new Point(60, 50))); // Far away from main outline
        
        sm.setLeftAdditionalLines(unconnectedLines);
        sm.setRightAdditionalLines(new ArrayList<>());
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean connectionCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("connected") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                connectionCheckFailed = true;
                break;
            }
        }
        assertTrue(connectionCheckFailed);
        
        // Clean up
        sm.setLeftAdditionalLines(new ArrayList<>());
    }

    @Test
    void testJuttingOutLinesWithConnectionAreValid() {
        GCodeResult result = new GCodeResult();
        
        // Create main outline
        List<Line> mainLines = new ArrayList<>();
        mainLines.add(new Line(new Point(10, 10), new Point(20, 10)));
        mainLines.add(new Line(new Point(20, 10), new Point(20, 20)));
        CutOutline mainOutline = new CutOutline(mainLines, false);
        
        // Set additional lines that jut out with one connected endpoint
        List<Line> juttingLines = new ArrayList<>();
        juttingLines.add(new Line(new Point(10, 10), new Point(0, 10))); // Connected at (10,10)
        juttingLines.add(new Line(new Point(20, 10), new Point(30, 10))); // Connected at (20,10)
        
        // Create left cut outline with both main and additional lines
        List<Line> leftCutLines = new ArrayList<>(mainLines);
        leftCutLines.addAll(juttingLines);
        CutOutline leftCutWithAdditional = new CutOutline(leftCutLines, false);
        
        result.setLeftCut(leftCutWithAdditional);
        result.setRightCut(mainOutline.copy());
        result.setLeftMove(mainOutline.copy());
        result.setRightMove(mainOutline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10");
        
        sm.setLeftAdditionalLines(juttingLines);
        sm.setRightAdditionalLines(new ArrayList<>());
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Find the connection check
        SanityCheckValidator.CheckResult connectionCheck = null;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("connected")) {
                connectionCheck = check;
                break;
            }
        }
        
        assertNotNull(connectionCheck, "No connection check found");
        assertEquals(SanityCheckValidator.CheckSeverity.OK, connectionCheck.severity, "Connection check should pass. Message: " + connectionCheck.message);
        
        // Clean up
        sm.setLeftAdditionalLines(new ArrayList<>());
    }

    @Test
    void testAdditionalLinesIncludedInGCode() {
        GCodeResult result = new GCodeResult();
        
        // Create main outline: (10,10)-(20,10)-(20,20)
        List<Line> mainLines = new ArrayList<>();
        mainLines.add(new Line(new Point(10, 10), new Point(20, 10)));
        mainLines.add(new Line(new Point(20, 10), new Point(20, 20)));
        
        // Create additional lines that branch from main outline
        List<Line> additionalLines = new ArrayList<>();
        additionalLines.add(new Line(new Point(15, 10), new Point(15, 15))); // Branch from main line
        
        // Create cut outline with all lines (main + additional)
        List<Line> allLines = new ArrayList<>(mainLines);
        allLines.addAll(additionalLines);
        CutOutline cutOutline = new CutOutline(allLines, false);
        
        result.setLeftCut(cutOutline);
        result.setRightCut(cutOutline.copy());
        result.setLeftMove(cutOutline.copy());
        result.setRightMove(cutOutline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10\nG1 X15 Y15\nG1 X20 Y10\nG1 X20 Y20");
        
        sm.setLeftAdditionalLines(additionalLines);
        sm.setRightAdditionalLines(new ArrayList<>());
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Find the G-Code check
        SanityCheckValidator.CheckResult gCodeCheck = null;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("G-Code")) {
                gCodeCheck = check;
                break;
            }
        }
        
        assertNotNull(gCodeCheck, "No G-Code check found");
        assertEquals(SanityCheckValidator.CheckSeverity.OK, gCodeCheck.severity, "G-Code check should pass when additional lines are included. Message: " + gCodeCheck.message);
        
        // Clean up
        sm.setLeftAdditionalLines(new ArrayList<>());
    }

    @Test
    void testAdditionalLinesMissingFromGCode() {
        GCodeResult result = new GCodeResult();
        
        // Create main outline
        List<Line> mainLines = new ArrayList<>();
        mainLines.add(new Line(new Point(10, 10), new Point(20, 10)));
        mainLines.add(new Line(new Point(20, 10), new Point(20, 20)));
        CutOutline mainOutline = new CutOutline(mainLines, false);
        
        result.setLeftCut(mainOutline);  // Only main lines, no additional lines
        result.setRightCut(mainOutline.copy());
        result.setLeftMove(mainOutline.copy());
        result.setRightMove(mainOutline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10");
        
        // Set additional lines that should be in G-code but aren't
        List<Line> additionalLines = new ArrayList<>();
        additionalLines.add(new Line(new Point(15, 10), new Point(15, 20))); // Connected to main outline
        
        sm.setLeftAdditionalLines(additionalLines);
        sm.setRightAdditionalLines(new ArrayList<>());
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        boolean gCodeCheckFailed = false;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("G-Code") && check.message.contains("additional") && check.severity != SanityCheckValidator.CheckSeverity.OK) {
                gCodeCheckFailed = true;
                break;
            }
        }
        assertTrue(gCodeCheckFailed);
        
        // Clean up
        sm.setLeftAdditionalLines(new ArrayList<>());
    }

    @Test
    void testMultipleDisconnectedIslands() {
        GCodeResult result = new GCodeResult();
        
        // Create main outline: (10,10)-(20,10)-(20,20)
        List<Line> mainLines = new ArrayList<>();
        mainLines.add(new Line(new Point(10, 10), new Point(20, 10)));
        mainLines.add(new Line(new Point(20, 10), new Point(20, 20)));
        CutOutline mainOutline = new CutOutline(mainLines, false);
        
        // Create isolated island of additional lines at (50,50)-(60,50)-(60,60)
        List<Line> isolatedLines = new ArrayList<>();
        isolatedLines.add(new Line(new Point(50, 50), new Point(60, 50)));
        isolatedLines.add(new Line(new Point(60, 50), new Point(60, 60)));
        
        // Create left cut with main and isolated lines
        List<Line> leftCutLines = new ArrayList<>(mainLines);
        leftCutLines.addAll(isolatedLines);
        CutOutline leftCutWithIslands = new CutOutline(leftCutLines, false);
        
        result.setLeftCut(leftCutWithIslands);
        result.setRightCut(mainOutline.copy());
        result.setLeftMove(mainOutline.copy());
        result.setRightMove(mainOutline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10");
        
        sm.setLeftAdditionalLines(isolatedLines);
        sm.setRightAdditionalLines(new ArrayList<>());
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Find the connection check
        SanityCheckValidator.CheckResult connectionCheck = null;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("connected")) {
                connectionCheck = check;
                break;
            }
        }
        
        assertNotNull(connectionCheck, "No connection check found");
        assertNotEquals(SanityCheckValidator.CheckSeverity.OK, connectionCheck.severity, "Connection check should fail for disconnected islands. Message: " + connectionCheck.message);
        assertTrue(connectionCheck.message.contains("island"), "Error message should mention 'island'");
        
        // Clean up
        sm.setLeftAdditionalLines(new ArrayList<>());
    }

    @Test
    void testOutlineExceedsFoamBoundariesIsWarning() {
        GCodeResult result = new GCodeResult();
        
        // Create an outline that exceeds foam boundaries
        List<Line> exceedingLines = new ArrayList<>();
        exceedingLines.add(new Line(new Point(0, 0), new Point(600, 0))); // Exceeds height of 50
        CutOutline exceedingOutline = new CutOutline(exceedingLines, false);
        
        result.setLeftCut(exceedingOutline);
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(new CutOutline(new ArrayList<>(), false));
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Check that "outlines fit" check is a WARNING, not an ERROR
        SanityCheckValidator.CheckResult fitCheck = null;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("fit in foam block")) {
                fitCheck = check;
                break;
            }
        }
        assertNotNull(fitCheck, "No outlines fit check found");
        assertNotEquals(SanityCheckValidator.CheckSeverity.OK, fitCheck.severity, "Outlines fit check should fail");
        assertEquals(SanityCheckValidator.CheckSeverity.WARNING, fitCheck.severity, "Outlines fit check should be WARNING, not ERROR");
    }

    @Test
    void testWireAnchorExceedsAnchorBoundariesIsWarning() {
        GCodeResult result = new GCodeResult();
        
        // Create anchor moves that exceed boundaries
        List<Line> exceedingLines = new ArrayList<>();
        exceedingLines.add(new Line(new Point(0, 0), new Point(600, 0))); // Exceeds height of 50
        CutOutline exceedingOutline = new CutOutline(exceedingLines, false);
        
        result.setLeftCut(new CutOutline(new ArrayList<>(), false));
        result.setRightCut(new CutOutline(new ArrayList<>(), false));
        result.setLeftMove(exceedingOutline);
        result.setRightMove(new CutOutline(new ArrayList<>(), false));
        
        List<SanityCheckValidator.CheckResult> checks = validator.performAllChecks(result);
        
        // Check that "wire anchor" check is a WARNING, not an ERROR
        SanityCheckValidator.CheckResult anchorCheck = null;
        for (SanityCheckValidator.CheckResult check : checks) {
            if (check.message.contains("anchor movements exceed")) {
                anchorCheck = check;
                break;
            }
        }
        assertNotNull(anchorCheck, "No anchor movements check found");
        assertNotEquals(SanityCheckValidator.CheckSeverity.OK, anchorCheck.severity, "Anchor movements check should fail");
        assertEquals(SanityCheckValidator.CheckSeverity.WARNING, anchorCheck.severity, "Anchor movements check should be WARNING, not ERROR");
    }

    /**
     * Helper: Creates a valid G-Code result for testing
     */
    private GCodeResult createValidGCodeResult() {
        GCodeResult result = new GCodeResult();
        
        // Create valid outlines within foam dimensions
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(new Point(10, 10), new Point(20, 10)));
        lines.add(new Line(new Point(20, 10), new Point(20, 20)));
        CutOutline outline = new CutOutline(lines, false);
        
        result.setLeftCut(outline);
        result.setRightCut(outline.copy());
        result.setLeftMove(outline.copy());
        result.setRightMove(outline.copy());
        result.setGcode("G0 X0 Y0\nG1 X10 Y10");
        
        return result;
    }
}

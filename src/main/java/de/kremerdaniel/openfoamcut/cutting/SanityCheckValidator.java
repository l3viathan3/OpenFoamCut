package de.kremerdaniel.openfoamcut.cutting;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.model.StateManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates sanity checks for G-Code generation
 */
public class SanityCheckValidator {

    private static final double EPSILON = 1.0;

    private final StateManager sm = StateManager.getInstance();

    /**
     * Severity levels for check results
     */
    public enum CheckSeverity {
        /**
         * Check passed
         */
        OK,
        /**
         * Check resulted in a warning (non-blocking issue)
         */
        WARNING,
        /**
         * Check resulted in an error (blocking issue)
         */
        ERROR
    }

    /**
     * List of check results
     */
    public static class CheckResult {
        /**
         * The check message
         */
        public final String message;
        /**
         * The severity level
         */
        public final CheckSeverity severity;

        /**
         * Constructor for a check result
         * @param message The check message
         * @param severity The severity level
         */
        public CheckResult(String message, CheckSeverity severity) {
            this.message = message;
            this.severity = severity;
        }
    }

    /**
     * Performs all sanity checks on the G-Code result
     * @param result The GCodeResult to validate
     * @return List of check results
     */
    public List<CheckResult> performAllChecks(GCodeResult result) {
        List<CheckResult> checks = new ArrayList<>();

        checks.add(checkOutlinesFitInFoam(result));
        checks.add(checkWireAnchorsDontMoveOutsideFoamDimensions(result));
        checks.add(checkAnchorPointsNotNegative(result));
        checks.add(checkAllLinesConnected(result));
        checks.add(checkGCodePassesThroughAllLines(result));
        checks.add(checkFoamDimensionsValid());
        checks.add(checkNoEmptyOutlines(result));

        return checks;
    }

    /**
     * Checks if all outlines fit within the foam block
     */
    private CheckResult checkOutlinesFitInFoam(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No G-Code generated", CheckSeverity.ERROR);
        }

        CutOutline leftCut = result.getLeftCut();
        CutOutline rightCut = result.getRightCut();

        if (leftCut == null || rightCut == null) {
            return new CheckResult("Cut outlines not available", CheckSeverity.ERROR);
        }

        double foamWidth = sm.getFoamWidth();
        double foamDepth = sm.getFoamDepth();
        double foamHeight = sm.getFoamHeight();

        boolean leftFits = fitsInBounds(leftCut, 0, foamDepth, 0, foamHeight);
        boolean rightFits = fitsInBounds(rightCut, 0, foamDepth, 0, foamHeight);

        if (leftFits && rightFits) {
            return new CheckResult("Outlines fit within foam block", CheckSeverity.OK);
        } else {
            StringBuilder msg = new StringBuilder("Outlines do NOT fit in foam block (");
            msg.append(String.format(Locale.US, "%.1f x %.1f x %.1f mm", foamWidth, foamDepth, foamHeight)).append("): ");
            if (!leftFits) {
                msg.append("LEFT outline exceeds bounds");
            }
            if (!rightFits) {
                if (!leftFits) {
                    msg.append(", ");
                }
                msg.append("RIGHT outline exceeds bounds");
            }
            return new CheckResult(msg.toString(), CheckSeverity.WARNING);
        }
    }

    /**
     * Checks if wire anchors would move outside foam block dimensions
     */
    private CheckResult checkWireAnchorsDontMoveOutsideFoamDimensions(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No move trajectory available", CheckSeverity.ERROR);
        }

        CutOutline leftMove = result.getLeftMove();
        CutOutline rightMove = result.getRightMove();

        if (leftMove == null || rightMove == null) {
            return new CheckResult("Move trajectories not available (unable to validate)", CheckSeverity.OK);
        }

        double foamDepth = sm.getFoamDepth();
        double foamHeight = sm.getFoamHeight();

        boolean leftValid = fitsInBounds(leftMove, 0, foamDepth, 0, foamHeight);
        boolean rightValid = fitsInBounds(rightMove, 0, foamDepth, 0, foamHeight);

        if (leftValid && rightValid) {
            return new CheckResult("Wire anchors stay within foam block dimensions", CheckSeverity.OK);
        } else {
            StringBuilder msg = new StringBuilder("Wire anchor movements exceed foam boundaries: ");
            if (!leftValid) {
                msg.append("LEFT anchor");
            }
            if (!rightValid) {
                if (!leftValid) {
                    msg.append(", ");
                }
                msg.append("RIGHT anchor");
            }
            return new CheckResult(msg.toString(), CheckSeverity.WARNING);
        }
    }

    /**
     * Checks if anchor points would pass through negative coordinates
     */
    private CheckResult checkAnchorPointsNotNegative(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No anchor points available", CheckSeverity.ERROR);
        }

        CutOutline leftMove = result.getLeftMove();
        CutOutline rightMove = result.getRightMove();

        if (leftMove == null || rightMove == null) {
            return new CheckResult("Anchor trajectories not available (unable to validate)", CheckSeverity.OK);
        }

        StringBuilder errors = new StringBuilder();
        collectNegativeAnchorErrors(leftMove, "LEFT", errors);
        collectNegativeAnchorErrors(rightMove, "RIGHT", errors);

        if (errors.length() == 0) {
            return new CheckResult("All anchor points have non-negative coordinates (X >= 0, Y >= 0)", CheckSeverity.OK);
        } else {
            return new CheckResult("Wire anchors would move below table or in front of origin: " + errors.toString().trim(), CheckSeverity.ERROR);
        }
    }

    /**
     * Helper: Collects negative anchor coordinate errors for a given outline
     */
    private void collectNegativeAnchorErrors(CutOutline outline, String sideName, StringBuilder errors) {
        for (Line line : outline.getLines()) {
            checkPointForNegative(line.getStart(), sideName, errors);
            checkPointForNegative(line.getEnd(), sideName, errors);
        }
    }

    /**
     * Helper: Checks if a point has negative coordinates and adds to error list
     */
    private void checkPointForNegative(Point point, String sideName, StringBuilder errors) {
        if (point.getX() < -EPSILON || point.getY() < -EPSILON) {
            errors.append(String.format(Locale.US, "%s anchor at (%.1f, %.1f) ", sideName, point.getX(), point.getY()));
        }
    }

    /**
     * Checks if all lines (main and additional) in the cut outlines are connected
     * Validates that all lines form a single connected component with no isolated islands
     */
    private CheckResult checkAllLinesConnected(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No outlines available", CheckSeverity.ERROR);
        }

        CutOutline leftCut = result.getLeftCut();
        CutOutline rightCut = result.getRightCut();

        if (leftCut == null || rightCut == null) {
            return new CheckResult("Cut outlines not available", CheckSeverity.ERROR);
        }

        // Check LEFT side connectivity
        List<Line> leftAdditional = sm.getLeftAdditionalLines();
        String leftError = checkSideConnectivity("LEFT", leftCut, leftAdditional);
        
        // Check RIGHT side connectivity
        List<Line> rightAdditional = sm.getRightAdditionalLines();
        String rightError = checkSideConnectivity("RIGHT", rightCut, rightAdditional);

        if (leftError == null && rightError == null) {
            return new CheckResult("All main and additional lines are properly connected", CheckSeverity.OK);
        } else {
            StringBuilder msg = new StringBuilder("Disconnected lines detected: ");
            if (leftError != null) {
                msg.append(leftError);
            }
            if (rightError != null) {
                if (leftError != null) {
                    msg.append(", ");
                }
                msg.append(rightError);
            }
            return new CheckResult(msg.toString(), CheckSeverity.ERROR);
        }
    }

    /**
     * Helper: Checks connectivity for one side (LEFT or RIGHT)
     * Returns null if connected, error message if disconnected
     */
    private String checkSideConnectivity(String side, CutOutline mainOutline, List<Line> additionalLines) {
        if (mainOutline == null || mainOutline.getLines().isEmpty()) {
            return null;
        }

        // Collect all lines (main + additional)
        List<Line> allLines = new ArrayList<>(mainOutline.getLines());
        if (additionalLines != null) {
            allLines.addAll(additionalLines);
        }

        // Build graph of connected endpoints
        Map<String, Set<String>> graph = buildConnectivityGraph(allLines);

        if (graph.isEmpty()) {
            return null;
        }

        // Check if all endpoints are in the same connected component
        Set<String> visited = new HashSet<>();
        String startPoint = graph.keySet().iterator().next();
        dfsVisit(startPoint, graph, visited);

        if (visited.size() == graph.size()) {
            return null; // All endpoints are connected
        } else {
            return side + " outline contains disconnected line islands";
        }
    }

    /**
     * Helper: Builds a graph where nodes are unique points and edges are lines
     */
    private Map<String, Set<String>> buildConnectivityGraph(List<Line> allLines) {
        Map<String, Set<String>> graph = new HashMap<>();

        for (Line line : allLines) {
            String startKey = pointToKey(line.getStart());
            String endKey = pointToKey(line.getEnd());

            graph.computeIfAbsent(startKey, k -> new HashSet<>()).add(endKey);
            graph.computeIfAbsent(endKey, k -> new HashSet<>()).add(startKey);
        }

        return graph;
    }

    /**
     * Helper: Converts a point to a canonical key for graph matching
     */
    private String pointToKey(Point p) {
        return String.format(Locale.US, "%.1f,%.1f", p.getX(), p.getY());
    }

    /**
     * Helper: DFS traversal to find all connected nodes
     */
    private void dfsVisit(String node, Map<String, Set<String>> graph, Set<String> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        for (String neighbor : graph.getOrDefault(node, new HashSet<>())) {
            dfsVisit(neighbor, graph, visited);
        }
    }

    /**
     * Checks if the generated G-Code passes through all lines
     */
    private CheckResult checkGCodePassesThroughAllLines(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No G-Code generated", CheckSeverity.ERROR);
        }

        CutOutline leftCut = result.getLeftCut();
        CutOutline rightCut = result.getRightCut();

        if (leftCut == null || leftCut.getLines().isEmpty()) {
            return new CheckResult("LEFT cut outline is empty", CheckSeverity.ERROR);
        }

        if (rightCut == null || rightCut.getLines().isEmpty()) {
            return new CheckResult("RIGHT cut outline is empty", CheckSeverity.ERROR);
        }
        
        List<Line> leftAdditional = sm.getLeftAdditionalLines();
        List<Line> rightAdditional = sm.getRightAdditionalLines();
        
        if (leftAdditional.isEmpty() && rightAdditional.isEmpty()) {
            return new CheckResult("G-Code will cut through all main outline lines", CheckSeverity.OK);
        }
        
        return validateAdditionalLinesInGCode(leftCut, rightCut, leftAdditional, rightAdditional);
    }

    /**
     * Helper: Validates all additional lines are in the G-Code by checking if their endpoints are covered
     */
    private CheckResult validateAdditionalLinesInGCode(CutOutline leftCut, CutOutline rightCut,
                                                       List<Line> leftAdditional, List<Line> rightAdditional) {
        boolean leftCovered = areAllEndpointsCovered(leftCut, leftAdditional);
        boolean rightCovered = areAllEndpointsCovered(rightCut, rightAdditional);
        
        if (leftCovered && rightCovered) {
            return new CheckResult("G-Code will cut through all main and additional lines", CheckSeverity.OK);
        } else {
            StringBuilder msg = new StringBuilder("Some additional lines are not included in G-Code: ");
            boolean first = true;
            
            if (!leftCovered) {
                msg.append("LEFT additional lines missing");
                first = false;
            }
            if (!rightCovered) {
                if (!first) {
                    msg.append(", ");
                }
                msg.append("RIGHT additional lines missing");
            }
            return new CheckResult(msg.toString(), CheckSeverity.ERROR);
        }
    }

    /**
     * Helper: Checks if all endpoints of the given lines are covered in the outline's cut path
     */
    private boolean areAllEndpointsCovered(CutOutline outline, List<Line> linesToCheck) {
        if (linesToCheck == null || linesToCheck.isEmpty()) {
            return true;
        }

        for (Line line : linesToCheck) {
            if (!isPointInOutline(line.getStart(), outline) || !isPointInOutline(line.getEnd(), outline)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper: Checks if a point is covered in an outline's cut path
     */
    private boolean isPointInOutline(Point point, CutOutline outline) {
        for (Line line : outline.getLines()) {
            if (pointsAreClose(point, line.getStart()) || pointsAreClose(point, line.getEnd())) {
                return true;
            }
            // Also check if point lies on the line segment
            if (pointOnLineSegment(point, line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: Checks if a point lies on a line segment
     */
    private boolean pointOnLineSegment(Point p, Line segment) {
        Point start = segment.getStart();
        Point end = segment.getEnd();
        
        // Check if point is collinear with line endpoints
        double crossProduct = (p.getY() - start.getY()) * (end.getX() - start.getX()) - 
                             (p.getX() - start.getX()) * (end.getY() - start.getY());
        
        if (Math.abs(crossProduct) > EPSILON) {
            return false;
        }
        
        // Check if point is within the bounding box of the line
        double minX = Math.min(start.getX(), end.getX());
        double maxX = Math.max(start.getX(), end.getX());
        double minY = Math.min(start.getY(), end.getY());
        double maxY = Math.max(start.getY(), end.getY());
        
        return p.getX() >= minX - EPSILON && p.getX() <= maxX + EPSILON &&
               p.getY() >= minY - EPSILON && p.getY() <= maxY + EPSILON;
    }

    /**
     * Checks if foam dimensions are valid
     */
    private CheckResult checkFoamDimensionsValid() {
        double foamWidth = sm.getFoamWidth();
        double foamDepth = sm.getFoamDepth();
        double foamHeight = sm.getFoamHeight();

        if (foamWidth > 0 && foamDepth > 0 && foamHeight > 0) {
            return new CheckResult(
                String.format(Locale.US, "Foam dimensions valid (%.1f x %.1f x %.1f mm)", foamWidth, foamDepth, foamHeight), CheckSeverity.OK);
        } else {
            StringBuilder msg = new StringBuilder("Invalid foam dimensions: ");
            if (foamWidth <= 0) {
                msg.append("Width=").append(foamWidth).append(" ");
            }
            if (foamDepth <= 0) {
                msg.append("Depth=").append(foamDepth).append(" ");
            }
            if (foamHeight <= 0) {
                msg.append("Height=").append(foamHeight).append(" ");
            }
            return new CheckResult(msg.toString().trim(), CheckSeverity.ERROR);
        }
    }

    /**
     * Checks if outlines are not empty
     */
    private CheckResult checkNoEmptyOutlines(GCodeResult result) {
        if (result == null) {
            return new CheckResult("No outlines generated", CheckSeverity.ERROR);
        }

        CutOutline leftCut = result.getLeftCut();
        CutOutline rightCut = result.getRightCut();

        StringBuilder msg = new StringBuilder();
        int emptyCount = 0;

        if (leftCut == null || leftCut.getLines().isEmpty()) {
            msg.append("LEFT ");
            emptyCount++;
        }

        if (rightCut == null || rightCut.getLines().isEmpty()) {
            if (emptyCount > 0) {
                msg.append("and ");
            }
            msg.append("RIGHT ");
            emptyCount++;
        }

        if (emptyCount > 0) {
            return new CheckResult(msg.append("outline(s) are empty").toString(), CheckSeverity.ERROR);
        }

        return new CheckResult("Both outlines contain lines to cut", CheckSeverity.OK);
    }

    /**
     * Helper: Checks if a CutOutline fits within given bounds
     */
    private boolean fitsInBounds(CutOutline outline, double minX, double maxX, double minY, double maxY) {
        if (outline == null || outline.getLines().isEmpty()) {
            return true;
        }

        for (Line line : outline.getLines()) {
            if (!pointFitsInBounds(line.getStart(), minX, maxX, minY, maxY)) {
                return false;
            }
            if (!pointFitsInBounds(line.getEnd(), minX, maxX, minY, maxY)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper: Checks if a point fits within given bounds
     */
    private boolean pointFitsInBounds(Point point, double minX, double maxX, double minY, double maxY) {
        return point.getX() >= minX - EPSILON && point.getX() <= maxX + EPSILON &&
               point.getY() >= minY - EPSILON && point.getY() <= maxY + EPSILON;
    }

    /**
     * Helper: Checks if two points are close (within EPSILON)
     */
    private boolean pointsAreClose(Point p1, Point p2) {
        return p1.distanceTo(p2) < EPSILON;
    }

    /**
     * Formats all check results as a displayable HTML string
     * @param checks List of check results
     * @return HTML formatted string with OK/WARNING/ERROR prefixes and color coding
     */
    public static String formatCheckResults(List<CheckResult> checks) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: monospace; margin: 5px;'>");

        for (CheckResult check : checks) {
            switch (check.severity) {
                case OK:
                    sb.append("<div style='color: green; white-space: nowrap;'>[OK]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                    break;
                case WARNING:
                    sb.append("<div style='color: #FF8C00; white-space: nowrap;'>[WARNING]&nbsp;");
                    break;
                case ERROR:
                default:
                    sb.append("<div style='color: red; white-space: nowrap;'>[ERROR]&nbsp;&nbsp;&nbsp;");
                    break;
            }
            sb.append(escapeHtml(check.message)).append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Escapes HTML special characters
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

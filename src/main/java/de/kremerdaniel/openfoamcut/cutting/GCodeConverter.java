package de.kremerdaniel.openfoamcut.cutting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.model.MachineManager;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can generate G-Codes
 */
@SuppressWarnings("PMD.LooseCoupling")
public class GCodeConverter {

    private static final Logger logger = LoggerFactory.getLogger(GCodeConverter.class);

    StateManager sm = StateManager.getInstance();
    RuntimeModelManager rmm = RuntimeModelManager.getInstance();
    MachineManager mm = MachineManager.getInstance();
    
    /**
     * Takes the current application state as defined by the various managers and calculates G-Code based on that.
     * @return The result of the G-Code generation
     */
    public GCodeResult generateGCodeFromState() {
        logger.info("Starting G-Code generation");

        List<Line> leftLines  = getLines(Side.LEFT);
        List<Line> rightLines = getLines(Side.RIGHT);

        List<SyncedPoints> cutOrder = getCutOrder();

        List<SyncedPoints> moveSteps = generateMoves(leftLines, rightLines, cutOrder);

        validateMoves(moveSteps);

        List<SyncedPoints> projectedPoints = projectPoints(sm.getFoamWidth(), sm.getFoamOffsetLeft(), sm.getFoamOffsetRight(), moveSteps);

        GCodeResult result = new GCodeResult(
            toGCode(projectedPoints), 
            toCutOutline(Side.LEFT, moveSteps, true), 
            toCutOutline(Side.RIGHT, moveSteps, true), 
            toCutOutline(Side.LEFT, projectedPoints, false),
            toCutOutline(Side.RIGHT, projectedPoints, false));

        // Perform sanity checks
        SanityCheckValidator validator = new SanityCheckValidator();
        var checkResults = validator.performAllChecks(result);
        String formattedResults = SanityCheckValidator.formatCheckResults(checkResults);
        result.setSanityCheckResults(formattedResults);
        result.setSanityCheckList(checkResults);

        logger.info("G-Code generation completed");
        return result;
    }

    private List<SyncedPoints> projectPoints(double foamWidth, double offsetLeft, double offsetRight, List<SyncedPoints> moveSteps) {
        List<SyncedPoints> result = new ArrayList<>();

        if (moveSteps == null || foamWidth == 0.0) {
            return result; // edge case protection (empty result or invalid width)
        }

        for (SyncedPoints moveStep : moveSteps) {
            Point leftFace = moveStep.getLeft();
            Point rightFace = moveStep.getRight();

            // Slope of the straight wire line (independent for X and Y coordinates)
            double dx = rightFace.getX() - leftFace.getX();
            double dy = rightFace.getY() - leftFace.getY();
            double slopeX = dx / foamWidth;
            double slopeY = dy / foamWidth;

            // Extrapolate left anchor: go backwards from left face by offsetLeft
            Point leftAnchor = new Point(leftFace.getX(), leftFace.getY())
                    .offset(-slopeX * offsetLeft, -slopeY * offsetLeft);

            // Extrapolate right anchor: go forwards from right face by offsetRight
            Point rightAnchor = new Point(rightFace.getX(), rightFace.getY())
                    .offset(slopeX * offsetRight, slopeY * offsetRight);

            result.add(new SyncedPoints(leftAnchor, rightAnchor));
        }

        return result;
    }

    private CutOutline toCutOutline(Side side, List<SyncedPoints> moveSteps, boolean normalize) {
        List<Point> points;
        if(side == Side.LEFT) {
            points = moveSteps.stream().map(SyncedPoints::getLeft).collect(Collectors.toList());
        } else {
            points = moveSteps.stream().map(SyncedPoints::getRight).collect(Collectors.toList());
        }

        if(points.size() < 2) {
            return new CutOutline();
        }

        List<Line> lines = new ArrayList<>();
        Point previous = points.get(0);
        for(int i=1; i<points.size(); i++) {
            Point thisPoint = points.get(i);
            Line l = new Line(new Point(previous.getX(), previous.getY()), new Point(thisPoint.getX(), thisPoint.getY()));

            lines.add(l);
            previous = thisPoint;
        }
        
        return new CutOutline(lines, normalize);
    }

    private void validateMoves(List<SyncedPoints> moves) {
        for (int i = 1; i < moves.size(); i++) {
            double dl = moves.get(i-1).getLeft().distanceTo(moves.get(i).getLeft());
            double dr = moves.get(i-1).getRight().distanceTo(moves.get(i).getRight());

            if (dl > 50 || dr > 50) { // threshold in mm
                throw new IllegalStateException("Jump detected at step " + i);
            }
        }
    }

    private List<SyncedPoints> generateMoves(
            List<Line> leftLines,
            List<Line> rightLines,
            List<SyncedPoints> cutOrder) {

        List<SyncedPoints> result = new ArrayList<>();
        Set<Line> usedLeft = new HashSet<>();
        Set<Line> usedRight = new HashSet<>();

        for (int i = 0; i < cutOrder.size() - 1; i++) {
            SyncedPoints from = cutOrder.get(i);
            SyncedPoints to = cutOrder.get(i + 1);

            // Find raw paths
            List<Line> leftPath = findPath(from.getLeft(), to.getLeft(), leftLines, usedLeft);
            List<Line> rightPath = findPath(from.getRight(), to.getRight(), rightLines, usedRight);

            usedLeft.addAll(leftPath);
            usedRight.addAll(rightPath);

            // Enforce correct direction
            leftPath = orientPath(leftPath, from.getLeft());
            rightPath = orientPath(rightPath, from.getRight());

            // Convert to segments
            List<ParamSegment> leftSegs = toSegments(leftPath);
            List<ParamSegment> rightSegs = toSegments(rightPath);

            // Better step calculation
            double leftTotal = totalLength(leftSegs);
            double rightTotal = totalLength(rightSegs);
            double maxLen = Math.max(leftTotal, rightTotal);

            int steps = (int)(maxLen / 1.0); // 1mm resolution
            steps = Math.max(steps, 10);

            // Interpolate
            List<SyncedPoints> segment = interpolateSync(leftSegs, rightSegs, steps);

            // Stitch into global result
            appendContinuous(result, segment);
        }

        // 7️⃣ Optional cleanup
        return deduplicate(result);
    }

    double cost(Line l, Point from, Point start, Point to, Set<Line> used) {
        double base = l.getStart().distanceTo(l.getEnd());

        double reusePenalty = used.contains(l) ? 1000 : 0;

        // direction penalty
        Point next = nextPoint(l, from);
        double before = from.distanceTo(to);
        double after  = next.distanceTo(to);

        double backwardPenalty = after > before ? 500 : 0;

        // Prefer upper paths based on overall position relative to straight line from start to target
        double totalDistance = start.distanceTo(to);
        if (totalDistance > 0) {
            double distanceTraveled = start.distanceTo(from);
            double progress = distanceTraveled / totalDistance;  // 0 at start, 1 at target
            
            // Expected Y based on linear interpolation from start to target
            double expectedY = start.getY() + (to.getY() - start.getY()) * progress;
            
            // Penalize positions below the expected line (encourages upper paths)
            double yDeviation = expectedY - next.getY();  // positive when below line
            double pathHeightPenalty = Math.max(0, yDeviation) * 10;
            
            return base + reusePenalty + backwardPenalty + pathHeightPenalty;
        }

        return base + reusePenalty + backwardPenalty;
    }

    private double totalLength(List<ParamSegment> segs) {
        double sum = 0;
        for (ParamSegment s : segs) {
            sum += s.length;
        }
        return sum;
    }

    private List<SyncedPoints> deduplicate(List<SyncedPoints> input) {
        List<SyncedPoints> out = new ArrayList<>();

        for (SyncedPoints p : input) {
            if (out.isEmpty() || !out.get(out.size()-1).same(p)) {
                out.add(p);
            }
        }

        return out;
    }

    // dijkstra
    private List<Line> findPath(
            Point start,
            Point target,
            List<Line> lines,
            Set<Line> used) {

        Map<Point, List<Line>> graph = buildGraph(lines);

        Map<Point, Double> dist = new HashMap<>();
        Map<Point, Line> prevEdge = new HashMap<>();

        PriorityQueue<Point> pq = new PriorityQueue<>(
            Comparator.comparingDouble(dist::get)
        );

        dist.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            Point current = pq.poll();

            if (current.toCanonical().equals(target.toCanonical())) {
                break;
            }
                
            for (Line l : graph.getOrDefault(current, List.of())) {
                Point next = nextPoint(l, current);

                double newDist = dist.get(current)
                    + cost(l, current, start, target, used);

                if (newDist < dist.getOrDefault(next, Double.POSITIVE_INFINITY)) {
                    dist.put(next, newDist);
                    prevEdge.put(next, l);
                    pq.add(next);
                }
            }
        }

        return reconstructPath(prevEdge, start, target);
    }

    private Point nextPoint(Line l, Point current) {
        Point a = l.getStart().toCanonical();
        Point b = l.getEnd().toCanonical();
        Point c = current.toCanonical();

        if (a.equals(c)) {
            return b;
        }
        if (b.equals(c)) {
            return a;
        }

        throw new IllegalStateException(
            "Line is not connected to current point: " + l + " current=" + current
        );
    }

    private Map<Point, List<Line>> buildGraph(List<Line> lines) {
        Map<Point, List<Line>> graph = new HashMap<>();

        for (Line l : lines) {
            Point a = l.getStart().toCanonical();
            Point b = l.getEnd().toCanonical();

            graph.computeIfAbsent(a, k -> new ArrayList<>()).add(l);
            graph.computeIfAbsent(b, k -> new ArrayList<>()).add(l);
        }

        return graph;
    }

    private List<Line> reconstructPath(
            Map<Point, Line> prev,
            Point start,
            Point target) {

        List<Line> path = new ArrayList<>();
        Point current = target;

        while (!current.toCanonical().equals(start.toCanonical())) {
            Line l = prev.get(current);
            if (l == null) {
                break;
            }

            path.add(l);

            current = l.getStart().toCanonical().equals(current.toCanonical())
                    ? l.getEnd()
                    : l.getStart();
        }

        Collections.reverse(path);
        return path;
    }

    private List<Line> orientPath(List<Line> path, Point start) {
        List<Line> result = new ArrayList<>();

        Point current = start.toCanonical();

        for (Line l : path) {
            Point a = l.getStart().toCanonical();
            Point b = l.getEnd().toCanonical();

            if (a.equals(current)) {
                result.add(l);
                current = b;
            } else if (b.equals(current)) {
                result.add(new Line(l.getEnd(), l.getStart())); // reverse
                current = a;
            } else {
                throw new IllegalStateException("Disconnected path");
            }
        }

        return result;
    }

    private void appendContinuous(
            List<SyncedPoints> result,
            List<SyncedPoints> segment) {

        if (result.isEmpty()) {
            result.addAll(segment);
            return;
        }

        SyncedPoints last = result.get(result.size() - 1);
        SyncedPoints first = segment.get(0);

        if (!last.same(first)) {
            // Insert transition move (or throw if not allowed)
            result.add(first);
        }

        // Skip duplicate first point
        result.addAll(segment.subList(1, segment.size()));
    }

    private static class ParamSegment {
        Point a;
        Point b;
        double length;

        ParamSegment(Point a, Point b) {
            this.a = a;
            this.b = b;
            this.length = a.distanceTo(b);
        }

        Point interpolate(double t) {
            return new Point(
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t
            );
        }
    }

    private List<ParamSegment> toSegments(List<Line> lines) {
        List<ParamSegment> segs = new ArrayList<>();
        for (Line l : lines) {
            segs.add(new ParamSegment(l.getStart(), l.getEnd()));
        }
        return segs;
    }

    private List<SyncedPoints> interpolateSync(
            List<ParamSegment> leftSegs,
            List<ParamSegment> rightSegs,
            int steps) {

        double[] leftCum = cumulativeLengths(leftSegs);
        double[] rightCum = cumulativeLengths(rightSegs);

        double leftTotal = leftCum[leftCum.length - 1];
        double rightTotal = rightCum[rightCum.length - 1];

        List<SyncedPoints> result = new ArrayList<>();

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;

            Point lp = sample(leftSegs, leftCum, t * leftTotal);
            Point rp = sample(rightSegs, rightCum, t * rightTotal);

            result.add(new SyncedPoints(lp, rp));
        }

        return result;
    }

    private Point sample(List<ParamSegment> segs, double[] cum, double dist) {
        if(segs.isEmpty()) {
            return new Point();
        }
        for (int i = 0; i < segs.size(); i++) {
            if (dist <= cum[i + 1]) {
                double local = dist - cum[i];
                double t = segs.get(i).length == 0 ? 0 : local / segs.get(i).length;
                return segs.get(i).interpolate(t);
            }
        }
        return segs.get(segs.size() - 1).b;
    }

    private double[] cumulativeLengths(List<ParamSegment> segs) {
        double[] cum = new double[segs.size() + 1];
        cum[0] = 0.0;

        for (int i = 0; i < segs.size(); i++) {
            cum[i + 1] = cum[i] + segs.get(i).length;
        }

        return cum;
    }
    // END ACTUAL COMPUTATION


    private String toGCode(List<SyncedPoints> moveSteps) {
        StringBuilder sb = new StringBuilder();
        sb.append(mm.getStartCode());
        sb.append("\n");
        for(SyncedPoints sp : moveSteps) {
            String out = mm.getLineCode().replaceAll("\\{x1\\}", String.format("%.3f", sp.getLeft().getX()));
            out = out.replaceAll("\\{y1\\}", String.format("%.3f", sp.getLeft().getY()));
            out = out.replaceAll("\\{x2\\}", String.format("%.3f", sp.getRight().getX()));
            out = out.replaceAll("\\{y2\\}", String.format("%.3f", sp.getRight().getY()));
            sb.append(out);
            sb.append("\n");
        }
        sb.append(mm.getEndCode());
        sb.append("\n");
        return sb.toString();
    }

    private List<Line> getLines(Side side) {
        List<Line> list = new ArrayList<>();

        if(rmm.getOutline(side) == null) {
            return list;
        }

        double offsetX = sm.getOffsetX(side) + sm.getFoamOffsetX();
        double offsetY = sm.getOffsetY(side);
        
        for(Line l : rmm.getOutline(side).getLines()) {
            list.add(new Line(
                new Point(l.getStart().getX(), l.getStart().getY()).offset(offsetX, offsetY),
                new Point(l.getEnd().getX(), l.getEnd().getY()).offset(offsetX, offsetY)
            ).toCanonical());
        }

        for(Line l : sm.getAdditionalLines(side)) {
            list.add(l.toCanonical());
        }

        return list;
    }

    private List<SyncedPoints> getCutOrder() {
        List<SyncedPoints> list = new ArrayList<>();

        for(SyncedPoints sp : sm.getCutOrder()) {
            list.add(new SyncedPoints(
                new Point(sp.getLeft().getX(), sp.getLeft().getY()).toCanonical(),
                new Point(sp.getRight().getX(), sp.getRight().getY()).toCanonical()
            ));
        }

        return list;
    }

}

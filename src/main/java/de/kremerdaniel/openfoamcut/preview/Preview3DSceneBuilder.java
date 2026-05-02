package de.kremerdaniel.openfoamcut.preview;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.model.StateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the geometry used by the 3D preview.
 */
public final class Preview3DSceneBuilder {

    private static final double EPS = 0.01;
    private static final double TABLE_MARGIN = 60.0;
    private static final double TABLE_Z = -2.0;
    private static final double DEFAULT_MESH_STEP = 5.0;

    /**
     * Builds a preview scene from the current G-Code result and state.
     * @param result G-code result with final cut outlines
     * @param stateManager Current application state
     * @return Scene data for rendering
     */
    public Preview3DScene buildScene(GCodeResult result, StateManager stateManager) {
        return buildScene(result, stateManager, DEFAULT_MESH_STEP);
    }

    /**
     * Builds a preview scene from the current G-Code result and state.
     * @param result G-code result with final cut outlines
     * @param stateManager Current application state
     * @param meshStep Desired maximum distance between sampled mesh sections
     * @return Scene data for rendering
     */
    public Preview3DScene buildScene(GCodeResult result, StateManager stateManager, double meshStep) {
        if (result == null) {
            throw new IllegalStateException("Generate a valid geometry before opening the 3D preview.");
        }

        List<Point> leftProfile = normalizeProfile(extractOrderedPoints(result.getLeftCut()));
        List<Point> rightProfile = normalizeProfile(extractOrderedPoints(result.getRightCut()));
        int pointCount = Math.min(leftProfile.size(), rightProfile.size());

        if (pointCount < 3) {
            throw new IllegalStateException("The current cut order does not define a closed 3D geometry.");
        }

        leftProfile = new ArrayList<>(leftProfile.subList(0, pointCount));
        rightProfile = new ArrayList<>(rightProfile.subList(0, pointCount));

        double foamLeftX = stateManager.getFoamOffsetLeft();
        double foamRightX = foamLeftX + stateManager.getFoamWidth();
        List<Preview3DScene.Vec3> leftVertices = toVertices(leftProfile, foamLeftX);
        List<Preview3DScene.Vec3> rightVertices = toVertices(rightProfile, foamRightX);
        SampledProfiles sampledProfiles = sampleProfiles(leftProfile, rightProfile, meshStep);
        List<Preview3DScene.Vec3> leftMeshVertices = toVertices(sampledProfiles.leftProfile(), foamLeftX);
        List<Preview3DScene.Vec3> rightMeshVertices = toVertices(sampledProfiles.rightProfile(), foamRightX);

        List<Preview3DScene.Edge3D> tableEdges = buildTableEdges(stateManager);
        List<Preview3DScene.Edge3D> foamEdges = buildFoamEdges(stateManager);
        List<Preview3DScene.Edge3D> meshEdges = buildMeshEdges(leftMeshVertices, rightMeshVertices);
        List<Preview3DScene.Triangle3D> solidFaces = buildSolidFaces(leftProfile, leftVertices, rightVertices);

        Preview3DScene.Bounds3D bounds = computeBounds(tableEdges, foamEdges, meshEdges, solidFaces);
        return new Preview3DScene(tableEdges, foamEdges, meshEdges, solidFaces, bounds);
    }

    private List<Point> extractOrderedPoints(CutOutline outline) {
        List<Point> points = new ArrayList<>();
        if (outline == null || outline.getLines().isEmpty()) {
            return points;
        }

        List<Line> lines = outline.getLines();
        points.add(lines.get(0).getStart().copy());
        for (Line line : lines) {
            points.add(line.getEnd().copy());
        }
        return points;
    }

    private List<Point> normalizeProfile(List<Point> points) {
        List<Point> normalized = new ArrayList<>();
        for (Point point : points) {
            if (normalized.isEmpty() || normalized.get(normalized.size() - 1).distanceTo(point) > EPS) {
                normalized.add(point.copy());
            }
        }

        if (normalized.size() > 1 && normalized.get(0).distanceTo(normalized.get(normalized.size() - 1)) < EPS) {
            normalized.remove(normalized.size() - 1);
        }

        if (polygonArea(normalized) < 0) {
            Collections.reverse(normalized);
        }

        return normalized;
    }

    private double polygonArea(List<Point> polygon) {
        if (polygon.size() < 3) {
            return 0;
        }

        double area = 0;
        for (int index = 0; index < polygon.size(); index++) {
            Point current = polygon.get(index);
            Point next = polygon.get((index + 1) % polygon.size());
            area += current.getX() * next.getY() - next.getX() * current.getY();
        }
        return area / 2.0;
    }

    private List<Preview3DScene.Vec3> toVertices(List<Point> profile, double x) {
        List<Preview3DScene.Vec3> vertices = new ArrayList<>(profile.size());
        for (Point point : profile) {
            vertices.add(new Preview3DScene.Vec3(x, point.getX(), point.getY()));
        }
        return vertices;
    }

    private SampledProfiles sampleProfiles(List<Point> leftProfile, List<Point> rightProfile, double meshStep) {
        double effectiveMeshStep = Math.max(0.1, meshStep);
        int pointCount = Math.min(leftProfile.size(), rightProfile.size());
        if (pointCount < 3) {
            return new SampledProfiles(leftProfile, rightProfile);
        }

        double totalDistance = 0.0;
        for (int index = 0; index < pointCount; index++) {
            int next = (index + 1) % pointCount;
            double leftDistance = leftProfile.get(index).distanceTo(leftProfile.get(next));
            double rightDistance = rightProfile.get(index).distanceTo(rightProfile.get(next));
            totalDistance += Math.max(leftDistance, rightDistance);
        }

        int sampleCount = Math.max(3, (int) Math.ceil(totalDistance / effectiveMeshStep));
        List<Point> sampledLeft = new ArrayList<>(sampleCount);
        List<Point> sampledRight = new ArrayList<>(sampleCount);
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            double distance = totalDistance * sampleIndex / sampleCount;
            SyncedSample syncedSample = sampleAtDistance(leftProfile, rightProfile, distance);
            sampledLeft.add(syncedSample.left());
            sampledRight.add(syncedSample.right());
        }

        return new SampledProfiles(sampledLeft, sampledRight);
    }

    private SyncedSample sampleAtDistance(List<Point> leftProfile, List<Point> rightProfile, double distance) {
        int pointCount = Math.min(leftProfile.size(), rightProfile.size());
        double remaining = distance;

        for (int index = 0; index < pointCount; index++) {
            int next = (index + 1) % pointCount;
            Point leftCurrent = leftProfile.get(index);
            Point leftNext = leftProfile.get(next);
            Point rightCurrent = rightProfile.get(index);
            Point rightNext = rightProfile.get(next);
            double leftDistance = leftCurrent.distanceTo(leftNext);
            double rightDistance = rightCurrent.distanceTo(rightNext);
            double segmentLength = Math.max(leftDistance, rightDistance);

            if (segmentLength < EPS) {
                continue;
            }

            if (remaining <= segmentLength) {
                double ratio = remaining / segmentLength;
                return new SyncedSample(
                    interpolate(leftCurrent, leftNext, ratio),
                    interpolate(rightCurrent, rightNext, ratio)
                );
            }
            remaining -= segmentLength;
        }

        return new SyncedSample(leftProfile.get(0).copy(), rightProfile.get(0).copy());
    }

    private Point interpolate(Point start, Point end, double ratio) {
        return new Point(
            start.getX() + (end.getX() - start.getX()) * ratio,
            start.getY() + (end.getY() - start.getY()) * ratio
        );
    }

    private List<Preview3DScene.Edge3D> buildTableEdges(StateManager stateManager) {
        List<Preview3DScene.Edge3D> edges = new ArrayList<>();
        double totalWidth = stateManager.getFoamOffsetLeft() + stateManager.getFoamWidth() + stateManager.getFoamOffsetRight();
        double tableDepth = stateManager.getFoamOffsetX() + stateManager.getFoamDepth() + TABLE_MARGIN;

        Preview3DScene.Vec3 frontLeft = new Preview3DScene.Vec3(0, 0, TABLE_Z);
        Preview3DScene.Vec3 frontRight = new Preview3DScene.Vec3(totalWidth, 0, TABLE_Z);
        Preview3DScene.Vec3 backRight = new Preview3DScene.Vec3(totalWidth, tableDepth, TABLE_Z);
        Preview3DScene.Vec3 backLeft = new Preview3DScene.Vec3(0, tableDepth, TABLE_Z);

        addEdgeLoop(edges, List.of(frontLeft, frontRight, backRight, backLeft));

        int slatCount = 4;
        for (int index = 1; index <= slatCount; index++) {
            double y = tableDepth * index / (slatCount + 1);
            edges.add(new Preview3DScene.Edge3D(
                new Preview3DScene.Vec3(0, y, TABLE_Z),
                new Preview3DScene.Vec3(totalWidth, y, TABLE_Z)
            ));
        }

        return edges;
    }

    private List<Preview3DScene.Edge3D> buildFoamEdges(StateManager stateManager) {
        List<Preview3DScene.Edge3D> edges = new ArrayList<>();

        double x0 = stateManager.getFoamOffsetLeft();
        double x1 = x0 + stateManager.getFoamWidth();
        double y0 = stateManager.getFoamOffsetX();
        double y1 = y0 + stateManager.getFoamDepth();
        double z0 = 0;
        double z1 = stateManager.getFoamHeight();

        Preview3DScene.Vec3 leftFrontBottom = new Preview3DScene.Vec3(x0, y0, z0);
        Preview3DScene.Vec3 rightFrontBottom = new Preview3DScene.Vec3(x1, y0, z0);
        Preview3DScene.Vec3 rightBackBottom = new Preview3DScene.Vec3(x1, y1, z0);
        Preview3DScene.Vec3 leftBackBottom = new Preview3DScene.Vec3(x0, y1, z0);
        Preview3DScene.Vec3 leftFrontTop = new Preview3DScene.Vec3(x0, y0, z1);
        Preview3DScene.Vec3 rightFrontTop = new Preview3DScene.Vec3(x1, y0, z1);
        Preview3DScene.Vec3 rightBackTop = new Preview3DScene.Vec3(x1, y1, z1);
        Preview3DScene.Vec3 leftBackTop = new Preview3DScene.Vec3(x0, y1, z1);

        addEdgeLoop(edges, List.of(leftFrontBottom, rightFrontBottom, rightBackBottom, leftBackBottom));
        addEdgeLoop(edges, List.of(leftFrontTop, rightFrontTop, rightBackTop, leftBackTop));

        edges.add(new Preview3DScene.Edge3D(leftFrontBottom, leftFrontTop));
        edges.add(new Preview3DScene.Edge3D(rightFrontBottom, rightFrontTop));
        edges.add(new Preview3DScene.Edge3D(rightBackBottom, rightBackTop));
        edges.add(new Preview3DScene.Edge3D(leftBackBottom, leftBackTop));

        return edges;
    }

    private List<Preview3DScene.Edge3D> buildMeshEdges(List<Preview3DScene.Vec3> leftVertices,
                                                       List<Preview3DScene.Vec3> rightVertices) {
        List<Preview3DScene.Edge3D> edges = new ArrayList<>();
        addEdgeLoop(edges, leftVertices);
        addEdgeLoop(edges, rightVertices);

        for (int index = 0; index < leftVertices.size(); index++) {
            edges.add(new Preview3DScene.Edge3D(leftVertices.get(index), rightVertices.get(index)));
        }

        return edges;
    }

    private List<Preview3DScene.Triangle3D> buildSolidFaces(List<Point> profile,
                                                             List<Preview3DScene.Vec3> leftVertices,
                                                             List<Preview3DScene.Vec3> rightVertices) {
        List<Preview3DScene.Triangle3D> triangles = new ArrayList<>();
        List<int[]> triangulation = triangulatePolygon(profile);

        for (int index = 0; index < leftVertices.size(); index++) {
            int next = (index + 1) % leftVertices.size();
            triangles.add(new Preview3DScene.Triangle3D(
                leftVertices.get(index),
                rightVertices.get(index),
                rightVertices.get(next),
                Preview3DScene.SurfaceType.SIDE
            ));
            triangles.add(new Preview3DScene.Triangle3D(
                leftVertices.get(index),
                rightVertices.get(next),
                leftVertices.get(next),
                Preview3DScene.SurfaceType.SIDE
            ));
        }

        for (int[] triangle : triangulation) {
            triangles.add(new Preview3DScene.Triangle3D(
                leftVertices.get(triangle[0]),
                leftVertices.get(triangle[1]),
                leftVertices.get(triangle[2]),
                Preview3DScene.SurfaceType.LEFT_CAP
            ));
            triangles.add(new Preview3DScene.Triangle3D(
                rightVertices.get(triangle[2]),
                rightVertices.get(triangle[1]),
                rightVertices.get(triangle[0]),
                Preview3DScene.SurfaceType.RIGHT_CAP
            ));
        }

        return triangles;
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private List<int[]> triangulatePolygon(List<Point> polygon) {
        List<int[]> triangles = new ArrayList<>();
        if (polygon.size() == 3) {
            triangles.add(new int[]{0, 1, 2});
            return triangles;
        }

        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < polygon.size(); index++) {
            indices.add(index);
        }

        int guard = polygon.size() * polygon.size();
        while (indices.size() > 3 && guard-- > 0) {
            boolean clipped = false;
            for (int position = 0; position < indices.size(); position++) {
                int previousIndex = indices.get((position - 1 + indices.size()) % indices.size());
                int currentIndex = indices.get(position);
                int nextIndex = indices.get((position + 1) % indices.size());

                Point previous = polygon.get(previousIndex);
                Point current = polygon.get(currentIndex);
                Point next = polygon.get(nextIndex);

                if (!isConvex(previous, current, next)) {
                    continue;
                }

                boolean containsPoint = false;
                for (Integer candidate : indices) {
                    if (candidate == previousIndex || candidate == currentIndex || candidate == nextIndex) {
                        continue;
                    }
                    if (pointInTriangle(polygon.get(candidate), previous, current, next)) {
                        containsPoint = true;
                        break;
                    }
                }

                if (!containsPoint) {
                    triangles.add(new int[]{previousIndex, currentIndex, nextIndex});
                    indices.remove(position);
                    clipped = true;
                    break;
                }
            }

            if (!clipped) {
                triangles.clear();
                for (int index = 1; index < polygon.size() - 1; index++) {
                    triangles.add(new int[]{0, index, index + 1});
                }
                return triangles;
            }
        }

        if (indices.size() == 3) {
            triangles.add(new int[]{indices.get(0), indices.get(1), indices.get(2)});
        }

        return triangles;
    }

    private boolean isConvex(Point previous, Point current, Point next) {
        return cross(previous, current, next) > EPS;
    }

    private double cross(Point previous, Point current, Point next) {
        double ax = current.getX() - previous.getX();
        double ay = current.getY() - previous.getY();
        double bx = next.getX() - current.getX();
        double by = next.getY() - current.getY();
        return ax * by - ay * bx;
    }

    private boolean pointInTriangle(Point point, Point a, Point b, Point c) {
        double d1 = sign(point, a, b);
        double d2 = sign(point, b, c);
        double d3 = sign(point, c, a);

        boolean hasNegative = d1 < -EPS || d2 < -EPS || d3 < -EPS;
        boolean hasPositive = d1 > EPS || d2 > EPS || d3 > EPS;
        return !(hasNegative && hasPositive);
    }

    private double sign(Point p1, Point p2, Point p3) {
        return (p1.getX() - p3.getX()) * (p2.getY() - p3.getY())
            - (p2.getX() - p3.getX()) * (p1.getY() - p3.getY());
    }

    private void addEdgeLoop(List<Preview3DScene.Edge3D> edges, List<Preview3DScene.Vec3> vertices) {
        for (int index = 0; index < vertices.size(); index++) {
            Preview3DScene.Vec3 start = vertices.get(index);
            Preview3DScene.Vec3 end = vertices.get((index + 1) % vertices.size());
            edges.add(new Preview3DScene.Edge3D(start, end));
        }
    }

    private Preview3DScene.Bounds3D computeBounds(List<Preview3DScene.Edge3D> tableEdges,
                                                  List<Preview3DScene.Edge3D> foamEdges,
                                                  List<Preview3DScene.Edge3D> meshEdges,
                                                  List<Preview3DScene.Triangle3D> solidFaces) {
        List<Preview3DScene.Vec3> vertices = new ArrayList<>();
        collectEdgeVertices(vertices, tableEdges);
        collectEdgeVertices(vertices, foamEdges);
        collectEdgeVertices(vertices, meshEdges);
        for (Preview3DScene.Triangle3D triangle : solidFaces) {
            vertices.add(triangle.getA());
            vertices.add(triangle.getB());
            vertices.add(triangle.getC());
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (Preview3DScene.Vec3 vertex : vertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            minZ = Math.min(minZ, vertex.getZ());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        return new Preview3DScene.Bounds3D(
            new Preview3DScene.Vec3(minX, minY, minZ),
            new Preview3DScene.Vec3(maxX, maxY, maxZ)
        );
    }

    private void collectEdgeVertices(List<Preview3DScene.Vec3> vertices, List<Preview3DScene.Edge3D> edges) {
        for (Preview3DScene.Edge3D edge : edges) {
            vertices.add(edge.getStart());
            vertices.add(edge.getEnd());
        }
    }

    private record SampledProfiles(List<Point> leftProfile, List<Point> rightProfile) {
    }

    private record SyncedSample(Point left, Point right) {
    }
}
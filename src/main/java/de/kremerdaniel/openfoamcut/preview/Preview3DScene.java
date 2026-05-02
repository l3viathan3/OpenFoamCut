package de.kremerdaniel.openfoamcut.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable scene data for the 3D preview.
 */
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:JavadocVariable"})
public final class Preview3DScene {

    private final List<Edge3D> tableEdges;
    private final List<Edge3D> foamEdges;
    private final List<Edge3D> meshEdges;
    private final List<Triangle3D> solidFaces;
    private final Bounds3D bounds;

    /**
     * Creates a new scene.
     * @param tableEdges Table outline edges
     * @param foamEdges Foam block outline edges
     * @param meshEdges Approximate mesh edges for the cut solid
     * @param solidFaces Triangles for the closed solid rendering mode
     * @param bounds Overall scene bounds
     */
    public Preview3DScene(List<Edge3D> tableEdges,
                          List<Edge3D> foamEdges,
                          List<Edge3D> meshEdges,
                          List<Triangle3D> solidFaces,
                          Bounds3D bounds) {
        this.tableEdges = Collections.unmodifiableList(new ArrayList<>(tableEdges));
        this.foamEdges = Collections.unmodifiableList(new ArrayList<>(foamEdges));
        this.meshEdges = Collections.unmodifiableList(new ArrayList<>(meshEdges));
        this.solidFaces = Collections.unmodifiableList(new ArrayList<>(solidFaces));
        this.bounds = bounds;
    }

    public List<Edge3D> getTableEdges() {
        return tableEdges;
    }

    public List<Edge3D> getFoamEdges() {
        return foamEdges;
    }

    public List<Edge3D> getMeshEdges() {
        return meshEdges;
    }

    public List<Triangle3D> getSolidFaces() {
        return solidFaces;
    }

    public Bounds3D getBounds() {
        return bounds;
    }

    /**
     * 3D vector.
     */
    public static final class Vec3 {
        private final double x;
        private final double y;
        private final double z;

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }
    }

    /**
     * Line segment in 3D.
     */
    public static final class Edge3D {
        private final Vec3 start;
        private final Vec3 end;

        public Edge3D(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }

        public Vec3 getStart() {
            return start;
        }

        public Vec3 getEnd() {
            return end;
        }
    }

    /**
     * Triangle in 3D.
     */
    public static final class Triangle3D {
        private final Vec3 a;
        private final Vec3 b;
        private final Vec3 c;
        private final SurfaceType surfaceType;

        public Triangle3D(Vec3 a, Vec3 b, Vec3 c, SurfaceType surfaceType) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.surfaceType = surfaceType;
        }

        public Vec3 getA() {
            return a;
        }

        public Vec3 getB() {
            return b;
        }

        public Vec3 getC() {
            return c;
        }

        public SurfaceType getSurfaceType() {
            return surfaceType;
        }
    }

    /**
     * Surface roles for shading.
     */
    public enum SurfaceType {
        SIDE,
        LEFT_CAP,
        RIGHT_CAP
    }

    /**
     * Axis-aligned bounds.
     */
    public static final class Bounds3D {
        private final Vec3 min;
        private final Vec3 max;

        public Bounds3D(Vec3 min, Vec3 max) {
            this.min = min;
            this.max = max;
        }

        public Vec3 getMin() {
            return min;
        }

        public Vec3 getMax() {
            return max;
        }

        public double getSpanX() {
            return max.getX() - min.getX();
        }

        public double getSpanY() {
            return max.getY() - min.getY();
        }

        public double getSpanZ() {
            return max.getZ() - min.getZ();
        }

        public double getCenterX() {
            return (min.getX() + max.getX()) / 2.0;
        }

        public double getCenterY() {
            return (min.getY() + max.getY()) / 2.0;
        }

        public double getCenterZ() {
            return (min.getZ() + max.getZ()) / 2.0;
        }
    }
}
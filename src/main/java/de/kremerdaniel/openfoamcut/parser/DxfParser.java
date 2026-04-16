package de.kremerdaniel.openfoamcut.parser;

import org.kabeja.dxf.DXFArc;
import org.kabeja.dxf.DXFCircle;
import org.kabeja.dxf.DXFConstants;
import org.kabeja.dxf.DXFDocument;
import org.kabeja.dxf.DXFEllipse;
import org.kabeja.dxf.DXFEntity;
import org.kabeja.dxf.DXFLine;
import org.kabeja.dxf.DXFSpline;
import org.kabeja.dxf.helpers.Point;
import org.kabeja.dxf.DXFLayer;
import org.kabeja.parser.DXFParser;
import org.kabeja.parser.ParseException;
import org.kabeja.parser.Parser;
import org.kabeja.parser.ParserBuilder;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.UserErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DXF loading methods
 */
public final class DxfParser {

    private static final Logger logger = LoggerFactory.getLogger(DxfParser.class);

    @SuppressWarnings("PMD.LooseCoupling")
    private static final Map<Path, CachedCutOutline> CACHE = new ConcurrentHashMap<>();

    /**
     * Loads a DXF file, if already loaded from cache
     * @param dxfFilePath The absolute path to the DXF file
     * @return Loaded DXF file
     * @throws UserErrorException If there was an error the user should know about
     */
    public static CutOutline loadCutOutline(String dxfFilePath) throws UserErrorException {
        Path path = Paths.get(dxfFilePath).toAbsolutePath().normalize();

        FileTime fileTime;
        try {
            fileTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            throw new UserErrorException("Could not load file " + path, e);
        }
        long lastModified = fileTime.toMillis();

        CachedCutOutline cached = CACHE.get(path);

        if (cached != null && cached.lastModified == lastModified) {
            // Cache hit
            return cached.outline;
        }

        // Cache miss or file changed → reload
        CutOutline outline;
        try {
            outline = loadCutOutlineInternal(path.toString());
        } catch (Exception e) {
            throw new UserErrorException("Could not load file " + path, e);
        }

        CACHE.put(path, new CachedCutOutline(outline, lastModified));
        return outline;
    }
    
    @SuppressWarnings("unchecked")
    private static CutOutline loadCutOutlineInternal(String dxfFilePath) throws ParseException {
        logger.info("Loading from {}", dxfFilePath);
        Parser parser = ParserBuilder.createDefaultParser();
        parser.parse(dxfFilePath, DXFParser.DEFAULT_ENCODING);

        DXFDocument doc = parser.getDocument();

        List<DXFLine> result = new ArrayList<>();

        Iterator<DXFLayer> layerIterator = doc.getDXFLayerIterator();
        while (layerIterator.hasNext()) {
            DXFLayer layer = layerIterator.next();
            List<DXFEntity> entities = nullToEmptyList(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_LINE));
            entities.addAll(nullToEmptyList(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ARC)));
            entities.addAll(nullToEmptyList(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_CIRCLE)));
            entities.addAll(nullToEmptyList(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_ELLIPSE)));
            entities.addAll(nullToEmptyList(layer.getDXFEntities(DXFConstants.ENTITY_TYPE_SPLINE)));

            for (DXFEntity entity : entities) {
                if (entity instanceof DXFLine) {
                    result.add((DXFLine) entity);
                } else if (entity instanceof DXFCircle) {
                    logger.debug("Circle");
                    result.addAll(discretizeCircle((DXFCircle) entity, 32));
                } else if (entity instanceof DXFArc) {
                    logger.debug("Arc");
                    result.addAll(discretizeArc((DXFArc) entity, 32));
                } else if (entity instanceof DXFEllipse) {
                    logger.debug("Ellipse");
                    result.addAll(discretizeEllipse((DXFEllipse) entity, 32));
                } else if (entity instanceof DXFSpline) {
                    logger.debug("Spline");
                    result.addAll(discretizeSpline((DXFSpline) entity, 8));
                }
            }
        }
        List<Line> converted = result.stream().map(l -> new Line(
            new de.kremerdaniel.openfoamcut.bo.Point(l.getStartPoint().getX(), l.getStartPoint().getY()),
            new de.kremerdaniel.openfoamcut.bo.Point(l.getEndPoint().getX(), l.getEndPoint().getY()))).collect(Collectors.toList());
        return new CutOutline(converted, true);
    }

    private static List<DXFLine> discretizeCircle(DXFCircle circle, int segments) {
        List<DXFLine> lines = new ArrayList<>();
        double cx = circle.getCenterPoint().getX();
        double cy = circle.getCenterPoint().getY();
        double r = circle.getRadius();

        double angleStep = 2 * Math.PI / segments;

        for (int i = 0; i < segments; i++) {
            double theta1 = i * angleStep;
            double theta2 = (i + 1) * angleStep;

            DXFLine line = new DXFLine();
            line.setStartPoint(new Point(cx + r*Math.cos(theta1), cy + r*Math.sin(theta1), 0));
            line.setEndPoint(new Point(cx + r*Math.cos(theta2), cy + r*Math.sin(theta2), 0));
            lines.add(line);
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    private static List<DXFLine> discretizeSpline(DXFSpline spline, int segments) {
        List<DXFLine> lines = new ArrayList<>();

        // Get control points of the spline
        List<Point> ctrlPoints = new LinkedList<>();
        spline.getSplinePointIterator().forEachRemaining(p -> ctrlPoints.add((Point) p));
        if (ctrlPoints.size() < 2) {
            return lines; // nothing to draw
        }

        // Simple uniform sampling along control points
        // Interpolate 'segments' points between first and last control point
        for (int i = 0; i < ctrlPoints.size() - 1; i++) {
            Point p1 = ctrlPoints.get(i);
            Point p2 = ctrlPoints.get(i + 1);

            // Subdivide the segment into smaller pieces for smoother approximation
            for (int s = 0; s < segments; s++) {
                double alpha1 = (double) s / segments;
                double alpha2 = (double) (s + 1) / segments;

                Point start = new Point(
                        p1.getX() * (1 - alpha1) + p2.getX() * alpha1,
                        p1.getY() * (1 - alpha1) + p2.getY() * alpha1,
                        p1.getZ() * (1 - alpha1) + p2.getZ() * alpha1
                );
                Point end = new Point(
                        p1.getX() * (1 - alpha2) + p2.getX() * alpha2,
                        p1.getY() * (1 - alpha2) + p2.getY() * alpha2,
                        p1.getZ() * (1 - alpha2) + p2.getZ() * alpha2
                );

                DXFLine line = new DXFLine();
                line.setStartPoint(start);
                line.setEndPoint(end);
                lines.add(line);
            }
        }

        try {
            smoothTransitions(lines, 3);
        } catch(Exception e) {
            logger.warn("Failed to smooth spline transitions", e);
        }
        return lines;
        //return lines;
    }

    /**
     * Takes a flat list of small DXFLine segments and smooths the transitions/junctions between them.
     * 
     * The first and last point of the whole chain stay exactly the same.
     * Every internal junction is replaced by a simple moving average over +-n lines.
     * 
     * Result has exactly the same number of lines as the input.
     * Higher n = smoother (but slightly more rounded) transitions.
     */
    private static void smoothTransitions(List<DXFLine> originalLines, int n) {
        if (originalLines.size() < n * 2 || n < 1) {          // need at least a few lines to smooth
            return;
        }

        // For all lines starting with nth line
        for (int i = n; i < originalLines.size() - n; i++) {

            DXFLine actual = originalLines.get(i);
            DXFLine before = originalLines.get(i - 1);
            DXFLine after = originalLines.get(i + 1);

            double backSumX = actual.getStartPoint().getX();
            double backSumY = actual.getStartPoint().getY();
            double forwardSumX = actual.getEndPoint().getX();
            double forwardSumY = actual.getEndPoint().getY();

            for(int j = 1; j <= n; j++) {
                backSumX += originalLines.get(i - j).getStartPoint().getX();
                backSumY += originalLines.get(i - j).getStartPoint().getY();
                forwardSumX += originalLines.get(i + j).getEndPoint().getX();
                forwardSumY += originalLines.get(i + j).getEndPoint().getY();
            }
            int count = n + 1;
            before.getEndPoint().setX(backSumX / count);
            before.getEndPoint().setY(backSumY / count);
            actual.getStartPoint().setX(backSumX / count);
            actual.getStartPoint().setY(backSumY / count);

            actual.getEndPoint().setX(forwardSumX / count);
            actual.getEndPoint().setY(forwardSumY / count);
            after.getStartPoint().setX(forwardSumX / count);
            after.getStartPoint().setY(forwardSumY / count);
        }
    }

    private static List<DXFLine> discretizeArc(DXFArc arc, int segments) {
        List<DXFLine> lines = new ArrayList<>();

        double cx = arc.getCenterPoint().getX();
        double cy = arc.getCenterPoint().getY();
        double r = arc.getRadius();
        double startAngle = Math.toRadians(arc.getStartAngle());
        double endAngle = Math.toRadians(arc.getEndAngle());

        // Handle negative sweep or full circle
        if (endAngle < startAngle) {
            endAngle += 2 * Math.PI;
        }

        double angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double theta1 = startAngle + i * angleStep;
            double theta2 = startAngle + (i + 1) * angleStep;

            DXFLine line = new DXFLine();
            line.setStartPoint(new Point(
                    cx + r * Math.cos(theta1),
                    cy + r * Math.sin(theta1),
                    0
            ));
            line.setEndPoint(new Point(
                    cx + r * Math.cos(theta2),
                    cy + r * Math.sin(theta2),
                    0
            ));
            lines.add(line);
        }

        return lines;
    }

    private static List<DXFLine> discretizeEllipse(DXFEllipse ellipse, int segments) {
        List<DXFLine> lines = new ArrayList<>();

        Point center = ellipse.getCenterPoint();
        Point majorVec = ellipse.getMajorAxisDirection();
        double a = Math.sqrt(majorVec.getX()*majorVec.getX() + majorVec.getY()*majorVec.getY()); // semi-major
        double b = a * ellipse.getRatio(); // semi-minor
        double rotation = Math.atan2(majorVec.getY(), majorVec.getX()); // angle of major axis

        double startParam = ellipse.getStartParameter();
        double endParam = ellipse.getEndParameter();
        if (endParam < startParam) {
            endParam += 2 * Math.PI;
        }

        double step = (endParam - startParam) / segments;

        for (int i = 0; i < segments; i++) {
            double t1 = startParam + i * step;
            double t2 = startParam + (i + 1) * step;

            // parametric ellipse before rotation
            double x1 = a * Math.cos(t1);
            double y1 = b * Math.sin(t1);
            double x2 = a * Math.cos(t2);
            double y2 = b * Math.sin(t2);

            // rotate around center
            double xr1 = x1 * Math.cos(rotation) - y1 * Math.sin(rotation) + center.getX();
            double yr1 = x1 * Math.sin(rotation) + y1 * Math.cos(rotation) + center.getY();
            double xr2 = x2 * Math.cos(rotation) - y2 * Math.sin(rotation) + center.getX();
            double yr2 = x2 * Math.sin(rotation) + y2 * Math.cos(rotation) + center.getY();

            DXFLine line = new DXFLine();
            line.setStartPoint(new Point(xr1, yr1, 0));
            line.setEndPoint(new Point(xr2, yr2, 0));
            lines.add(line);
        }

        return lines;
    }

    private static <T> List<T> nullToEmptyList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

    private static class CachedCutOutline {
        final CutOutline outline;
        final long lastModified;

        CachedCutOutline(CutOutline outline, long lastModified) {
            this.outline = outline;
            this.lastModified = lastModified;
        }
    }

    private DxfParser() {

    }

}

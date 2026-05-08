package de.kremerdaniel.openfoamcut.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.kremerdaniel.openfoamcut.Logger;
import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.UserErrorException;

/**
 * Can parse .dat files, Selig or Lednicer format
 */
public final class DatParser {

    private static final Logger logger = new Logger(DatParser.class);

    private DatParser() {
        // Utility class
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private static final Map<Path, CachedCutOutline> CACHE = new ConcurrentHashMap<>();

    /**
     * Loads an outline from a .dat file. Supports Selig and Lednicer formats.
     * @param datFilePath Absolute path to .dat file
     * @return Loaded CutOutline, normalized
     * @throws UserErrorException If there was an error loading/parsing the file
     */
    public static CutOutline loadCutOutline(String datFilePath) throws UserErrorException {
        Path file = Paths.get(datFilePath).toAbsolutePath().normalize();

        FileTime fileTime;
        try {
            fileTime = Files.getLastModifiedTime(file);
        } catch (IOException e) {
            throw new UserErrorException("Could not find file " + file, e);
        }
        long lastModified = fileTime.toMillis();

        CachedCutOutline cached = CACHE.get(file);

        if (cached != null && cached.lastModified == lastModified) {
            // Cache hit
            return cached.outline;
        }

        // Cache miss or file changed → reload
        CutOutline outline;
        try {
            outline = loadCutOutlineInternal(file.toString());
        } catch (Exception e) {
            throw new UserErrorException("Could not load file " + file, e);
        }

        CACHE.put(file, new CachedCutOutline(outline, lastModified));
        return outline;

    }

    private static CutOutline loadCutOutlineInternal(String datFilePath) {

        Path file = Paths.get(datFilePath).toAbsolutePath().normalize();

        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new UserErrorException("Could not read dat file: " + datFilePath, e);
        }

        if (lines.size() < 3) {
            throw new UserErrorException("Invalid dat file: " + datFilePath, null);
        }

        String name = lines.get(0).trim();

        // ---------- Detect Lednicer ----------
        boolean isLednicer = false;
        int nu = 0;
        int nl = 0;

        String[] headerTok = lines.get(1).trim().split("\\s+");
        if (headerTok.length == 2) {
            try {
                double a = Double.parseDouble(headerTok[0]);
                double b = Double.parseDouble(headerTok[1]);

                if (a > 2 && b > 2) {
                    nu = (int) a;
                    nl = (int) b;
                    if (lines.size() >= 2 + nu + nl) {
                        isLednicer = true;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        logger.info("Found (lednicer? {}) dat file {} with internal name {}", isLednicer, datFilePath, name);

        List<Point> ordered = new ArrayList<>();

        // ---------- Lednicer ----------
        if (isLednicer) {
            parseLednicer(lines, nu, nl, ordered);

        // ---------- Selig ----------
        } else {
            parseSelig(datFilePath, lines, ordered);
        }

        // ---------- Build outline ----------
        List<Line> outlineLines = new ArrayList<>();
        Point start = ordered.get(0);

        for (int i = 1; i < ordered.size(); i++) {
            Point end = ordered.get(i);
            outlineLines.add(new Line(start, end));
            start = end;
        }

        // close contour
        outlineLines.add(new Line(start, ordered.get(0)));

        outlineLines = splitLines(outlineLines, 4);
        smoothTransitions(outlineLines, 2);

        CutOutline outline = new CutOutline(outlineLines, true);
        double currentWidth = outline.getBounds().getWidth();
        double scaleFactor = 100.0 / currentWidth;
        outline.scale(scaleFactor);
        return outline;
    }

    private static void parseLednicer(List<String> lines, int nu, int nl, List<Point> ordered) {
        List<Point> upper = new ArrayList<>();
        List<Point> lower = new ArrayList<>();

        int idx = 2;

        // skip blank lines before upper surface
        while (idx < lines.size() && lines.get(idx).trim().isEmpty()) {
            idx++;
        }

        // upper surface (LE → TE)
        for (int i = 0; i < nu; i++) {
            String[] t = lines.get(idx).trim().split("\\s+");
            idx++;
            upper.add(new Point(
                Double.parseDouble(t[0]),
                Double.parseDouble(t[1])
            ));
        }

        // skip blanks
        while (idx < lines.size() && lines.get(idx).trim().isEmpty()) {
            idx++;
        }

        // lower surface (LE → TE)
        for (int i = 0; i < nl; i++) {
            String[] t = lines.get(idx).trim().split("\\s+");
            idx++;
            lower.add(new Point(
                Double.parseDouble(t[0]),
                Double.parseDouble(t[1])
            ));
        }

        // reorder to TE → LE → TE
        Collections.reverse(upper);
        ordered.addAll(upper);
        ordered.addAll(lower.subList(1, lower.size())); // skip duplicate LE
    }

    private static void parseSelig(String datFilePath, List<String> lines, List<Point> ordered) {
        List<Point> coords = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String l = lines.get(i).trim();
            if (l.isEmpty()) {
                continue;
            }

            String[] t = l.split("\\s+");
            if (t.length < 2) {
                continue;
            }

            coords.add(new Point(
                Double.parseDouble(t[0]),
                Double.parseDouble(t[1])
            ));
        }

        if (coords.size() < 3) {
            throw new UserErrorException("Invalid Selig dat file: " + datFilePath, null);
        }

        // find LE
        int leIndex = 0;
        double minX = coords.get(0).getX();
        for (int i = 1; i < coords.size(); i++) {
            if (coords.get(i).getX() < minX) {
                minX = coords.get(i).getX();
                leIndex = i;
            }
        }

        List<Point> upper = new ArrayList<>(coords.subList(0, leIndex + 1));
        List<Point> lower = new ArrayList<>(coords.subList(leIndex, coords.size()));

        ordered.addAll(upper);
        ordered.addAll(lower.subList(1, lower.size())); // skip duplicate LE
    }

    private static List<Line> splitLines(List<Line> originalLines, int n) {

        if (n < 1) {
            throw new IllegalArgumentException("n must be >= 1");
        }

        List<Line> result = new ArrayList<>(originalLines.size() * n);

        for (Line line : originalLines) {

            Point start = line.getStart();
            Point end   = line.getEnd();

            double dx = (end.getX() - start.getX()) / n;
            double dy = (end.getY() - start.getY()) / n;

            Point prev = start;

            for (int i = 1; i <= n; i++) {
                Point next = new Point(
                    start.getX() + dx * i,
                    start.getY() + dy * i
                );
                result.add(new Line(prev, next));
                prev = next;
            }
        }

        return result;
    }

    /**
     * Takes a flat list of small Line segments and smooths the transitions/junctions between them.
     * 
     * The first and last point of the whole chain stay exactly the same.
     * Every internal junction is replaced by a simple moving average over +-n lines.
     * 
     * Result has exactly the same number of lines as the input.
     * Higher n = smoother (but slightly more rounded) transitions.
     */
    private static void smoothTransitions(List<Line> originalLines, int n) {
        if (originalLines.size() < n * 2 || n < 1) {          // need at least a few lines to smooth
            return;
        }

        // For all lines starting with nth line
        for (int i = n; i < originalLines.size() - n; i++) {

            Line actual = originalLines.get(i);
            Line before = originalLines.get(i - 1);
            Line after = originalLines.get(i + 1);

            double backSumX = actual.getStart().getX();
            double backSumY = actual.getStart().getY();
            double forwardSumX = actual.getEnd().getX();
            double forwardSumY = actual.getEnd().getY();

            for(int j = 1; j <= n; j++) {
                backSumX += originalLines.get(i - j).getStart().getX();
                backSumY += originalLines.get(i - j).getStart().getY();
                forwardSumX += originalLines.get(i + j).getEnd().getX();
                forwardSumY += originalLines.get(i + j).getEnd().getY();
            }
            int count = n + 1;
            before.getEnd().setX(backSumX / count);
            before.getEnd().setY(backSumY / count);
            actual.getStart().setX(backSumX / count);
            actual.getStart().setY(backSumY / count);

            actual.getEnd().setX(forwardSumX / count);
            actual.getEnd().setY(forwardSumY / count);
            after.getStart().setX(forwardSumX / count);
            after.getStart().setY(forwardSumY / count);
        }
    }

    private static class CachedCutOutline {
        final CutOutline outline;
        final long lastModified;

        CachedCutOutline(CutOutline outline, long lastModified) {
            this.outline = outline;
            this.lastModified = lastModified;
        }
    }
    
}

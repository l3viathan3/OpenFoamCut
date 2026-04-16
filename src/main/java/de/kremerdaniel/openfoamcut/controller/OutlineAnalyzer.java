package de.kremerdaniel.openfoamcut.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import de.kremerdaniel.openfoamcut.bo.Line;

/**
 * Methods to analyze outlines
 */
@SuppressWarnings("PMD.LooseCoupling")
public final class OutlineAnalyzer {

    /**
     * Count the number of distinct islands of connected lines
     * @param lines Outline
     * @return Number of islands of connected lines
     */
    public static int countIslands(List<Line> lines) {
        if (lines.isEmpty()) {
            return 0;
        }

        Map<PointKey, List<Integer>> endpointMap = buildEndpointMap(lines);
        boolean[] visited = new boolean[lines.size()];
        int islands = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (!visited[i]) {
                islands++;
                dfs(i, lines, endpointMap, visited);
            }
        }
        return islands;
    }

    private static void dfs(
            int lineIndex,
            List<Line> lines,
            Map<PointKey, List<Integer>> endpointMap,
            boolean[] visited) {
        Stack<Integer> stack = new Stack<>();
        stack.push(lineIndex);
        visited[lineIndex] = true;

        while (!stack.isEmpty()) {
            int idx = stack.pop();
            Line line = lines.get(idx);

            PointKey p1 = new PointKey(
                    line.getStart().getX(),
                    line.getStart().getY()
            );
            PointKey p2 = new PointKey(
                    line.getEnd().getX(),
                    line.getEnd().getY()
            );

            for (PointKey pk : List.of(p1, p2)) {
                for (int neighbor : endpointMap.get(pk)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        stack.push(neighbor);
                    }
                }
            }
        }
    }
    
    private static Map<PointKey, List<Integer>> buildEndpointMap(List<Line> lines) {
        Map<PointKey, List<Integer>> map = new HashMap<>();

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);

            PointKey p1 = new PointKey(
                    l.getStart().getX(),
                    l.getStart().getY()
            );
            PointKey p2 = new PointKey(
                    l.getEnd().getX(),
                    l.getEnd().getY()
            );

            map.computeIfAbsent(p1, k -> new ArrayList<>()).add(i);
            map.computeIfAbsent(p2, k -> new ArrayList<>()).add(i);
        }
        return map;
    }

    static class PointKey {
        final long x;
        final long y;

        static final double EPS = 1e-3; // adjust if needed

        PointKey(double x, double y) {
            this.x = Math.round(x / EPS);
            this.y = Math.round(y / EPS);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PointKey)) {
                return false;
            }
            PointKey p = (PointKey) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(x) * 31 + Long.hashCode(y);
        }
    }

    private OutlineAnalyzer() {

    }
}

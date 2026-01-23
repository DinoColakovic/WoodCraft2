package unze.ptf.woodcraft.woodcraft.service;

import javafx.geometry.Point2D;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeometryService {
    public static final class CycleResult {
        private final boolean cycleDetected;
        private final List<Integer> nodeIds;

        public CycleResult(boolean cycleDetected, List<Integer> nodeIds) {
            this.cycleDetected = cycleDetected;
            this.nodeIds = nodeIds;
        }

        public boolean cycleDetected() {
            return cycleDetected;
        }

        public List<Integer> nodeIds() {
            return nodeIds;
        }
    }

    public CycleResult detectCycleForEdge(List<Edge> existingEdges, int startNodeId, int endNodeId) {
        Map<Integer, List<Integer>> adjacency = buildAdjacency(existingEdges);
        List<Integer> path = findPath(adjacency, startNodeId, endNodeId);
        if (path.isEmpty()) {
            return new CycleResult(false, List.of());
        }
        return new CycleResult(true, path);
    }

    public List<List<Integer>> detectAllCycles(List<Edge> edges) {
        Set<String> seen = new HashSet<>();
        List<List<Integer>> cycles = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            List<Edge> remaining = new ArrayList<>(edges);
            remaining.remove(i);
            CycleResult result = detectCycleForEdge(remaining, edge.getStartNodeId(), edge.getEndNodeId());
            if (result.cycleDetected()) {
                List<Integer> cycle = result.nodeIds();
                String key = normalizeCycle(cycle);
                if (seen.add(key)) {
                    cycles.add(cycle);
                }
            }
        }
        return cycles;
    }

    public ShapePolygon buildShapeFromCycle(int documentId, Integer materialId, List<Integer> nodeIds,
                                            Map<Integer, NodePoint> nodeMap, Map<String, Edge> edgeMap) {
        List<NodePoint> nodes = new ArrayList<>();
        for (Integer nodeId : nodeIds) {
            NodePoint node = nodeMap.get(nodeId);
            if (node != null) {
                nodes.add(node);
            }
        }
        List<Point2D> sampled = samplePath(nodeIds, nodeMap, edgeMap);
        double area = computeAreaCm2(sampled);
        double perimeter = computePerimeterCm(sampled);
        return new ShapePolygon(0, documentId, materialId, 1, nodeIds, nodes, area, perimeter);
    }

    public List<ShapePolygon> buildShapes(int documentId, List<NodePoint> nodes, List<Edge> edges) {
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        Map<String, Edge> edgeMap = buildEdgeMap(edges);
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.getStartNodeId(), key -> new ArrayList<>()).add(edge.getEndNodeId());
            adjacency.computeIfAbsent(edge.getEndNodeId(), key -> new ArrayList<>()).add(edge.getStartNodeId());
        }

        Set<String> seenCycles = new HashSet<>();
        List<ShapePolygon> shapes = new ArrayList<>();
        for (NodePoint node : nodes) {
            List<Integer> path = new ArrayList<>();
            path.add(node.getId());
            dfsCycles(node.getId(), node.getId(), adjacency, path, seenCycles, shapes, nodeMap, edgeMap, documentId);
        }
        return filterContainedCycles(shapes, nodeMap, edgeMap);
    }

    public double computeAreaCm2(List<Point2D> points) {
        if (points.size() < 3) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            Point2D current = points.get(i);
            Point2D next = points.get((i + 1) % points.size());
            sum += (current.getX() * next.getY()) - (next.getX() * current.getY());
        }
        return Math.abs(sum) / 2.0;
    }

    public double computePerimeterCm(List<Point2D> points) {
        if (points.size() < 2) {
            return 0;
        }
        double perimeter = 0;
        for (int i = 0; i < points.size(); i++) {
            Point2D current = points.get(i);
            Point2D next = points.get((i + 1) % points.size());
            double dx = current.getX() - next.getX();
            double dy = current.getY() - next.getY();
            perimeter += Math.hypot(dx, dy);
        }
        return perimeter;
    }

    private Map<Integer, List<Integer>> buildAdjacency(List<Edge> edges) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (Edge edge : edges) {
            adjacency.computeIfAbsent(edge.getStartNodeId(), key -> new ArrayList<>()).add(edge.getEndNodeId());
            adjacency.computeIfAbsent(edge.getEndNodeId(), key -> new ArrayList<>()).add(edge.getStartNodeId());
        }
        return adjacency;
    }

    private Map<String, Edge> buildEdgeMap(List<Edge> edges) {
        Map<String, Edge> map = new HashMap<>();
        for (Edge edge : edges) {
            map.put(edgeKey(edge.getStartNodeId(), edge.getEndNodeId()), edge);
        }
        return map;
    }

    private String edgeKey(int a, int b) {
        return a < b ? a + "-" + b : b + "-" + a;
    }

    private List<Integer> findPath(Map<Integer, List<Integer>> adjacency, int startNodeId, int endNodeId) {
        if (startNodeId == endNodeId) {
            return List.of(startNodeId);
        }
        Deque<Integer> queue = new ArrayDeque<>();
        Map<Integer, Integer> prev = new HashMap<>();
        queue.add(startNodeId);
        prev.put(startNodeId, null);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            for (int neighbor : adjacency.getOrDefault(current, List.of())) {
                if (prev.containsKey(neighbor)) {
                    continue;
                }
                prev.put(neighbor, current);
                if (neighbor == endNodeId) {
                    return buildPath(prev, endNodeId);
                }
                queue.add(neighbor);
            }
        }
        return List.of();
    }

    private List<Integer> buildPath(Map<Integer, Integer> prev, int endNodeId) {
        List<Integer> path = new ArrayList<>();
        Integer current = endNodeId;
        while (current != null) {
            path.add(current);
            current = prev.get(current);
        }
        List<Integer> ordered = new ArrayList<>();
        for (int i = path.size() - 1; i >= 0; i--) {
            ordered.add(path.get(i));
        }
        return ordered;
    }

    private void dfsCycles(int start, int current, Map<Integer, List<Integer>> adjacency, List<Integer> path,
                           Set<String> seenCycles, List<ShapePolygon> shapes, Map<Integer, NodePoint> nodeMap,
                           Map<String, Edge> edgeMap, int documentId) {
        List<Integer> neighbors = adjacency.getOrDefault(current, List.of());
        for (int neighbor : neighbors) {
            if (neighbor == start && path.size() >= 3) {
                String key = normalizeCycle(path);
                if (seenCycles.add(key)) {
                    List<NodePoint> cycleNodes = new ArrayList<>();
                    for (int nodeId : path) {
                        NodePoint node = nodeMap.get(nodeId);
                        if (node != null) {
                            cycleNodes.add(node);
                        }
                    }
                    List<Point2D> sampled = samplePath(path, nodeMap, edgeMap);
                    double area = computeAreaCm2(sampled);
                    double perimeter = computePerimeterCm(sampled);
                    shapes.add(new ShapePolygon(-1, documentId, null, 1, new ArrayList<>(path), cycleNodes, area, perimeter));
                }
            } else if (!path.contains(neighbor)) {
                path.add(neighbor);
                dfsCycles(start, neighbor, adjacency, path, seenCycles, shapes, nodeMap, edgeMap, documentId);
                path.remove(path.size() - 1);
            }
        }
    }

    private List<Point2D> samplePath(List<Integer> nodeIds, Map<Integer, NodePoint> nodeMap,
                                     Map<String, Edge> edgeMap) {
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < nodeIds.size(); i++) {
            int startId = nodeIds.get(i);
            int endId = nodeIds.get((i + 1) % nodeIds.size());
            NodePoint start = nodeMap.get(startId);
            NodePoint end = nodeMap.get(endId);
            if (start == null || end == null) {
                continue;
            }
            Edge edge = edgeMap.get(edgeKey(startId, endId));
            if (edge != null && edge.getControlStartXCm() != null && edge.getControlStartYCm() != null
                    && edge.getControlEndXCm() != null && edge.getControlEndYCm() != null) {
                Point2D c1;
                Point2D c2;
                if (edge.getStartNodeId() == startId) {
                    c1 = new Point2D(edge.getControlStartXCm(), edge.getControlStartYCm());
                    c2 = new Point2D(edge.getControlEndXCm(), edge.getControlEndYCm());
                } else {
                    c1 = new Point2D(edge.getControlEndXCm(), edge.getControlEndYCm());
                    c2 = new Point2D(edge.getControlStartXCm(), edge.getControlStartYCm());
                }
                sampleCubic(points,
                        new Point2D(start.getXCm(), start.getYCm()),
                        c1, c2,
                        new Point2D(end.getXCm(), end.getYCm()),
                        20);
            } else {
                if (points.isEmpty()) {
                    points.add(new Point2D(start.getXCm(), start.getYCm()));
                }
                points.add(new Point2D(end.getXCm(), end.getYCm()));
            }
        }
        return points;
    }

    private void sampleCubic(List<Point2D> points, Point2D p0, Point2D p1, Point2D p2, Point2D p3, int segments) {
        if (points.isEmpty()) {
            points.add(p0);
        }
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double u = 1 - t;
            double x = u * u * u * p0.getX()
                    + 3 * u * u * t * p1.getX()
                    + 3 * u * t * t * p2.getX()
                    + t * t * t * p3.getX();
            double y = u * u * u * p0.getY()
                    + 3 * u * u * t * p1.getY()
                    + 3 * u * t * t * p2.getY()
                    + t * t * t * p3.getY();
            points.add(new Point2D(x, y));
        }
    }

    private List<ShapePolygon> filterContainedCycles(List<ShapePolygon> shapes,
                                                     Map<Integer, NodePoint> nodeMap,
                                                     Map<String, Edge> edgeMap) {
        if (shapes.size() <= 1) {
            return shapes;
        }
        List<List<Point2D>> samples = new ArrayList<>();
        for (ShapePolygon shape : shapes) {
            samples.add(samplePath(shape.getNodeIds(), nodeMap, edgeMap));
        }
        boolean[] keep = new boolean[shapes.size()];
        for (int i = 0; i < keep.length; i++) {
            keep[i] = true;
        }
        for (int i = 0; i < shapes.size(); i++) {
            List<Point2D> outer = samples.get(i);
            if (outer.size() < 3) {
                continue;
            }
            double outerArea = shapes.get(i).getAreaCm2();
            for (int j = 0; j < shapes.size(); j++) {
                if (i == j) {
                    continue;
                }
                if (outerArea <= shapes.get(j).getAreaCm2()) {
                    continue;
                }
                List<Point2D> inner = samples.get(j);
                if (inner.size() < 3) {
                    continue;
                }
                Point2D probe = inner.get(0);
                if (pointInPolygon(outer, probe)) {
                    keep[i] = false;
                    break;
                }
            }
        }
        List<ShapePolygon> filtered = new ArrayList<>();
        for (int i = 0; i < shapes.size(); i++) {
            if (keep[i]) {
                filtered.add(shapes.get(i));
            }
        }
        return filtered;
    }

    private boolean pointInPolygon(List<Point2D> polygon, Point2D point) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Point2D a = polygon.get(i);
            Point2D b = polygon.get(j);
            boolean intersect = ((a.getY() > point.getY()) != (b.getY() > point.getY()))
                    && (point.getX() < (b.getX() - a.getX()) * (point.getY() - a.getY())
                    / (b.getY() - a.getY()) + a.getX());
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private String normalizeCycle(List<Integer> path) {
        int size = path.size();
        List<Integer> forward = new ArrayList<>(path);
        List<Integer> backward = new ArrayList<>();
        for (int i = path.size() - 1; i >= 0; i--) {
            backward.add(path.get(i));
        }
        String a = rotationKey(forward, size);
        String b = rotationKey(backward, size);
        return (a.compareTo(b) <= 0) ? a : b;
    }

    private String rotationKey(List<Integer> cycle, int size) {
        int minIndex = 0;
        for (int i = 1; i < size; i++) {
            if (cycle.get(i) < cycle.get(minIndex)) {
                minIndex = i;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int index = (minIndex + i) % size;
            builder.append(cycle.get(index)).append('-');
        }
        return builder.toString();
    }
}

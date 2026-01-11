package unze.ptf.woodcraft.woodcraft.service;

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
                                            Map<Integer, NodePoint> nodeMap) {
        List<NodePoint> nodes = new ArrayList<>();
        for (Integer nodeId : nodeIds) {
            NodePoint node = nodeMap.get(nodeId);
            if (node != null) {
                nodes.add(node);
            }
        }
        double area = computeAreaCm2(nodes);
        double perimeter = computePerimeterCm(nodes);
        return new ShapePolygon(0, documentId, materialId, 1, nodeIds, nodes, area, perimeter);
    }

    public List<ShapePolygon> buildShapes(int documentId, List<NodePoint> nodes, List<Edge> edges) {
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
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
            dfsCycles(node.getId(), node.getId(), adjacency, path, seenCycles, shapes, nodeMap, documentId);
        }
        return shapes;
    }

    public double computeAreaCm2(List<NodePoint> nodes) {
        if (nodes.size() < 3) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < nodes.size(); i++) {
            NodePoint current = nodes.get(i);
            NodePoint next = nodes.get((i + 1) % nodes.size());
            sum += (current.getXCm() * next.getYCm()) - (next.getXCm() * current.getYCm());
        }
        return Math.abs(sum) / 2.0;
    }

    public double computePerimeterCm(List<NodePoint> nodes) {
        if (nodes.size() < 2) {
            return 0;
        }
        double perimeter = 0;
        for (int i = 0; i < nodes.size(); i++) {
            NodePoint current = nodes.get(i);
            NodePoint next = nodes.get((i + 1) % nodes.size());
            double dx = current.getXCm() - next.getXCm();
            double dy = current.getYCm() - next.getYCm();
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
                           int documentId) {
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
                    double area = computeAreaCm2(cycleNodes);
                    double perimeter = computePerimeterCm(cycleNodes);
                    shapes.add(new ShapePolygon(-1, documentId, null, 1, new ArrayList<>(path), cycleNodes, area, perimeter));
                }
            } else if (!path.contains(neighbor)) {
                path.add(neighbor);
                dfsCycles(start, neighbor, adjacency, path, seenCycles, shapes, nodeMap, documentId);
                path.remove(path.size() - 1);
            }
        }
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

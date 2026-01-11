package unze.ptf.woodcraft.woodcraft.service;

import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryService {
    public CycleResult detectCycleForEdge(List<Edge> existingEdges, int startNodeId, int endNodeId) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (Edge edge : existingEdges) {
            adjacency.computeIfAbsent(edge.getStartNodeId(), key -> new ArrayList<>()).add(edge.getEndNodeId());
            adjacency.computeIfAbsent(edge.getEndNodeId(), key -> new ArrayList<>()).add(edge.getStartNodeId());
        }

        Map<Integer, Integer> previous = new HashMap<>();
        List<Integer> queue = new ArrayList<>();
        queue.add(startNodeId);
        previous.put(startNodeId, null);

        int index = 0;
        while (index < queue.size()) {
            int current = queue.get(index++);
            if (current == endNodeId) {
                break;
            }
            for (int neighbor : adjacency.getOrDefault(current, List.of())) {
                if (!previous.containsKey(neighbor)) {
                    previous.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!previous.containsKey(endNodeId)) {
            return CycleResult.empty();
        }

        List<Integer> path = new ArrayList<>();
        Integer cursor = endNodeId;
        while (cursor != null) {
            path.add(0, cursor);
            cursor = previous.get(cursor);
        }

        if (path.isEmpty() || path.get(0) != startNodeId) {
            return CycleResult.empty();
        }

        List<Integer> cycleNodes = new ArrayList<>(path);
        cycleNodes.add(startNodeId);
        return new CycleResult(true, normalizeCycle(cycleNodes));
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

    private List<Integer> normalizeCycle(List<Integer> path) {
        List<Integer> normalizedInput = new ArrayList<>(path);
        if (normalizedInput.size() > 1 && normalizedInput.get(0).equals(normalizedInput.get(normalizedInput.size() - 1))) {
            normalizedInput.remove(normalizedInput.size() - 1);
        }
        int size = normalizedInput.size();
        List<Integer> forward = new ArrayList<>(normalizedInput);
        List<Integer> backward = new ArrayList<>();
        for (int i = normalizedInput.size() - 1; i >= 0; i--) {
            backward.add(normalizedInput.get(i));
        }
        List<Integer> forwardKey = rotationKey(forward, size);
        List<Integer> backwardKey = rotationKey(backward, size);
        List<Integer> core = compareCycles(forwardKey, backwardKey) <= 0 ? forwardKey : backwardKey;
        List<Integer> closed = new ArrayList<>(core);
        closed.add(core.get(0));
        return List.copyOf(closed);
    }

    private List<Integer> rotationKey(List<Integer> cycle, int size) {
        int minIndex = 0;
        for (int i = 1; i < size; i++) {
            if (cycle.get(i) < cycle.get(minIndex)) {
                minIndex = i;
            }
        }
        List<Integer> rotated = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int index = (minIndex + i) % size;
            rotated.add(cycle.get(index));
        }
        return List.copyOf(rotated);
    }

    private int compareCycles(List<Integer> left, List<Integer> right) {
        int size = left.size() < right.size() ? left.size() : right.size();
        for (int i = 0; i < size; i++) {
            int compare = Integer.compare(left.get(i), right.get(i));
            if (compare != 0) {
                return compare;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    public ShapePolygon buildShapeFromCycle(int documentId, Integer materialId, List<Integer> nodeIds,
                                            Map<Integer, NodePoint> nodeMap) {
        List<NodePoint> nodes = new ArrayList<>();
        for (int nodeId : nodeIds) {
            NodePoint node = nodeMap.get(nodeId);
            if (node != null) {
                nodes.add(node);
            }
        }
        double area = computeAreaCm2(nodes);
        double perimeter = computePerimeterCm(nodes);
        return new ShapePolygon(-1, documentId, materialId, 1, nodeIds, nodes, area, perimeter);
    }

    public List<List<Integer>> detectAllCycles(List<Edge> edges) {
        List<List<Integer>> cycles = new ArrayList<>();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            List<Edge> others = new ArrayList<>(edges);
            others.remove(i);
            CycleResult result = detectCycleForEdge(others, edge.getStartNodeId(), edge.getEndNodeId());
            if (result.cycleDetected() && !containsCycle(cycles, result.nodeIds())) {
                cycles.add(result.nodeIds());
            }
        }
        return cycles;
    }

    private boolean containsCycle(List<List<Integer>> cycles, List<Integer> candidate) {
        for (List<Integer> cycle : cycles) {
            if (cycle.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    public record CycleResult(boolean cycleDetected, List<Integer> nodeIds) {
        public static CycleResult empty() {
            return new CycleResult(false, List.of());
        }
    }
}

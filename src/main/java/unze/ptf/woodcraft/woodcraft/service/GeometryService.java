package unze.ptf.woodcraft.woodcraft.service;

import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeometryService {
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
                    shapes.add(new ShapePolygon(-1, documentId, null, 1, cycleNodes, area, perimeter));
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

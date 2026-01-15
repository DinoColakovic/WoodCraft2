package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class CanvasPane extends Pane {
    public enum Mode {
        PEN,
        SELECT,
        ERASE
    }

    private static final double NODE_RADIUS = 4.5;

    private final Group shapeLayer = new Group();
    private final Group edgeLayer = new Group();
    private final Group guideLayer = new Group();
    private final Group nodeLayer = new Group();

    private final Map<Integer, Circle> nodeViews = new HashMap<>();
    private final Map<Integer, Polygon> shapeViews = new HashMap<>();

    private final List<NodePoint> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final Map<Integer, Color> materialColors = new HashMap<>();

    private Consumer<Point2D> onCanvasClicked;
    private IntConsumer onNodeClicked;
    private IntConsumer onShapeClicked;

    private double scale = 10.0;
    private Mode mode = Mode.PEN;
    private Integer selectedNodeId;
    private Integer selectedShapeId;

    public CanvasPane() {
        setStyle("-fx-background-color: #fdfdfd; -fx-border-color: #d0d0d0;");
        getChildren().addAll(shapeLayer, edgeLayer, guideLayer, nodeLayer);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (event.getTarget() != this) {
                return;
            }
            if (onCanvasClicked != null) {
                onCanvasClicked.accept(toCm(event.getX(), event.getY()));
            }
        });
    }

    public void setOnCanvasClicked(Consumer<Point2D> handler) {
        this.onCanvasClicked = handler;
    }

    public void setOnNodeClicked(IntConsumer handler) {
        this.onNodeClicked = handler;
    }

    public void setOnShapeClicked(IntConsumer handler) {
        this.onShapeClicked = handler;
    }

    public void setMaterialColors(Map<Integer, Color> colors) {
        materialColors.clear();
        if (colors != null) {
            materialColors.putAll(colors);
        }
        redraw();
    }

    public void addShape(ShapePolygon shape) {
        shapes.add(shape);
        drawShape(shape);
    }

    public void setShapes(List<ShapePolygon> shapes) {
        this.shapes.clear();
        this.shapes.addAll(shapes);
        redraw();
    }

    public void setSelectedNode(int nodeId) {
        selectedNodeId = nodeId;
        selectedShapeId = null;
        updateSelectionStyles();
    }

    public void setSelectedShape(int shapeId) {
        selectedShapeId = shapeId;
        selectedNodeId = null;
        updateSelectionStyles();
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedShapeId = null;
        updateSelectionStyles();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.PEN) {
            setCursor(Cursor.HAND);
        } else if (mode == Mode.SELECT) {
            setCursor(Cursor.DEFAULT);
        } else {
            setCursor(Cursor.CROSSHAIR);
        }
    }

    public void setScale(double scale) {
        this.scale = scale;
        redraw();
    }

    public void setNodes(List<NodePoint> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
        redraw();
    }

    public void setEdges(List<Edge> edges) {
        this.edges.clear();
        this.edges.addAll(edges);
        redraw();
    }

    public void setGuides(List<Guide> guides) {
        this.guides.clear();
        this.guides.addAll(guides);
        redraw();
    }

    public void addNode(NodePoint node) {
        nodes.add(node);
        drawNode(node);
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
        drawEdge(edge);
    }

    public void addGuide(Guide guide) {
        guides.add(guide);
        drawGuide(guide);
    }

    private void redraw() {
        shapeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
        guideLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        nodeViews.clear();
        shapeViews.clear();

        for (ShapePolygon shape : shapes) {
            drawShape(shape);
        }
        for (Edge edge : edges) {
            drawEdge(edge);
        }
        for (Guide guide : guides) {
            drawGuide(guide);
        }
        for (NodePoint node : nodes) {
            drawNode(node);
        }
        updateSelectionStyles();
    }

    private void drawNode(NodePoint node) {
        double x = node.getXCm() * scale;
        double y = node.getYCm() * scale;
        Circle circle = new Circle(x, y, NODE_RADIUS, Color.DODGERBLUE);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(1);
        circle.setOnMouseClicked(event -> {
            if (onNodeClicked != null) {
                onNodeClicked.accept(node.getId());
            }
            event.consume();
        });
        nodeViews.put(node.getId(), circle);
        nodeLayer.getChildren().add(circle);
    }

    private void drawEdge(Edge edge) {
        NodePoint start = findNode(edge.getStartNodeId());
        NodePoint end = findNode(edge.getEndNodeId());
        if (start == null || end == null) {
            return;
        }
        Line line = new Line(start.getXCm() * scale, start.getYCm() * scale, end.getXCm() * scale, end.getYCm() * scale);
        line.setStroke(Color.DARKSLATEGRAY);
        line.setStrokeWidth(2);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        edgeLayer.getChildren().add(line);
    }

    private void drawGuide(Guide guide) {
        Line line;
        if (guide.getOrientation() == Guide.Orientation.HORIZONTAL) {
            double y = guide.getPositionCm() * scale;
            line = new Line(0, y, getWidth(), y);
        } else {
            double x = guide.getPositionCm() * scale;
            line = new Line(x, 0, x, getHeight());
        }
        line.setStroke(Color.rgb(200, 80, 80, 0.65));
        line.setStrokeWidth(1);
        line.getStrokeDashArray().setAll(6.0, 4.0);
        guideLayer.getChildren().add(line);
    }

    private void drawShape(ShapePolygon shape) {
        List<NodePoint> shapeNodes = resolveShapeNodes(shape);
        if (shapeNodes.size() < 3) {
            return;
        }
        Polygon polygon = new Polygon();
        for (NodePoint node : shapeNodes) {
            polygon.getPoints().addAll(node.getXCm() * scale, node.getYCm() * scale);
        }
        Color base = shape.getMaterialId() == null ? Color.LIGHTGRAY : materialColors.get(shape.getMaterialId());
        if (base == null) {
            base = Color.LIGHTGRAY;
        }
        polygon.setFill(new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.25));
        polygon.setStroke(Color.GRAY);
        polygon.setStrokeWidth(1);
        polygon.setOnMouseClicked(event -> {
            if (onShapeClicked != null) {
                onShapeClicked.accept(shape.getId());
            }
            event.consume();
        });
        shapeViews.put(shape.getId(), polygon);
        shapeLayer.getChildren().add(polygon);
    }

    private List<NodePoint> resolveShapeNodes(ShapePolygon shape) {
        if (shape.getNodes() != null && !shape.getNodes().isEmpty()) {
            return shape.getNodes();
        }
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        List<NodePoint> resolved = new ArrayList<>();
        if (shape.getNodeIds() != null) {
            for (Integer nodeId : shape.getNodeIds()) {
                NodePoint node = nodeMap.get(nodeId);
                if (node != null) {
                    resolved.add(node);
                }
            }
        }
        return resolved;
    }

    private void updateSelectionStyles() {
        for (Map.Entry<Integer, Circle> entry : nodeViews.entrySet()) {
            Circle circle = entry.getValue();
            if (selectedNodeId != null && entry.getKey().equals(selectedNodeId)) {
                circle.setFill(Color.ORANGE);
            } else {
                circle.setFill(Color.DODGERBLUE);
            }
        }
        for (Map.Entry<Integer, Polygon> entry : shapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (selectedShapeId != null && entry.getKey().equals(selectedShapeId)) {
                polygon.setStroke(Color.ORANGE);
                polygon.setStrokeWidth(2);
            } else {
                polygon.setStroke(Color.GRAY);
                polygon.setStrokeWidth(1);
            }
        }
    }

    private NodePoint findNode(int nodeId) {
        for (NodePoint node : nodes) {
            if (node.getId() == nodeId) {
                return node;
            }
        }
        return null;
    }

    private Point2D toCm(double xPx, double yPx) {
        return new Point2D(xPx / scale, yPx / scale);
    }
}

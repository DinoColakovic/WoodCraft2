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

    private final Group guideLayer = new Group();
    private final Group shapeLayer = new Group();
    private final Group edgeLayer = new Group();
    private final Group nodeLayer = new Group();

    private final Map<Integer, Circle> nodeViews = new HashMap<>();
    private final Map<Integer, Polygon> shapeViews = new HashMap<>();
    private final List<NodePoint> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final Map<Integer, Color> materialColors = new HashMap<>();

    private double scale = 10.0;
    private Mode mode = Mode.PEN;
    private Integer selectedNodeId;
    private Integer selectedShapeId;

    private Consumer<Guide> onGuideRequested;
    private Consumer<Point2D> onCanvasClicked;
    private IntConsumer onNodeClicked;
    private IntConsumer onShapeClicked;

    public CanvasPane() {
        setStyle("-fx-background-color: #fdfdfd; -fx-border-color: #d0d0d0;");
        getChildren().addAll(guideLayer, shapeLayer, edgeLayer, nodeLayer);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Point2D cmPoint = toCm(event.getX(), event.getY());
            if (onCanvasClicked != null) {
                onCanvasClicked.accept(cmPoint);
            }
        });
    }

    public void setScale(double scale) {
        this.scale = scale;
        redraw();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.PEN) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.SELECT) {
            setCursor(Cursor.HAND);
        } else if (mode == Mode.ERASE) {
            setCursor(Cursor.DISAPPEAR);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }

    public void setOnGuideRequested(Consumer<Guide> onGuideRequested) {
        this.onGuideRequested = onGuideRequested;
    }

    public void setOnCanvasClicked(Consumer<Point2D> onCanvasClicked) {
        this.onCanvasClicked = onCanvasClicked;
    }

    public void setOnNodeClicked(IntConsumer onNodeClicked) {
        this.onNodeClicked = onNodeClicked;
    }

    public void setOnShapeClicked(IntConsumer onShapeClicked) {
        this.onShapeClicked = onShapeClicked;
    }

    public void setSelectedNode(int nodeId) {
        selectedNodeId = nodeId;
        selectedShapeId = null;
        refreshSelectionStyles();
    }

    public void setSelectedShape(int shapeId) {
        selectedShapeId = shapeId;
        selectedNodeId = null;
        refreshSelectionStyles();
    }

    public void clearSelection() {
        selectedNodeId = null;
        selectedShapeId = null;
        refreshSelectionStyles();
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

    public void setShapes(List<ShapePolygon> shapes) {
        this.shapes.clear();
        this.shapes.addAll(shapes);
        redraw();
    }

    public void setMaterialColors(Map<Integer, Color> materialColors) {
        this.materialColors.clear();
        this.materialColors.putAll(materialColors);
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

    public void addShape(ShapePolygon shape) {
        shapes.add(shape);
        drawShape(shape);
    }

    private void redraw() {
        edgeLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        guideLayer.getChildren().clear();
        shapeLayer.getChildren().clear();
        nodeViews.clear();
        shapeViews.clear();
        for (Guide guide : guides) {
            drawGuide(guide);
        }
        for (ShapePolygon shape : shapes) {
            drawShape(shape);
        }
        for (Edge edge : edges) {
            drawEdge(edge);
        }
        for (NodePoint node : nodes) {
            drawNode(node);
        }
    }

    private void drawNode(NodePoint node) {
        double x = node.getXCm() * scale;
        double y = node.getYCm() * scale;
        Circle circle = new Circle(x, y, 4.5, Color.DODGERBLUE);
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
        if (selectedNodeId != null && selectedNodeId == node.getId()) {
            circle.setFill(Color.ORANGE);
            circle.setStroke(Color.DARKRED);
        }
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

    private void drawShape(ShapePolygon shape) {
        if (shape.getNodes().size() < 3) {
            return;
        }
        Polygon polygon = new Polygon();
        for (NodePoint node : shape.getNodes()) {
            polygon.getPoints().addAll(node.getXCm() * scale, node.getYCm() * scale);
        }
        Color fillColor = materialColors.getOrDefault(shape.getMaterialId(), Color.web("#8FAADC"));
        polygon.setFill(Color.color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 0.2));
        polygon.setStroke(fillColor.darker());
        polygon.setStrokeWidth(2);
        polygon.setOnMouseClicked(event -> {
            if (onShapeClicked != null) {
                onShapeClicked.accept(shape.getId());
            }
            event.consume();
        });
        shapeLayer.getChildren().add(polygon);
        shapeViews.put(shape.getId(), polygon);
        if (selectedShapeId != null && selectedShapeId == shape.getId()) {
            polygon.setStroke(Color.GOLDENROD);
            polygon.setStrokeWidth(3);
        }
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

    private void refreshSelectionStyles() {
        for (Map.Entry<Integer, Circle> entry : nodeViews.entrySet()) {
            Circle circle = entry.getValue();
            if (selectedNodeId != null && entry.getKey().equals(selectedNodeId)) {
                circle.setFill(Color.ORANGE);
                circle.setStroke(Color.DARKRED);
            } else {
                circle.setFill(Color.DODGERBLUE);
                circle.setStroke(Color.WHITE);
            }
        }
        for (Map.Entry<Integer, Polygon> entry : shapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (selectedShapeId != null && entry.getKey().equals(selectedShapeId)) {
                polygon.setStroke(Color.GOLDENROD);
                polygon.setStrokeWidth(3);
            } else {
                polygon.setStrokeWidth(2);
            }
        }
    }

}

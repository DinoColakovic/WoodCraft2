package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CanvasPane extends Pane {
    public enum Mode {
        NONE,
        ADD_NODE,
        CONNECT_NODES
    }

    private final Group edgeLayer = new Group();
    private final Group guideLayer = new Group();
    private final Group nodeLayer = new Group();

    private final Map<Integer, Circle> nodeViews = new HashMap<>();
    private final List<NodePoint> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();

    private double scale = 10.0;
    private Mode mode = Mode.NONE;
    private Integer selectedNodeId;

    private Consumer<Point2D> onNodeRequested;
    private BiConsumer<Integer, Integer> onEdgeRequested;
    private Consumer<Guide> onGuideRequested;

    public CanvasPane() {
        setStyle("-fx-background-color: #fdfdfd; -fx-border-color: #d0d0d0;");
        getChildren().addAll(guideLayer, edgeLayer, nodeLayer);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (mode == Mode.ADD_NODE) {
                Point2D cmPoint = toCm(event.getX(), event.getY());
                if (onNodeRequested != null) {
                    onNodeRequested.accept(cmPoint);
                }
            }
        });
    }

    public void setScale(double scale) {
        this.scale = scale;
        redraw();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.CONNECT_NODES) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.ADD_NODE) {
            setCursor(Cursor.HAND);
        } else {
            setCursor(Cursor.DEFAULT);
        }
        selectedNodeId = null;
    }

    public void setOnNodeRequested(Consumer<Point2D> onNodeRequested) {
        this.onNodeRequested = onNodeRequested;
    }

    public void setOnEdgeRequested(BiConsumer<Integer, Integer> onEdgeRequested) {
        this.onEdgeRequested = onEdgeRequested;
    }

    public void setOnGuideRequested(Consumer<Guide> onGuideRequested) {
        this.onGuideRequested = onGuideRequested;
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
        edgeLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        guideLayer.getChildren().clear();
        nodeViews.clear();
        for (Guide guide : guides) {
            drawGuide(guide);
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
            if (mode != Mode.CONNECT_NODES) {
                return;
            }
            if (selectedNodeId == null) {
                selectedNodeId = node.getId();
                circle.setFill(Color.ORANGE);
            } else {
                int startNodeId = selectedNodeId;
                selectedNodeId = null;
                if (onEdgeRequested != null && startNodeId != node.getId()) {
                    onEdgeRequested.accept(startNodeId, node.getId());
                }
                for (Circle view : nodeViews.values()) {
                    view.setFill(Color.DODGERBLUE);
                }
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

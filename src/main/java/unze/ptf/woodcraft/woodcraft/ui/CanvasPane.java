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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class CanvasPane extends Pane {
    public enum Mode {
        DRAW_SHAPE,
        MOVE_NODE,
        DELETE_NODE,
        DELETE_GUIDE
    }

    private static final double NODE_RADIUS = 4.5;
    private static final double HANDLE_RADIUS = 4;

    private final Group contentLayer = new Group();
    private final Group boardLayer = new Group();
    private final Group shapeLayer = new Group();
    private final Group edgeLayer = new Group();
    private final Group handleLayer = new Group();
    private final Group guideLayer = new Group();
    private final Group nodeLayer = new Group();
    private final Rectangle selectionRect = new Rectangle();
    private final Rectangle clipRect = new Rectangle();
    private final Rectangle boardRect = new Rectangle();
    private final Rectangle viewportClip = new Rectangle();

    private final Map<Integer, Circle> nodeViews = new HashMap<>();
    private final Map<Integer, Polygon> shapeViews = new HashMap<>();
    private final Map<Integer, CubicCurveView> edgeViews = new HashMap<>();

    private final List<NodePoint> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final Map<Integer, Color> materialColors = new HashMap<>();
    private final Map<Integer, EdgeControls> edgeControls = new HashMap<>();

    private Consumer<Point2D> onCanvasClicked;
    private IntConsumer onNodeClicked;
    private IntConsumer onShapeClicked;
    private BiConsumer<Integer, Point2D> onNodeMoveFinished;
    private Consumer<List<Integer>> onDeleteNodes;
    private Consumer<List<Integer>> onDeleteGuides;

    private double scale = 10.0;
    private Mode mode = Mode.DRAW_SHAPE;
    private Integer selectedNodeId;
    private Integer selectedShapeId;
    private Integer movingNodeId;
    private Integer activeHandleEdgeId;
    private boolean activeHandleStart;
    private double canvasWidthCm = 244;
    private double canvasHeightCm = 122;
    private double panX;
    private double panY;
    private double panStartX;
    private double panStartY;
    private boolean panning;
    private double selectionStartX;
    private double selectionStartY;

    public CanvasPane() {
        setStyle("-fx-background-color: #fdfdfd; -fx-border-color: #d0d0d0;");
        selectionRect.setManaged(false);
        selectionRect.setVisible(false);
        selectionRect.setFill(Color.rgb(60, 120, 200, 0.15));
        selectionRect.setStroke(Color.rgb(60, 120, 200, 0.6));
        selectionRect.getStrokeDashArray().setAll(6.0, 4.0);
        selectionRect.setMouseTransparent(true);
        viewportClip.widthProperty().bind(widthProperty());
        viewportClip.heightProperty().bind(heightProperty());
        setClip(viewportClip);
        boardRect.setFill(Color.web("#faf8f2"));
        boardRect.setStroke(Color.web("#c9c2b5"));
        boardRect.setStrokeWidth(1);
        boardLayer.getChildren().add(boardRect);
        contentLayer.getChildren().addAll(boardLayer, shapeLayer, edgeLayer, nodeLayer, handleLayer);
        contentLayer.setClip(clipRect);
        getChildren().addAll(contentLayer, guideLayer, selectionRect);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());

        setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                panning = true;
                panStartX = event.getX();
                panStartY = event.getY();
                event.consume();
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY
                    && (mode == Mode.DELETE_NODE || mode == Mode.DELETE_GUIDE)
                    && (event.getTarget() == this || event.getTarget() == boardRect)) {
                selectionStartX = event.getX();
                selectionStartY = event.getY();
                selectionRect.setX(selectionStartX);
                selectionRect.setY(selectionStartY);
                selectionRect.setWidth(0);
                selectionRect.setHeight(0);
                selectionRect.setVisible(true);
                event.consume();
            }
        });

        setOnMouseDragged(event -> {
            if (panning && event.getButton() == MouseButton.MIDDLE) {
                double dx = event.getX() - panStartX;
                double dy = event.getY() - panStartY;
                panStartX = event.getX();
                panStartY = event.getY();
                panX += dx;
                panY += dy;
                updateLayerTransforms();
                redrawGuides();
                event.consume();
                return;
            }
            if (selectionRect.isVisible() && mode == Mode.DELETE_NODE) {
                double x = Math.min(selectionStartX, event.getX());
                double y = Math.min(selectionStartY, event.getY());
                double w = Math.abs(event.getX() - selectionStartX);
                double h = Math.abs(event.getY() - selectionStartY);
                selectionRect.setX(x);
                selectionRect.setY(y);
                selectionRect.setWidth(w);
                selectionRect.setHeight(h);
                event.consume();
            }
        });

        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                panning = false;
                event.consume();
                return;
            }
            if (selectionRect.isVisible() && (mode == Mode.DELETE_NODE || mode == Mode.DELETE_GUIDE)) {
                selectionRect.setVisible(false);
                if (mode == Mode.DELETE_NODE && onDeleteNodes != null) {
                    List<Integer> selected = collectNodesInSelection();
                    if (!selected.isEmpty()) {
                        onDeleteNodes.accept(selected);
                    }
                }
                if (mode == Mode.DELETE_GUIDE && onDeleteGuides != null) {
                    List<Integer> guideIds = collectGuidesInSelection();
                    if (!guideIds.isEmpty()) {
                        onDeleteGuides.accept(guideIds);
                    }
                }
                event.consume();
            }
        });

        setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (selectionRect.isVisible()) {
                return;
            }
            if (event.getTarget() != this && event.getTarget() != boardRect) {
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

    public void setOnNodeMoveFinished(BiConsumer<Integer, Point2D> handler) {
        this.onNodeMoveFinished = handler;
    }

    public void setOnDeleteNodes(Consumer<List<Integer>> handler) {
        this.onDeleteNodes = handler;
    }

    public void setOnDeleteGuides(Consumer<List<Integer>> handler) {
        this.onDeleteGuides = handler;
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
        if (mode == Mode.DRAW_SHAPE) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.MOVE_NODE) {
            setCursor(Cursor.MOVE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
        if (mode != Mode.DELETE_NODE && mode != Mode.DELETE_GUIDE) {
            selectionRect.setVisible(false);
        }
        refreshHandleLayer();
    }

    public void setScale(double scale) {
        this.scale = scale;
        redraw();
    }

    public void setCanvasSizeCm(double widthCm, double heightCm) {
        this.canvasWidthCm = Math.max(1, widthCm);
        this.canvasHeightCm = Math.max(1, heightCm);
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
        handleLayer.getChildren().clear();
        nodeViews.clear();
        shapeViews.clear();
        edgeViews.clear();

        updateBoardAndClip();

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
        updateLayerTransforms();
    }

    private void drawNode(NodePoint node) {
        double x = node.getXCm() * scale;
        double y = node.getYCm() * scale;
        Circle circle = new Circle(x, y, NODE_RADIUS, Color.DODGERBLUE);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(1);
        circle.setOnMouseClicked(event -> {
            if (onNodeClicked != null && mode != Mode.MOVE_NODE) {
                onNodeClicked.accept(node.getId());
            }
            event.consume();
        });
        circle.setOnMousePressed(event -> {
            if (mode == Mode.MOVE_NODE) {
                if (onNodeClicked != null) {
                    onNodeClicked.accept(node.getId());
                }
                movingNodeId = node.getId();
                event.consume();
            }
        });
        circle.setOnMouseDragged(event -> {
            if (mode != Mode.MOVE_NODE || movingNodeId == null || movingNodeId != node.getId()) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            Point2D cmPoint = toCm(local.getX(), local.getY());
            updateNodePosition(node.getId(), cmPoint, event.isShiftDown());
            event.consume();
        });
        circle.setOnMouseReleased(event -> {
            if (mode != Mode.MOVE_NODE || movingNodeId == null || movingNodeId != node.getId()) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            Point2D cmPoint = toCm(local.getX(), local.getY());
            updateNodePosition(node.getId(), cmPoint, event.isShiftDown());
            if (onNodeMoveFinished != null) {
                onNodeMoveFinished.accept(node.getId(), cmPoint);
            }
            movingNodeId = null;
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
        Point2D controlStart = getControlPoint(edge.getId(), true, start, end);
        Point2D controlEnd = getControlPoint(edge.getId(), false, start, end);
        javafx.scene.shape.CubicCurve curve = new javafx.scene.shape.CubicCurve(
                start.getXCm() * scale,
                start.getYCm() * scale,
                controlStart.getX() * scale,
                controlStart.getY() * scale,
                controlEnd.getX() * scale,
                controlEnd.getY() * scale,
                end.getXCm() * scale,
                end.getYCm() * scale
        );
        curve.setFill(Color.TRANSPARENT);
        curve.setStroke(Color.DARKSLATEGRAY);
        curve.setStrokeWidth(2);
        curve.setStrokeLineCap(StrokeLineCap.ROUND);
        edgeLayer.getChildren().add(curve);
        edgeViews.put(edge.getId(), new CubicCurveView(curve));
    }

    private void drawGuide(Guide guide) {
        Line line;
        if (guide.getOrientation() == Guide.Orientation.HORIZONTAL) {
            double y = guide.getPositionCm() * scale;
            line = new Line(0, y + panY, getWidth(), y + panY);
        } else {
            double x = guide.getPositionCm() * scale;
            line = new Line(x + panX, 0, x + panX, getHeight());
        }
        line.setStroke(Color.rgb(200, 80, 80, 0.65));
        line.setStrokeWidth(1);
        line.getStrokeDashArray().setAll(6.0, 4.0);
        line.setOnMouseClicked(event -> {
            if (mode == Mode.DELETE_GUIDE && event.getButton() == MouseButton.PRIMARY && onDeleteGuides != null) {
                onDeleteGuides.accept(List.of(guide.getId()));
                event.consume();
            }
        });
        guideLayer.getChildren().add(line);
    }

    private void redrawGuides() {
        guideLayer.getChildren().clear();
        for (Guide guide : guides) {
            drawGuide(guide);
        }
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

    private void updateNodePosition(int nodeId, Point2D cmPoint, boolean resetHandles) {
        double xCm = Math.max(0, cmPoint.getX());
        double yCm = Math.max(0, cmPoint.getY());
        Point2D previous = null;
        for (int i = 0; i < nodes.size(); i++) {
            NodePoint node = nodes.get(i);
            if (node.getId() == nodeId) {
                previous = new Point2D(node.getXCm(), node.getYCm());
                nodes.set(i, new NodePoint(nodeId, node.getDocumentId(), xCm, yCm));
                break;
            }
        }
        if (previous != null && !resetHandles) {
            updateControlsForNode(nodeId, previous, new Point2D(xCm, yCm));
        } else if (resetHandles) {
            resetControlsForNode(nodeId);
        }
        Circle circle = nodeViews.get(nodeId);
        if (circle != null) {
            circle.setCenterX(xCm * scale);
            circle.setCenterY(yCm * scale);
        }
        updateConnectedEdges(nodeId);
        updateShapePolygons();
        refreshHandleLayer();
    }

    private void updateConnectedEdges(int nodeId) {
        for (Edge edge : edges) {
            if (edge.getStartNodeId() != nodeId && edge.getEndNodeId() != nodeId) {
                continue;
            }
            CubicCurveView view = edgeViews.get(edge.getId());
            if (view == null) {
                continue;
            }
            NodePoint start = findNode(edge.getStartNodeId());
            NodePoint end = findNode(edge.getEndNodeId());
            if (start == null || end == null) {
                continue;
            }
            Point2D controlStart = getControlPoint(edge.getId(), true, start, end);
            Point2D controlEnd = getControlPoint(edge.getId(), false, start, end);
            view.curve.setStartX(start.getXCm() * scale);
            view.curve.setStartY(start.getYCm() * scale);
            view.curve.setControlX1(controlStart.getX() * scale);
            view.curve.setControlY1(controlStart.getY() * scale);
            view.curve.setControlX2(controlEnd.getX() * scale);
            view.curve.setControlY2(controlEnd.getY() * scale);
            view.curve.setEndX(end.getXCm() * scale);
            view.curve.setEndY(end.getYCm() * scale);
        }
    }

    private void updateShapePolygons() {
        for (ShapePolygon shape : shapes) {
            Polygon polygon = shapeViews.get(shape.getId());
            if (polygon == null) {
                continue;
            }
            List<NodePoint> shapeNodes = resolveShapeNodes(shape);
            if (shapeNodes.size() < 3) {
                continue;
            }
            polygon.getPoints().clear();
            for (NodePoint node : shapeNodes) {
                polygon.getPoints().addAll(node.getXCm() * scale, node.getYCm() * scale);
            }
        }
    }

    private void updateLayerTransforms() {
        contentLayer.setTranslateX(panX);
        contentLayer.setTranslateY(panY);
    }

    private void updateBoardAndClip() {
        double widthPx = canvasWidthCm * scale;
        double heightPx = canvasHeightCm * scale;
        boardRect.setWidth(widthPx);
        boardRect.setHeight(heightPx);
        clipRect.setWidth(widthPx);
        clipRect.setHeight(heightPx);
    }

    private List<Integer> collectNodesInSelection() {
        double minX = selectionRect.getX();
        double maxX = selectionRect.getX() + selectionRect.getWidth();
        double minY = selectionRect.getY();
        double maxY = selectionRect.getY() + selectionRect.getHeight();
        List<Integer> selected = new ArrayList<>();
        for (NodePoint node : nodes) {
            double x = node.getXCm() * scale + panX;
            double y = node.getYCm() * scale + panY;
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                selected.add(node.getId());
            }
        }
        return selected;
    }

    private List<Integer> collectGuidesInSelection() {
        double minX = selectionRect.getX();
        double maxX = selectionRect.getX() + selectionRect.getWidth();
        double minY = selectionRect.getY();
        double maxY = selectionRect.getY() + selectionRect.getHeight();
        List<Integer> selected = new ArrayList<>();
        for (Guide guide : guides) {
            if (guide.getOrientation() == Guide.Orientation.HORIZONTAL) {
                double y = guide.getPositionCm() * scale + panY;
                if (y >= minY && y <= maxY) {
                    selected.add(guide.getId());
                }
            } else {
                double x = guide.getPositionCm() * scale + panX;
                if (x >= minX && x <= maxX) {
                    selected.add(guide.getId());
                }
            }
        }
        return selected;
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
        refreshHandleLayer();
    }

    private NodePoint findNode(int nodeId) {
        for (NodePoint node : nodes) {
            if (node.getId() == nodeId) {
                return node;
            }
        }
        return null;
    }

    private void refreshHandleLayer() {
        handleLayer.getChildren().clear();
        if (mode != Mode.MOVE_NODE || selectedNodeId == null) {
            return;
        }
        NodePoint node = findNode(selectedNodeId);
        if (node == null) {
            return;
        }
        for (Edge edge : edges) {
            boolean isStart = edge.getStartNodeId() == selectedNodeId;
            boolean isEnd = edge.getEndNodeId() == selectedNodeId;
            if (!isStart && !isEnd) {
                continue;
            }
            NodePoint start = findNode(edge.getStartNodeId());
            NodePoint end = findNode(edge.getEndNodeId());
            if (start == null || end == null) {
                continue;
            }
            Point2D controlPoint = getControlPoint(edge.getId(), isStart, start, end);
            Line handleLine = new Line(
                    node.getXCm() * scale,
                    node.getYCm() * scale,
                    controlPoint.getX() * scale,
                    controlPoint.getY() * scale
            );
            handleLine.setStroke(Color.rgb(120, 120, 160, 0.7));
            handleLine.getStrokeDashArray().setAll(4.0, 4.0);
            Circle handle = new Circle(
                    controlPoint.getX() * scale,
                    controlPoint.getY() * scale,
                    HANDLE_RADIUS,
                    Color.WHITESMOKE
            );
            handle.setStroke(Color.DARKSLATEBLUE);
            handle.setStrokeWidth(1.2);
            handle.setOnMousePressed(event -> {
                if (mode == Mode.MOVE_NODE) {
                    activeHandleEdgeId = edge.getId();
                    activeHandleStart = isStart;
                    event.consume();
                }
            });
            handle.setOnMouseDragged(event -> {
                if (mode != Mode.MOVE_NODE || activeHandleEdgeId == null || activeHandleEdgeId != edge.getId()) {
                    return;
                }
                Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                Point2D cm = clampToCanvas(toCm(local.getX(), local.getY()));
                setControlPoint(edge.getId(), isStart, cm, start, end);
                handleLine.setEndX(cm.getX() * scale);
                handleLine.setEndY(cm.getY() * scale);
                handle.setCenterX(cm.getX() * scale);
                handle.setCenterY(cm.getY() * scale);
                updateConnectedEdges(edge.getStartNodeId());
                updateConnectedEdges(edge.getEndNodeId());
                event.consume();
            });
            handle.setOnMouseReleased(event -> {
                if (activeHandleEdgeId != null && activeHandleEdgeId == edge.getId()) {
                    activeHandleEdgeId = null;
                    event.consume();
                }
            });
            handleLayer.getChildren().addAll(handleLine, handle);
        }
    }

    private Point2D getControlPoint(int edgeId, boolean startSide, NodePoint start, NodePoint end) {
        EdgeControls controls = edgeControls.get(edgeId);
        if (controls == null) {
            EdgeControls defaults = buildDefaultControls(start, end);
            edgeControls.put(edgeId, defaults);
            return startSide ? defaults.start() : defaults.end();
        }
        Point2D point = startSide ? controls.start() : controls.end();
        if (point == null) {
            EdgeControls defaults = buildDefaultControls(start, end);
            edgeControls.put(edgeId, defaults);
            return startSide ? defaults.start() : defaults.end();
        }
        return point;
    }

    private void setControlPoint(int edgeId, boolean startSide, Point2D cmPoint, NodePoint start, NodePoint end) {
        EdgeControls controls = edgeControls.get(edgeId);
        if (controls == null) {
            controls = buildDefaultControls(start, end);
        }
        EdgeControls updated = startSide
                ? new EdgeControls(cmPoint, controls.end())
                : new EdgeControls(controls.start(), cmPoint);
        edgeControls.put(edgeId, updated);
    }

    private EdgeControls buildDefaultControls(NodePoint start, NodePoint end) {
        Point2D startPoint = new Point2D(start.getXCm(), start.getYCm());
        Point2D endPoint = new Point2D(end.getXCm(), end.getYCm());
        Point2D delta = endPoint.subtract(startPoint);
        Point2D controlStart = startPoint.add(delta.multiply(0.33));
        Point2D controlEnd = startPoint.add(delta.multiply(0.66));
        return new EdgeControls(controlStart, controlEnd);
    }

    private void updateControlsForNode(int nodeId, Point2D previous, Point2D current) {
        Point2D delta = current.subtract(previous);
        for (Edge edge : edges) {
            EdgeControls controls = edgeControls.get(edge.getId());
            if (controls == null) {
                continue;
            }
            boolean updated = false;
            Point2D start = controls.start();
            Point2D end = controls.end();
            if (edge.getStartNodeId() == nodeId && start != null) {
                start = start.add(delta);
                updated = true;
            }
            if (edge.getEndNodeId() == nodeId && end != null) {
                end = end.add(delta);
                updated = true;
            }
            if (updated) {
                edgeControls.put(edge.getId(), new EdgeControls(start, end));
            }
        }
    }

    private void resetControlsForNode(int nodeId) {
        for (Edge edge : edges) {
            if (edge.getStartNodeId() != nodeId && edge.getEndNodeId() != nodeId) {
                continue;
            }
            NodePoint start = findNode(edge.getStartNodeId());
            NodePoint end = findNode(edge.getEndNodeId());
            if (start == null || end == null) {
                continue;
            }
            edgeControls.put(edge.getId(), buildDefaultControls(start, end));
        }
    }

    private Point2D toCm(double xPx, double yPx) {
        return new Point2D((xPx - panX) / scale, (yPx - panY) / scale);
    }

    public Point2D toCanvasCm(Point2D localPoint) {
        return toCm(localPoint.getX(), localPoint.getY());
    }

    private Point2D clampToCanvas(Point2D cmPoint) {
        double x = Math.min(Math.max(0, cmPoint.getX()), canvasWidthCm);
        double y = Math.min(Math.max(0, cmPoint.getY()), canvasHeightCm);
        return new Point2D(x, y);
    }

    private record EdgeControls(Point2D start, Point2D end) {
    }

    private record CubicCurveView(javafx.scene.shape.CubicCurve curve) {
    }
}

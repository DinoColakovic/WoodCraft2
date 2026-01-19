package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Dimension;
import unze.ptf.woodcraft.woodcraft.model.DimensionType;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.ManualShape;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;
import unze.ptf.woodcraft.woodcraft.util.UnitConverter;

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
        SELECT,
        MOVE_NODE,
        DELETE_NODE,
        DELETE_GUIDE,
        DIMENSION,
        DELETE_DIMENSION,
        SLICE
    }

    private enum SelectableType {
        NODE,
        SHAPE,
        GUIDE,
        DIMENSION,
        MANUAL_SHAPE
    }

    private static final double NODE_RADIUS = 4.5;
    private static final double HANDLE_RADIUS = 4;

    private final Group contentLayer = new Group();
    private final Group boardLayer = new Group();
    private final Group shapeLayer = new Group();
    private final Group manualShapeLayer = new Group();
    private final Group plankLayer = new Group();
    private final Group edgeLayer = new Group();
    private final Group dimensionLayer = new Group();
    private final Group dimensionPreviewLayer = new Group();
    private final Group sliceLayer = new Group();
    private final Group handleLayer = new Group();
    private final Group guideLayer = new Group();
    private final Group nodeLayer = new Group();
    private final Rectangle selectionRect = new Rectangle();
    private final Rectangle clipRect = new Rectangle();
    private final Rectangle boardRect = new Rectangle();
    private final Rectangle viewportClip = new Rectangle();

    private final Map<Integer, Circle> nodeViews = new HashMap<>();
    private final Map<Integer, Polygon> shapeViews = new HashMap<>();
    private final Map<Integer, Polygon> manualShapeViews = new HashMap<>();
    private final Map<Integer, CubicCurveView> edgeViews = new HashMap<>();
    private final Map<Integer, DimensionView> dimensionViews = new HashMap<>();
    private final Map<Integer, Line> guideViews = new HashMap<>();

    private final List<NodePoint> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<Dimension> dimensions = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final List<ManualShape> manualShapes = new ArrayList<>();
    private final List<PlankRect> plankRects = new ArrayList<>();
    private final Map<Integer, Color> materialColors = new HashMap<>();
    private final Map<Integer, EdgeControls> edgeControls = new HashMap<>();

    private Consumer<Point2D> onCanvasClicked;
    private IntConsumer onNodeClicked;
    private IntConsumer onShapeClicked;
    private BiConsumer<Integer, Point2D> onNodeMoveFinished;
    private Consumer<List<NodePoint>> onNodesMoved;
    private Consumer<EdgeControlUpdate> onEdgeControlsChanged;
    private Consumer<List<Integer>> onDeleteNodes;
    private Consumer<List<Integer>> onDeleteGuides;
    private Consumer<DimensionDraft> onDimensionCreate;
    private BiConsumer<Integer, Point2D> onDimensionOffsetChanged;
    private Consumer<List<Integer>> onDeleteDimensions;
    private Consumer<List<Guide>> onGuidesMoved;
    private Consumer<List<Dimension>> onDimensionsMoved;
    private Consumer<List<ManualShape>> onManualShapesMoved;
    private BiConsumer<Point2D, Point2D> onSliceLine;

    private double scale = 10.0;
    private Mode mode = Mode.DRAW_SHAPE;
    private final java.util.Set<Integer> selectedNodes = new java.util.HashSet<>();
    private final java.util.Set<Integer> selectedShapes = new java.util.HashSet<>();
    private final java.util.Set<Integer> selectedGuides = new java.util.HashSet<>();
    private final java.util.Set<Integer> selectedDimensions = new java.util.HashSet<>();
    private final java.util.Set<Integer> selectedManualShapes = new java.util.HashSet<>();
    private Integer selectedNodeId;
    private Integer selectedShapeId;
    private Integer movingNodeId;
    private Integer activeHandleEdgeId;
    private boolean activeHandleStart;
    private Point2D pendingDimensionStart;
    private Point2D dimensionDragStart;
    private Integer pendingDimensionStartNodeId;
    private Point2D dimensionDragOffsetStart;
    private double canvasWidthCm = 244;
    private double canvasHeightCm = 122;
    private double panX;
    private double panY;
    private double panStartX;
    private double panStartY;
    private boolean panning;
    private double selectionStartX;
    private double selectionStartY;
    private boolean selectionDragging;
    private Point2D selectionDragStartCm;
    private java.util.Map<Integer, Point2D> selectionNodeStart = new java.util.HashMap<>();
    private java.util.Map<Integer, Double> selectionGuideStart = new java.util.HashMap<>();
    private java.util.Map<Integer, Dimension> selectionDimensionStart = new java.util.HashMap<>();
    private java.util.Map<Integer, List<Point2D>> selectionManualStart = new java.util.HashMap<>();
    private Point2D sliceStart;
    private Line slicePreview;
    private UnitSystem unitSystem = UnitSystem.CM;

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
        contentLayer.getChildren().addAll(boardLayer, shapeLayer, manualShapeLayer, plankLayer, edgeLayer,
                dimensionLayer, dimensionPreviewLayer, sliceLayer, nodeLayer, handleLayer);
        contentLayer.setClip(clipRect);
        plankLayer.setMouseTransparent(true);
        dimensionPreviewLayer.setMouseTransparent(true);
        sliceLayer.setMouseTransparent(true);
        slicePreview = new Line();
        slicePreview.setStroke(Color.rgb(80, 120, 200, 0.7));
        slicePreview.getStrokeDashArray().setAll(6.0, 4.0);
        slicePreview.setVisible(false);
        sliceLayer.getChildren().add(slicePreview);
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
                    && (mode == Mode.DELETE_NODE || mode == Mode.DELETE_GUIDE || mode == Mode.DELETE_DIMENSION
                    || mode == Mode.SELECT)
                    && (event.getTarget() == this || event.getTarget() == boardRect)) {
                selectionStartX = event.getX();
                selectionStartY = event.getY();
                selectionRect.setX(selectionStartX);
                selectionRect.setY(selectionStartY);
                selectionRect.setWidth(0);
                selectionRect.setHeight(0);
                selectionRect.setVisible(true);
                event.consume();
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY && mode == Mode.SLICE) {
                Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                sliceStart = toCm(local.getX(), local.getY());
                double x = local.getX() - panX;
                double y = local.getY() - panY;
                slicePreview.setStartX(x);
                slicePreview.setStartY(y);
                slicePreview.setEndX(x);
                slicePreview.setEndY(y);
                slicePreview.setVisible(true);
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
            if (selectionDragging) {
                Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                updateSelectionDrag(toCm(local.getX(), local.getY()));
                event.consume();
                return;
            }
            if (selectionRect.isVisible() && (mode == Mode.DELETE_NODE || mode == Mode.DELETE_GUIDE
                    || mode == Mode.DELETE_DIMENSION || mode == Mode.SELECT)) {
                double x = Math.min(selectionStartX, event.getX());
                double y = Math.min(selectionStartY, event.getY());
                double w = Math.abs(event.getX() - selectionStartX);
                double h = Math.abs(event.getY() - selectionStartY);
                selectionRect.setX(x);
                selectionRect.setY(y);
                selectionRect.setWidth(w);
                selectionRect.setHeight(h);
                event.consume();
                return;
            }
            if (slicePreview.isVisible() && mode == Mode.SLICE) {
                Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                slicePreview.setEndX(local.getX() - panX);
                slicePreview.setEndY(local.getY() - panY);
                event.consume();
            }
        });

        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.MIDDLE) {
                panning = false;
                event.consume();
                return;
            }
            if (selectionDragging) {
                finishSelectionDrag();
                event.consume();
                return;
            }
            if (selectionRect.isVisible() && (mode == Mode.DELETE_NODE || mode == Mode.DELETE_GUIDE
                    || mode == Mode.DELETE_DIMENSION || mode == Mode.SELECT)) {
                selectionRect.setVisible(false);
                if (mode == Mode.SELECT) {
                    applySelectionFromRect(event.isShiftDown());
                    event.consume();
                    return;
                }
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
                if (mode == Mode.DELETE_DIMENSION && onDeleteDimensions != null) {
                    List<Integer> dimensionIds = collectDimensionsInSelection();
                    if (!dimensionIds.isEmpty()) {
                        onDeleteDimensions.accept(dimensionIds);
                    }
                }
                event.consume();
                return;
            }
            if (slicePreview.isVisible() && mode == Mode.SLICE) {
                slicePreview.setVisible(false);
                Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                Point2D end = toCm(local.getX(), local.getY());
                if (sliceStart != null && onSliceLine != null) {
                    onSliceLine.accept(sliceStart, end);
                }
                sliceStart = null;
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
            if (mode == Mode.DIMENSION) {
                handleDimensionClick(toCm(event.getX(), event.getY()));
                event.consume();
                return;
            }
            if (mode == Mode.SELECT) {
                clearSelection();
                if (onShapeClicked != null) {
                    onShapeClicked.accept(-1);
                }
                event.consume();
                return;
            }
            if (onCanvasClicked != null) {
                onCanvasClicked.accept(toCm(event.getX(), event.getY()));
            }
        });

        addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (mode != Mode.DIMENSION || pendingDimensionStart == null) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            updateDimensionPreview(pendingDimensionStart, snapToNode(toCm(local.getX(), local.getY())));
        });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (mode != Mode.DIMENSION || pendingDimensionStart == null) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            updateDimensionPreview(pendingDimensionStart, snapToNode(toCm(local.getX(), local.getY())));
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

    public void setOnNodesMoved(Consumer<List<NodePoint>> handler) {
        this.onNodesMoved = handler;
    }

    public void setOnEdgeControlsChanged(Consumer<EdgeControlUpdate> handler) {
        this.onEdgeControlsChanged = handler;
    }

    public void setOnDeleteNodes(Consumer<List<Integer>> handler) {
        this.onDeleteNodes = handler;
    }

    public void setOnDeleteGuides(Consumer<List<Integer>> handler) {
        this.onDeleteGuides = handler;
    }

    public void setOnDimensionCreate(Consumer<DimensionDraft> handler) {
        this.onDimensionCreate = handler;
    }

    public void setOnDimensionOffsetChanged(BiConsumer<Integer, Point2D> handler) {
        this.onDimensionOffsetChanged = handler;
    }

    public void setOnDeleteDimensions(Consumer<List<Integer>> handler) {
        this.onDeleteDimensions = handler;
    }

    public void setOnGuidesMoved(Consumer<List<Guide>> handler) {
        this.onGuidesMoved = handler;
    }

    public void setOnDimensionsMoved(Consumer<List<Dimension>> handler) {
        this.onDimensionsMoved = handler;
    }

    public void setOnManualShapesMoved(Consumer<List<ManualShape>> handler) {
        this.onManualShapesMoved = handler;
    }

    public void setOnSliceLine(BiConsumer<Point2D, Point2D> handler) {
        this.onSliceLine = handler;
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

    public void setManualShapes(List<ManualShape> shapes) {
        manualShapes.clear();
        if (shapes != null) {
            manualShapes.addAll(shapes);
        }
        redraw();
    }

    public void setSelectedNode(int nodeId) {
        clearSelectionSets();
        selectedNodes.add(nodeId);
        selectedNodeId = nodeId;
        selectedShapeId = null;
        updateSelectionStyles();
    }

    public void setSelectedShape(int shapeId) {
        clearSelectionSets();
        selectedShapes.add(shapeId);
        selectedShapeId = shapeId;
        selectedNodeId = null;
        updateSelectionStyles();
    }

    public void clearSelection() {
        clearSelectionSets();
        updateSelectionStyles();
        notifySelectionChanged();
    }

    public List<Integer> getSelectedShapeIds() {
        return new ArrayList<>(selectedShapes);
    }

    public List<Integer> getSelectedNodeIds() {
        return new ArrayList<>(selectedNodes);
    }

    public List<Integer> getSelectedGuideIds() {
        return new ArrayList<>(selectedGuides);
    }

    public List<Integer> getSelectedDimensionIds() {
        return new ArrayList<>(selectedDimensions);
    }

    public List<Integer> getSelectedManualShapeIds() {
        return new ArrayList<>(selectedManualShapes);
    }

    public List<EdgeControlUpdate> getEdgeControlUpdates() {
        List<EdgeControlUpdate> updates = new ArrayList<>();
        for (Map.Entry<Integer, EdgeControls> entry : edgeControls.entrySet()) {
            EdgeControls controls = entry.getValue();
            if (controls != null) {
                updates.add(new EdgeControlUpdate(entry.getKey(), controls.start(), controls.end()));
            }
        }
        return updates;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.DRAW_SHAPE) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.DIMENSION) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.SLICE) {
            setCursor(Cursor.CROSSHAIR);
        } else if (mode == Mode.MOVE_NODE) {
            setCursor(Cursor.MOVE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
        if (mode != Mode.DELETE_NODE && mode != Mode.DELETE_GUIDE && mode != Mode.DELETE_DIMENSION
                && mode != Mode.SELECT) {
            selectionRect.setVisible(false);
        }
        if (mode != Mode.DIMENSION) {
            pendingDimensionStart = null;
            dimensionPreviewLayer.getChildren().clear();
        }
        if (mode != Mode.SLICE) {
            slicePreview.setVisible(false);
            sliceStart = null;
        }
        refreshHandleLayer();
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem == null ? UnitSystem.CM : unitSystem;
        redraw();
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
        edgeControls.clear();
        redraw();
    }

    public void setPlankRects(List<PlankRect> planks) {
        plankRects.clear();
        if (planks != null) {
            plankRects.addAll(planks);
        }
        redraw();
    }

    public void setDimensions(List<Dimension> dimensions) {
        this.dimensions.clear();
        if (dimensions != null) {
            this.dimensions.addAll(dimensions);
        }
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

    public void addDimension(Dimension dimension) {
        dimensions.add(dimension);
        drawDimension(dimension);
    }

    public void updateDimensionOffset(int dimensionId, Point2D offsetCm) {
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension dimension = dimensions.get(i);
            if (dimension.getId() == dimensionId) {
                dimensions.set(i, new Dimension(
                        dimension.getId(),
                        dimension.getDocumentId(),
                        dimension.getStartXCm(),
                        dimension.getStartYCm(),
                        dimension.getEndXCm(),
                        dimension.getEndYCm(),
                        offsetCm.getX(),
                        offsetCm.getY(),
                        dimension.getType(),
                        dimension.getStartNodeId(),
                        dimension.getEndNodeId()
                ));
                break;
            }
        }
        refreshDimensionView(dimensionId);
    }

    private void updateDimensionGeometry(int dimensionId, Point2D start, Point2D end, Point2D offset) {
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension dimension = dimensions.get(i);
            if (dimension.getId() == dimensionId) {
                dimensions.set(i, new Dimension(
                        dimension.getId(),
                        dimension.getDocumentId(),
                        start.getX(),
                        start.getY(),
                        end.getX(),
                        end.getY(),
                        offset.getX(),
                        offset.getY(),
                        dimension.getType(),
                        dimension.getStartNodeId(),
                        dimension.getEndNodeId()
                ));
                break;
            }
        }
        refreshDimensionView(dimensionId);
    }

    private void clearSelectionSets() {
        selectedNodes.clear();
        selectedShapes.clear();
        selectedGuides.clear();
        selectedDimensions.clear();
        selectedManualShapes.clear();
        selectedNodeId = null;
        selectedShapeId = null;
    }

    private void toggleSelection(SelectableType type, int id, boolean additive) {
        if (!additive) {
            clearSelectionSets();
        }
        java.util.Set<Integer> target = getSelectionSet(type);
        if (additive && target.contains(id)) {
            target.remove(id);
            if (type == SelectableType.NODE && selectedNodeId != null && selectedNodeId == id) {
                selectedNodeId = null;
            }
            if (type == SelectableType.SHAPE && selectedShapeId != null && selectedShapeId == id) {
                selectedShapeId = null;
            }
        } else {
            target.add(id);
            if (type == SelectableType.NODE) {
                selectedNodeId = id;
                selectedShapeId = null;
            } else if (type == SelectableType.SHAPE) {
                selectedShapeId = id;
                selectedNodeId = null;
            }
        }
        updateSelectionStyles();
        notifySelectionChanged();
    }

    private java.util.Set<Integer> getSelectionSet(SelectableType type) {
        return switch (type) {
            case NODE -> selectedNodes;
            case SHAPE -> selectedShapes;
            case GUIDE -> selectedGuides;
            case DIMENSION -> selectedDimensions;
            case MANUAL_SHAPE -> selectedManualShapes;
        };
    }

    private void applySelectionFromRect(boolean additive) {
        if (!additive) {
            clearSelectionSets();
        }
        selectedNodes.addAll(collectNodesInSelection());
        selectedGuides.addAll(collectGuidesInSelection());
        selectedDimensions.addAll(collectDimensionsInSelection());
        selectedShapes.addAll(collectShapesInSelection());
        selectedManualShapes.addAll(collectManualShapesInSelection());
        if (!selectedNodes.isEmpty()) {
            selectedNodeId = selectedNodes.iterator().next();
            selectedShapeId = null;
        } else if (!selectedShapes.isEmpty()) {
            selectedShapeId = selectedShapes.iterator().next();
            selectedNodeId = null;
        }
        updateSelectionStyles();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (onShapeClicked != null) {
            int shapeId = selectedShapes.isEmpty() ? -1 : selectedShapes.iterator().next();
            onShapeClicked.accept(shapeId);
        }
    }

    private void startSelectionDrag(Point2D startCm) {
        selectionDragging = true;
        selectionDragStartCm = startCm;
        selectionNodeStart = new HashMap<>();
        selectionGuideStart = new HashMap<>();
        selectionDimensionStart = new HashMap<>();
        selectionManualStart = new HashMap<>();

        for (Integer nodeId : selectedNodes) {
            NodePoint node = findNode(nodeId);
            if (node != null) {
                selectionNodeStart.put(nodeId, new Point2D(node.getXCm(), node.getYCm()));
            }
        }
        for (Integer shapeId : selectedShapes) {
            ShapePolygon shape = findShape(shapeId);
            if (shape == null) {
                continue;
            }
            List<NodePoint> shapeNodes = resolveShapeNodes(shape);
            for (NodePoint node : shapeNodes) {
                selectionNodeStart.putIfAbsent(node.getId(), new Point2D(node.getXCm(), node.getYCm()));
            }
        }
        for (Integer guideId : selectedGuides) {
            Guide guide = findGuide(guideId);
            if (guide != null) {
                selectionGuideStart.put(guideId, guide.getPositionCm());
            }
        }
        for (Integer dimensionId : selectedDimensions) {
            Dimension dimension = findDimensionById(dimensionId);
            if (dimension != null) {
                selectionDimensionStart.put(dimensionId, dimension);
            }
        }
        for (Integer manualId : selectedManualShapes) {
            ManualShape shape = findManualShape(manualId);
            if (shape != null) {
                selectionManualStart.put(manualId, new ArrayList<>(shape.getPoints()));
            }
        }
    }

    private void updateSelectionDrag(Point2D currentCm) {
        if (selectionDragStartCm == null) {
            return;
        }
        Point2D delta = currentCm.subtract(selectionDragStartCm);
        for (Map.Entry<Integer, Point2D> entry : selectionNodeStart.entrySet()) {
            Point2D start = entry.getValue();
            updateNodePosition(entry.getKey(), start.add(delta), false);
        }
        boolean guidesChanged = false;
        for (Map.Entry<Integer, Double> entry : selectionGuideStart.entrySet()) {
            Guide guide = findGuide(entry.getKey());
            if (guide == null) {
                continue;
            }
            double base = entry.getValue();
            double next = guide.getOrientation() == Guide.Orientation.HORIZONTAL
                    ? base + delta.getY()
                    : base + delta.getX();
            replaceGuide(guide, next);
            guidesChanged = true;
        }
        if (guidesChanged) {
            redrawGuides();
        }
        for (Map.Entry<Integer, Dimension> entry : selectionDimensionStart.entrySet()) {
            Dimension dimension = entry.getValue();
            Dimension current = findDimensionById(entry.getKey());
            if (current == null) {
                current = dimension;
            }
            boolean linked = current.getStartNodeId() != null || current.getEndNodeId() != null;
            Point2D start = new Point2D(current.getStartXCm(), current.getStartYCm());
            Point2D end = new Point2D(current.getEndXCm(), current.getEndYCm());
            Point2D offset = new Point2D(dimension.getOffsetXCm(), dimension.getOffsetYCm());
            if (linked) {
                Point2D normal = dimensionNormal(start, end, current.getType());
                double projected = delta.getX() * normal.getX() + delta.getY() * normal.getY();
                updateDimensionOffset(entry.getKey(), offset.add(normal.multiply(projected)));
            } else {
                updateDimensionGeometry(entry.getKey(), start.add(delta), end.add(delta), offset);
            }
        }
        for (Map.Entry<Integer, List<Point2D>> entry : selectionManualStart.entrySet()) {
            List<Point2D> moved = new ArrayList<>();
            for (Point2D point : entry.getValue()) {
                moved.add(point.add(delta));
            }
            updateManualShapePoints(entry.getKey(), moved);
        }
    }

    private void finishSelectionDrag() {
        selectionDragging = false;
        if (selectionDragStartCm == null) {
            return;
        }
        if (onNodesMoved != null && !selectionNodeStart.isEmpty()) {
            List<NodePoint> moved = new ArrayList<>();
            for (Integer nodeId : selectionNodeStart.keySet()) {
                NodePoint node = findNode(nodeId);
                if (node != null) {
                    moved.add(node);
                }
            }
            if (!moved.isEmpty()) {
                onNodesMoved.accept(moved);
            }
        }
        if (onGuidesMoved != null && !selectionGuideStart.isEmpty()) {
            List<Guide> movedGuides = new ArrayList<>();
            for (Integer guideId : selectionGuideStart.keySet()) {
                Guide guide = findGuide(guideId);
                if (guide != null) {
                    movedGuides.add(guide);
                }
            }
            onGuidesMoved.accept(movedGuides);
        }
        if (onDimensionsMoved != null && !selectionDimensionStart.isEmpty()) {
            List<Dimension> moved = new ArrayList<>();
            for (Integer dimensionId : selectionDimensionStart.keySet()) {
                Dimension dimension = findDimensionById(dimensionId);
                if (dimension != null) {
                    moved.add(dimension);
                }
            }
            onDimensionsMoved.accept(moved);
        }
        if (onManualShapesMoved != null && !selectionManualStart.isEmpty()) {
            List<ManualShape> moved = new ArrayList<>();
            for (Integer manualId : selectionManualStart.keySet()) {
                ManualShape shape = findManualShape(manualId);
                if (shape != null) {
                    moved.add(shape);
                }
            }
            onManualShapesMoved.accept(moved);
        }
        selectionDragStartCm = null;
        selectionNodeStart.clear();
        selectionGuideStart.clear();
        selectionDimensionStart.clear();
        selectionManualStart.clear();
    }

    private void redraw() {
        shapeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
        guideLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        handleLayer.getChildren().clear();
        dimensionLayer.getChildren().clear();
        dimensionPreviewLayer.getChildren().clear();
        slicePreview.setVisible(false);
        plankLayer.getChildren().clear();
        manualShapeLayer.getChildren().clear();
        nodeViews.clear();
        shapeViews.clear();
        manualShapeViews.clear();
        edgeViews.clear();
        dimensionViews.clear();
        guideViews.clear();

        updateBoardAndClip();

        for (ShapePolygon shape : shapes) {
            drawShape(shape);
        }
        for (ManualShape shape : manualShapes) {
            drawManualShape(shape);
        }
        for (PlankRect plank : plankRects) {
            drawPlank(plank);
        }
        for (Edge edge : edges) {
            drawEdge(edge);
        }
        for (Dimension dimension : dimensions) {
            drawDimension(dimension);
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
            if (mode == Mode.DIMENSION) {
                handleDimensionClick(new Point2D(node.getXCm(), node.getYCm()));
                event.consume();
                return;
            }
            if (mode == Mode.SELECT) {
                if (onNodeClicked != null) {
                    onNodeClicked.accept(node.getId());
                }
                event.consume();
                return;
            }
            if (onNodeClicked != null && mode != Mode.MOVE_NODE) {
                onNodeClicked.accept(node.getId());
            }
            event.consume();
        });
        circle.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedNodes.contains(node.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.NODE, node.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
                return;
            }
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
        ensureEdgeControls(edge, start, end);
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
        line.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedGuides.contains(guide.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.GUIDE, guide.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
            }
        });
        line.setOnMouseClicked(event -> {
            if (mode == Mode.DELETE_GUIDE && event.getButton() == MouseButton.PRIMARY && onDeleteGuides != null) {
                onDeleteGuides.accept(List.of(guide.getId()));
                event.consume();
                return;
            }
        });
        guideViews.put(guide.getId(), line);
        guideLayer.getChildren().add(line);
    }

    private void drawPlank(PlankRect plank) {
        Polygon polygon = new Polygon();
        for (Point2D point : plank.points) {
            polygon.getPoints().addAll(point.getX() * scale, point.getY() * scale);
        }
        polygon.setFill(Color.rgb(250, 200, 90, 0.18));
        polygon.setStroke(Color.rgb(200, 140, 60, 0.6));
        polygon.setStrokeWidth(1);
        plankLayer.getChildren().add(polygon);
    }

    private void drawDimension(Dimension dimension) {
        Point2D start = new Point2D(dimension.getStartXCm(), dimension.getStartYCm());
        Point2D end = new Point2D(dimension.getEndXCm(), dimension.getEndYCm());
        Point2D offset = new Point2D(dimension.getOffsetXCm(), dimension.getOffsetYCm());
        Point2D startOffset = start.add(offset);
        Point2D endOffset = end.add(offset);

        Line extensionStart = new Line(start.getX() * scale, start.getY() * scale,
                startOffset.getX() * scale, startOffset.getY() * scale);
        Line extensionEnd = new Line(end.getX() * scale, end.getY() * scale,
                endOffset.getX() * scale, endOffset.getY() * scale);
        Line dimensionLine = new Line(startOffset.getX() * scale, startOffset.getY() * scale,
                endOffset.getX() * scale, endOffset.getY() * scale);

        extensionStart.setStroke(Color.rgb(80, 80, 80, 0.7));
        extensionEnd.setStroke(Color.rgb(80, 80, 80, 0.7));
        dimensionLine.setStroke(Color.rgb(60, 60, 60, 0.9));
        dimensionLine.setStrokeWidth(1.2);
        extensionStart.setStrokeWidth(1);
        extensionEnd.setStrokeWidth(1);

        List<Polygon> arrows = buildArrowHeads(startOffset, endOffset);
        for (Polygon arrow : arrows) {
            arrow.setFill(Color.BLACK);
            arrow.setOnMousePressed(event -> {
                if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                    boolean wasSelected = selectedDimensions.contains(dimension.getId());
                    boolean additive = event.isShiftDown();
                    toggleSelection(SelectableType.DIMENSION, dimension.getId(), additive);
                    if (wasSelected && !additive) {
                        Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                        startSelectionDrag(toCm(local.getX(), local.getY()));
                    }
                    event.consume();
                }
            });
            arrow.setOnMouseClicked(event -> {
                if (mode == Mode.DELETE_DIMENSION && onDeleteDimensions != null) {
                    onDeleteDimensions.accept(List.of(dimension.getId()));
                    event.consume();
                    return;
                }
            });
        }

        String labelText = buildDimensionLabel(start, end, dimension.getType());
        Text text = new Text(labelText);
        text.setFill(Color.web("#3a3a3a"));
        Rectangle bg = new Rectangle();
        bg.setFill(Color.rgb(255, 255, 255, 0.85));
        bg.setStroke(Color.rgb(150, 150, 150, 0.6));

        Group labelGroup = new Group(bg, text);
        labelGroup.setManaged(false);
        positionLabel(labelGroup, text, bg, startOffset.midpoint(endOffset));

        labelGroup.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedDimensions.contains(dimension.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.DIMENSION, dimension.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
                return;
            }
            if (mode != Mode.DIMENSION) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            dimensionDragStart = toCm(local.getX(), local.getY());
            Dimension current = findDimensionById(dimension.getId());
            if (current != null) {
                dimensionDragOffsetStart = new Point2D(current.getOffsetXCm(), current.getOffsetYCm());
            } else {
                dimensionDragOffsetStart = offset;
            }
            event.consume();
        });
        labelGroup.setOnMouseDragged(event -> {
            if (dimensionDragStart == null || dimensionDragOffsetStart == null) {
                return;
            }
            Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
            Point2D currentCm = toCm(local.getX(), local.getY());
            Point2D normal = dimensionNormal(start, end, dimension.getType());
            Point2D delta = currentCm.subtract(dimensionDragStart);
            double projected = delta.getX() * normal.getX() + delta.getY() * normal.getY();
            Point2D newOffset = dimensionDragOffsetStart.add(normal.multiply(projected));
            updateDimensionOffset(dimension.getId(), newOffset);
            event.consume();
        });
        labelGroup.setOnMouseReleased(event -> {
            if (onDimensionOffsetChanged != null && dimensionDragOffsetStart != null) {
                Dimension current = findDimensionById(dimension.getId());
                if (current != null) {
                    onDimensionOffsetChanged.accept(current.getId(),
                            new Point2D(current.getOffsetXCm(), current.getOffsetYCm()));
                }
            }
            dimensionDragStart = null;
            dimensionDragOffsetStart = null;
            event.consume();
        });
        labelGroup.setOnMouseClicked(event -> {
            if (mode == Mode.DELETE_DIMENSION && onDeleteDimensions != null) {
                onDeleteDimensions.accept(List.of(dimension.getId()));
                event.consume();
                return;
            }
        });

        dimensionLine.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedDimensions.contains(dimension.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.DIMENSION, dimension.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
            }
        });
        dimensionLine.setOnMouseClicked(event -> {
            if (mode == Mode.DELETE_DIMENSION && onDeleteDimensions != null) {
                onDeleteDimensions.accept(List.of(dimension.getId()));
                event.consume();
                return;
            }
        });

        dimensionLayer.getChildren().addAll(extensionStart, extensionEnd, dimensionLine);
        dimensionLayer.getChildren().addAll(arrows);
        dimensionLayer.getChildren().add(labelGroup);
        dimensionViews.put(dimension.getId(), new DimensionView(extensionStart, extensionEnd, dimensionLine, arrows, labelGroup));
    }

    private void positionLabel(Group labelGroup, Text text, Rectangle bg, Point2D centerCm) {
        double padding = 3;
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        bg.setWidth(textWidth + padding * 2);
        bg.setHeight(textHeight + padding * 2);
        text.setX(padding);
        text.setY(textHeight + padding - 2);
        labelGroup.setLayoutX(centerCm.getX() * scale - bg.getWidth() / 2);
        labelGroup.setLayoutY(centerCm.getY() * scale - bg.getHeight() / 2);
    }

    private String buildDimensionLabel(Point2D start, Point2D end, DimensionType type) {
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double lengthCm = switch (type) {
            case HORIZONTAL -> Math.abs(dx);
            case VERTICAL -> Math.abs(dy);
            case ALIGNED -> Math.hypot(dx, dy);
        };
        double value = UnitConverter.fromCm(lengthCm, unitSystem);
        String unitLabel = unitSystem == UnitSystem.IN ? "in" : "cm";
        String format = unitSystem == UnitSystem.IN ? "%.2f" : "%.1f";
        return String.format(format + " %s", value, unitLabel);
    }

    private List<Polygon> buildArrowHeads(Point2D start, Point2D end) {
        List<Polygon> arrows = new ArrayList<>();
        Point2D direction = end.subtract(start);
        double length = Math.hypot(direction.getX(), direction.getY());
        if (length == 0) {
            return arrows;
        }
        Point2D dir = new Point2D(direction.getX() / length, direction.getY() / length);
        Point2D normal = new Point2D(-dir.getY(), dir.getX());
        double sizeCm = 0.4;
        double halfWidthCm = sizeCm * 0.6;
        Polygon startArrow = buildArrowTriangle(start, dir, normal, sizeCm, halfWidthCm);
        Polygon endArrow = buildArrowTriangle(end, dir.multiply(-1), normal, sizeCm, halfWidthCm);
        arrows.add(startArrow);
        arrows.add(endArrow);
        return arrows;
    }

    private void handleDimensionClick(Point2D cmPoint) {
        Integer nodeId = findNodeNear(cmPoint, 8.0 / scale);
        Point2D snapped = snapToNode(clampToCanvas(cmPoint));
        if (pendingDimensionStart == null) {
            pendingDimensionStart = snapped;
            pendingDimensionStartNodeId = nodeId;
            dimensionPreviewLayer.getChildren().clear();
            return;
        }
        Point2D start = pendingDimensionStart;
        Point2D end = snapped;
        Integer startNodeId = pendingDimensionStartNodeId;
        Integer endNodeId = nodeId;
        pendingDimensionStart = null;
        pendingDimensionStartNodeId = null;
        dimensionPreviewLayer.getChildren().clear();
        Point2D offset = defaultDimensionOffset(start, end);
        if (onDimensionCreate != null) {
            onDimensionCreate.accept(new DimensionDraft(start, end, offset, DimensionType.ALIGNED, startNodeId, endNodeId));
        }
    }

    private Point2D defaultDimensionOffset(Point2D start, Point2D end) {
        Point2D normal = dimensionNormal(start, end, DimensionType.ALIGNED);
        if (normal.magnitude() == 0) {
            return new Point2D(0, -1.5);
        }
        return normal.multiply(1.2);
    }

    private Point2D dimensionNormal(Point2D start, Point2D end, DimensionType type) {
        Point2D delta = end.subtract(start);
        double length = Math.hypot(delta.getX(), delta.getY());
        if (length == 0) {
            return new Point2D(0, -1);
        }
        return switch (type) {
            case HORIZONTAL -> new Point2D(0, -1);
            case VERTICAL -> new Point2D(-1, 0);
            case ALIGNED -> new Point2D(-delta.getY() / length, delta.getX() / length);
        };
    }

    private void updateDimensionPreview(Point2D start, Point2D end) {
        dimensionPreviewLayer.getChildren().clear();
        if (start == null || end == null) {
            return;
        }
        Point2D offset = defaultDimensionOffset(start, end);
        Point2D startOffset = start.add(offset);
        Point2D endOffset = end.add(offset);

        Line extensionStart = new Line(start.getX() * scale, start.getY() * scale,
                startOffset.getX() * scale, startOffset.getY() * scale);
        Line extensionEnd = new Line(end.getX() * scale, end.getY() * scale,
                endOffset.getX() * scale, endOffset.getY() * scale);
        Line dimensionLine = new Line(startOffset.getX() * scale, startOffset.getY() * scale,
                endOffset.getX() * scale, endOffset.getY() * scale);

        extensionStart.setStroke(Color.rgb(80, 80, 80, 0.5));
        extensionEnd.setStroke(Color.rgb(80, 80, 80, 0.5));
        dimensionLine.setStroke(Color.rgb(60, 60, 60, 0.75));
        dimensionLine.setStrokeWidth(1.1);
        extensionStart.setStrokeWidth(1);
        extensionEnd.setStrokeWidth(1);

        List<Polygon> arrows = buildArrowHeads(startOffset, endOffset);
        for (Polygon arrow : arrows) {
            arrow.setFill(Color.BLACK);
        }

        Text text = new Text(buildDimensionLabel(start, end, DimensionType.ALIGNED));
        text.setFill(Color.web("#3a3a3a"));
        Rectangle bg = new Rectangle();
        bg.setFill(Color.rgb(255, 255, 255, 0.7));
        bg.setStroke(Color.rgb(150, 150, 150, 0.45));
        Group label = new Group(bg, text);
        label.setManaged(false);
        positionLabel(label, text, bg, startOffset.midpoint(endOffset));

        dimensionPreviewLayer.getChildren().addAll(extensionStart, extensionEnd, dimensionLine);
        dimensionPreviewLayer.getChildren().addAll(arrows);
        dimensionPreviewLayer.getChildren().add(label);
    }

    private Polygon buildArrowTriangle(Point2D tip, Point2D dir, Point2D normal, double sizeCm, double halfWidthCm) {
        Point2D base = tip.add(dir.multiply(sizeCm));
        Point2D p1 = base.add(normal.multiply(halfWidthCm));
        Point2D p2 = base.subtract(normal.multiply(halfWidthCm));
        Polygon triangle = new Polygon(
                tip.getX() * scale, tip.getY() * scale,
                p1.getX() * scale, p1.getY() * scale,
                p2.getX() * scale, p2.getY() * scale
        );
        triangle.setFill(Color.BLACK);
        return triangle;
    }

    private void refreshDimensionView(int dimensionId) {
        Dimension dimension = findDimensionById(dimensionId);
        DimensionView view = dimensionViews.get(dimensionId);
        if (dimension == null || view == null) {
            return;
        }
        Point2D start = new Point2D(dimension.getStartXCm(), dimension.getStartYCm());
        Point2D end = new Point2D(dimension.getEndXCm(), dimension.getEndYCm());
        Point2D offset = new Point2D(dimension.getOffsetXCm(), dimension.getOffsetYCm());
        Point2D startOffset = start.add(offset);
        Point2D endOffset = end.add(offset);

        view.extensionStart.setStartX(start.getX() * scale);
        view.extensionStart.setStartY(start.getY() * scale);
        view.extensionStart.setEndX(startOffset.getX() * scale);
        view.extensionStart.setEndY(startOffset.getY() * scale);

        view.extensionEnd.setStartX(end.getX() * scale);
        view.extensionEnd.setStartY(end.getY() * scale);
        view.extensionEnd.setEndX(endOffset.getX() * scale);
        view.extensionEnd.setEndY(endOffset.getY() * scale);

        view.dimensionLine.setStartX(startOffset.getX() * scale);
        view.dimensionLine.setStartY(startOffset.getY() * scale);
        view.dimensionLine.setEndX(endOffset.getX() * scale);
        view.dimensionLine.setEndY(endOffset.getY() * scale);
        updateArrowPolygons(view.arrows, startOffset, endOffset);

        Text text = null;
        Rectangle bg = null;
        for (var child : view.labelGroup.getChildren()) {
            if (child instanceof Text) {
                text = (Text) child;
            } else if (child instanceof Rectangle) {
                bg = (Rectangle) child;
            }
        }
        if (text != null && bg != null) {
            text.setText(buildDimensionLabel(start, end, dimension.getType()));
            positionLabel(view.labelGroup, text, bg, startOffset.midpoint(endOffset));
        }
    }

    private void updateArrowPolygons(List<Polygon> arrows, Point2D start, Point2D end) {
        if (arrows == null || arrows.size() < 2) {
            return;
        }
        Point2D direction = end.subtract(start);
        double length = Math.hypot(direction.getX(), direction.getY());
        if (length == 0) {
            return;
        }
        Point2D dir = new Point2D(direction.getX() / length, direction.getY() / length);
        Point2D normal = new Point2D(-dir.getY(), dir.getX());
        double sizeCm = 0.4;
        double halfWidthCm = sizeCm * 0.6;
        Polygon startArrow = arrows.get(0);
        Polygon endArrow = arrows.get(1);
        updateArrowTriangle(startArrow, start, dir, normal, sizeCm, halfWidthCm);
        updateArrowTriangle(endArrow, end, dir.multiply(-1), normal, sizeCm, halfWidthCm);
    }

    private void updateArrowTriangle(Polygon triangle, Point2D tip, Point2D dir, Point2D normal,
                                     double sizeCm, double halfWidthCm) {
        Point2D base = tip.add(dir.multiply(sizeCm));
        Point2D p1 = base.add(normal.multiply(halfWidthCm));
        Point2D p2 = base.subtract(normal.multiply(halfWidthCm));
        triangle.getPoints().setAll(
                tip.getX() * scale, tip.getY() * scale,
                p1.getX() * scale, p1.getY() * scale,
                p2.getX() * scale, p2.getY() * scale
        );
    }

    private Dimension findDimensionById(int dimensionId) {
        for (Dimension dimension : dimensions) {
            if (dimension.getId() == dimensionId) {
                return dimension;
            }
        }
        return null;
    }

    private Point2D snapToNode(Point2D cmPoint) {
        Integer nodeId = findNodeNear(cmPoint, 8.0 / scale);
        if (nodeId == null) {
            return cmPoint;
        }
        NodePoint node = findNode(nodeId);
        if (node == null) {
            return cmPoint;
        }
        return new Point2D(node.getXCm(), node.getYCm());
    }

    private Integer findNodeNear(Point2D cmPoint, double thresholdCm) {
        for (NodePoint node : nodes) {
            double dx = node.getXCm() - cmPoint.getX();
            double dy = node.getYCm() - cmPoint.getY();
            if (Math.hypot(dx, dy) <= thresholdCm) {
                return node.getId();
            }
        }
        return null;
    }

    private void redrawGuides() {
        guideLayer.getChildren().clear();
        guideViews.clear();
        for (Guide guide : guides) {
            drawGuide(guide);
        }
    }

    private void drawShape(ShapePolygon shape) {
        List<Point2D> sampled = buildSampledPolygon(shape);
        if (sampled.size() < 3) {
            return;
        }
        Polygon polygon = new Polygon();
        for (Point2D point : sampled) {
            polygon.getPoints().addAll(point.getX() * scale, point.getY() * scale);
        }
        Color base = shape.getMaterialId() == null ? Color.LIGHTGRAY : materialColors.get(shape.getMaterialId());
        if (base == null) {
            base = Color.LIGHTGRAY;
        }
        polygon.setFill(new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.25));
        polygon.setStroke(Color.GRAY);
        polygon.setStrokeWidth(1);
        polygon.setOnMouseEntered(event -> {
            if (!selectedShapes.contains(shape.getId())) {
                polygon.setStroke(Color.DEEPSKYBLUE);
                polygon.setStrokeWidth(1.6);
            }
        });
        polygon.setOnMouseExited(event -> updateSelectionStyles());
        polygon.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedShapes.contains(shape.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.SHAPE, shape.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
            }
        });
        polygon.setOnMouseClicked(event -> {
            if (mode == Mode.SELECT) {
                if (onShapeClicked != null) {
                    onShapeClicked.accept(shape.getId());
                }
                event.consume();
                return;
            }
            if (onShapeClicked != null) {
                onShapeClicked.accept(shape.getId());
            }
            event.consume();
        });
        shapeViews.put(shape.getId(), polygon);
        shapeLayer.getChildren().add(polygon);
    }

    private void drawManualShape(ManualShape shape) {
        if (shape.getPoints() == null || shape.getPoints().size() < 3) {
            return;
        }
        Polygon polygon = new Polygon();
        for (Point2D point : shape.getPoints()) {
            polygon.getPoints().addAll(point.getX() * scale, point.getY() * scale);
        }
        polygon.setFill(Color.rgb(120, 160, 220, 0.2));
        polygon.setStroke(Color.rgb(60, 90, 140, 0.8));
        polygon.setStrokeWidth(1.2);
        polygon.setOnMouseEntered(event -> {
            if (!selectedManualShapes.contains(shape.getId())) {
                polygon.setStroke(Color.DEEPSKYBLUE);
                polygon.setStrokeWidth(1.6);
            }
        });
        polygon.setOnMouseExited(event -> updateSelectionStyles());
        polygon.setOnMousePressed(event -> {
            if (mode == Mode.SELECT && event.getButton() == MouseButton.PRIMARY) {
                boolean wasSelected = selectedManualShapes.contains(shape.getId());
                boolean additive = event.isShiftDown();
                toggleSelection(SelectableType.MANUAL_SHAPE, shape.getId(), additive);
                if (wasSelected && !additive) {
                    Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
                    startSelectionDrag(toCm(local.getX(), local.getY()));
                }
                event.consume();
            }
        });
        polygon.setOnMouseClicked(event -> {
            if (mode == Mode.SELECT) {
                event.consume();
                return;
            }
        });
        manualShapeViews.put(shape.getId(), polygon);
        manualShapeLayer.getChildren().add(polygon);
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
        updateDimensionsForNode(nodeId, new Point2D(xCm, yCm));
        Circle circle = nodeViews.get(nodeId);
        if (circle != null) {
            circle.setCenterX(xCm * scale);
            circle.setCenterY(yCm * scale);
        }
        updateConnectedEdges(nodeId);
        updateShapePolygons();
        refreshHandleLayer();
    }

    private void updateDimensionsForNode(int nodeId, Point2D position) {
        boolean changed = false;
        for (int i = 0; i < dimensions.size(); i++) {
            Dimension dimension = dimensions.get(i);
            boolean isStart = dimension.getStartNodeId() != null && dimension.getStartNodeId() == nodeId;
            boolean isEnd = dimension.getEndNodeId() != null && dimension.getEndNodeId() == nodeId;
            if (!isStart && !isEnd) {
                continue;
            }
            double startX = isStart ? position.getX() : dimension.getStartXCm();
            double startY = isStart ? position.getY() : dimension.getStartYCm();
            double endX = isEnd ? position.getX() : dimension.getEndXCm();
            double endY = isEnd ? position.getY() : dimension.getEndYCm();
            dimensions.set(i, new Dimension(
                    dimension.getId(),
                    dimension.getDocumentId(),
                    startX,
                    startY,
                    endX,
                    endY,
                    dimension.getOffsetXCm(),
                    dimension.getOffsetYCm(),
                    dimension.getType(),
                    dimension.getStartNodeId(),
                    dimension.getEndNodeId()
            ));
            refreshDimensionView(dimension.getId());
            changed = true;
        }
        if (changed) {
            redrawGuides();
        }
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
            List<Point2D> sampled = buildSampledPolygon(shape);
            if (sampled.size() < 3) {
                continue;
            }
            polygon.getPoints().clear();
            for (Point2D point : sampled) {
                polygon.getPoints().addAll(point.getX() * scale, point.getY() * scale);
            }
        }
    }

    private List<Point2D> buildSampledPolygon(ShapePolygon shape) {
        List<NodePoint> shapeNodes = resolveShapeNodes(shape);
        if (shapeNodes.size() < 2) {
            return List.of();
        }
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < shapeNodes.size(); i++) {
            NodePoint start = shapeNodes.get(i);
            NodePoint end = shapeNodes.get((i + 1) % shapeNodes.size());
            Edge edge = findEdgeBetween(start.getId(), end.getId());
            if (edge == null) {
                if (points.isEmpty()) {
                    points.add(new Point2D(start.getXCm(), start.getYCm()));
                }
                points.add(new Point2D(end.getXCm(), end.getYCm()));
                continue;
            }
            ensureEdgeControls(edge, start, end);
            EdgeControls controls = edgeControls.get(edge.getId());
            if (controls == null) {
                if (points.isEmpty()) {
                    points.add(new Point2D(start.getXCm(), start.getYCm()));
                }
                points.add(new Point2D(end.getXCm(), end.getYCm()));
                continue;
            }
            Point2D c1 = edge.getStartNodeId() == start.getId() ? controls.start() : controls.end();
            Point2D c2 = edge.getStartNodeId() == start.getId() ? controls.end() : controls.start();
            sampleCubic(points,
                    new Point2D(start.getXCm(), start.getYCm()),
                    c1,
                    c2,
                    new Point2D(end.getXCm(), end.getYCm()),
                    18);
        }
        return points;
    }

    private Edge findEdgeBetween(int nodeA, int nodeB) {
        for (Edge edge : edges) {
            if ((edge.getStartNodeId() == nodeA && edge.getEndNodeId() == nodeB)
                    || (edge.getStartNodeId() == nodeB && edge.getEndNodeId() == nodeA)) {
                return edge;
            }
        }
        return null;
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

    private List<Integer> collectDimensionsInSelection() {
        double minX = selectionRect.getX();
        double maxX = selectionRect.getX() + selectionRect.getWidth();
        double minY = selectionRect.getY();
        double maxY = selectionRect.getY() + selectionRect.getHeight();
        List<Integer> selected = new ArrayList<>();
        for (Map.Entry<Integer, DimensionView> entry : dimensionViews.entrySet()) {
            DimensionView view = entry.getValue();
            if (view == null) {
                continue;
            }
            if (view.labelGroup != null) {
                var bounds = view.labelGroup.getBoundsInParent();
                double centerX = bounds.getMinX() + bounds.getWidth() / 2;
                double centerY = bounds.getMinY() + bounds.getHeight() / 2;
                if (centerX >= minX && centerX <= maxX && centerY >= minY && centerY <= maxY) {
                    selected.add(entry.getKey());
                    continue;
                }
            }
            Line line = view.dimensionLine;
            if (line != null) {
                double midX = (line.getStartX() + line.getEndX()) / 2.0;
                double midY = (line.getStartY() + line.getEndY()) / 2.0;
                if (midX >= minX && midX <= maxX && midY >= minY && midY <= maxY) {
                    selected.add(entry.getKey());
                }
            }
        }
        return selected;
    }

    private List<Integer> collectShapesInSelection() {
        double minX = selectionRect.getX();
        double maxX = selectionRect.getX() + selectionRect.getWidth();
        double minY = selectionRect.getY();
        double maxY = selectionRect.getY() + selectionRect.getHeight();
        List<Integer> selected = new ArrayList<>();
        for (Map.Entry<Integer, Polygon> entry : shapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (polygon == null) {
                continue;
            }
            var bounds = polygon.getBoundsInParent();
            if (bounds.getMaxX() >= minX && bounds.getMinX() <= maxX
                    && bounds.getMaxY() >= minY && bounds.getMinY() <= maxY) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private List<Integer> collectManualShapesInSelection() {
        double minX = selectionRect.getX();
        double maxX = selectionRect.getX() + selectionRect.getWidth();
        double minY = selectionRect.getY();
        double maxY = selectionRect.getY() + selectionRect.getHeight();
        List<Integer> selected = new ArrayList<>();
        for (Map.Entry<Integer, Polygon> entry : manualShapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (polygon == null) {
                continue;
            }
            var bounds = polygon.getBoundsInParent();
            if (bounds.getMaxX() >= minX && bounds.getMinX() <= maxX
                    && bounds.getMaxY() >= minY && bounds.getMinY() <= maxY) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    private void updateSelectionStyles() {
        for (Map.Entry<Integer, Circle> entry : nodeViews.entrySet()) {
            Circle circle = entry.getValue();
            if (selectedNodes.contains(entry.getKey())) {
                circle.setFill(Color.ORANGE);
            } else {
                circle.setFill(Color.DODGERBLUE);
            }
        }
        for (Map.Entry<Integer, Polygon> entry : shapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (selectedShapes.contains(entry.getKey())) {
                polygon.setStroke(Color.RED);
                polygon.setStrokeWidth(2);
            } else {
                polygon.setStroke(Color.GRAY);
                polygon.setStrokeWidth(1);
            }
        }
        for (Map.Entry<Integer, Polygon> entry : manualShapeViews.entrySet()) {
            Polygon polygon = entry.getValue();
            if (selectedManualShapes.contains(entry.getKey())) {
                polygon.setStroke(Color.RED);
                polygon.setStrokeWidth(2);
            } else {
                polygon.setStroke(Color.rgb(60, 90, 140, 0.8));
                polygon.setStrokeWidth(1.2);
            }
        }
        for (Map.Entry<Integer, Line> entry : guideViews.entrySet()) {
            Line line = entry.getValue();
            if (selectedGuides.contains(entry.getKey())) {
                line.setStroke(Color.ORANGE);
                line.setStrokeWidth(1.6);
            } else {
                line.setStroke(Color.rgb(200, 80, 80, 0.65));
                line.setStrokeWidth(1);
            }
        }
        for (Map.Entry<Integer, DimensionView> entry : dimensionViews.entrySet()) {
            DimensionView view = entry.getValue();
            boolean selected = selectedDimensions.contains(entry.getKey());
            if (view.extensionStart != null) {
                view.extensionStart.setStroke(selected ? Color.ORANGE : Color.rgb(80, 80, 80, 0.7));
                view.extensionStart.setStrokeWidth(selected ? 1.4 : 1);
            }
            if (view.extensionEnd != null) {
                view.extensionEnd.setStroke(selected ? Color.ORANGE : Color.rgb(80, 80, 80, 0.7));
                view.extensionEnd.setStrokeWidth(selected ? 1.4 : 1);
            }
            if (view.dimensionLine != null) {
                view.dimensionLine.setStroke(selected ? Color.ORANGE : Color.rgb(60, 60, 60, 0.9));
                view.dimensionLine.setStrokeWidth(selected ? 1.6 : 1.2);
            }
            if (view.arrows != null) {
                for (Polygon arrow : view.arrows) {
                    arrow.setFill(selected ? Color.ORANGE : Color.BLACK);
                }
            }
            if (view.labelGroup != null && !view.labelGroup.getChildren().isEmpty()) {
                if (view.labelGroup.getChildren().get(0) instanceof Rectangle bg) {
                    bg.setStroke(selected ? Color.ORANGE : Color.rgb(150, 150, 150, 0.6));
                }
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

    private Guide findGuide(int guideId) {
        for (Guide guide : guides) {
            if (guide.getId() == guideId) {
                return guide;
            }
        }
        return null;
    }

    private ShapePolygon findShape(int shapeId) {
        for (ShapePolygon shape : shapes) {
            if (shape.getId() == shapeId) {
                return shape;
            }
        }
        return null;
    }

    private ManualShape findManualShape(int manualId) {
        for (ManualShape shape : manualShapes) {
            if (shape.getId() == manualId) {
                return shape;
            }
        }
        return null;
    }

    private void replaceGuide(Guide guide, double positionCm) {
        for (int i = 0; i < guides.size(); i++) {
            if (guides.get(i).getId() == guide.getId()) {
                guides.set(i, new Guide(guide.getId(), guide.getDocumentId(), guide.getOrientation(), positionCm));
                return;
            }
        }
    }

    private void updateManualShapePoints(int manualId, List<Point2D> points) {
        for (int i = 0; i < manualShapes.size(); i++) {
            ManualShape shape = manualShapes.get(i);
            if (shape.getId() == manualId) {
                manualShapes.set(i, new ManualShape(shape.getId(), shape.getDocumentId(), points));
                break;
            }
        }
        Polygon polygon = manualShapeViews.get(manualId);
        if (polygon != null) {
            polygon.getPoints().clear();
            for (Point2D point : points) {
                polygon.getPoints().addAll(point.getX() * scale, point.getY() * scale);
            }
        }
    }

    private void refreshHandleLayer() {
        handleLayer.getChildren().clear();
        Integer handleNodeId = selectedNodeId;
        if (handleNodeId == null && selectedNodes.size() == 1) {
            handleNodeId = selectedNodes.iterator().next();
        }
        if (mode != Mode.MOVE_NODE || handleNodeId == null) {
            return;
        }
        NodePoint node = findNode(handleNodeId);
        if (node == null) {
            return;
        }
        for (Edge edge : edges) {
            boolean isStart = edge.getStartNodeId() == handleNodeId;
            boolean isEnd = edge.getEndNodeId() == handleNodeId;
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
                    if (onEdgeControlsChanged != null) {
                        EdgeControls controls = edgeControls.get(edge.getId());
                        if (controls != null) {
                            onEdgeControlsChanged.accept(new EdgeControlUpdate(edge.getId(), controls.start(), controls.end()));
                        }
                    }
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

    private void ensureEdgeControls(Edge edge, NodePoint start, NodePoint end) {
        if (edgeControls.containsKey(edge.getId())) {
            return;
        }
        if (edge.getControlStartXCm() != null && edge.getControlStartYCm() != null
                && edge.getControlEndXCm() != null && edge.getControlEndYCm() != null) {
            edgeControls.put(edge.getId(), new EdgeControls(
                    new Point2D(edge.getControlStartXCm(), edge.getControlStartYCm()),
                    new Point2D(edge.getControlEndXCm(), edge.getControlEndYCm())
            ));
        } else {
            edgeControls.put(edge.getId(), buildDefaultControls(start, end));
        }
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

    public record DimensionDraft(Point2D start, Point2D end, Point2D offset, DimensionType type,
                                 Integer startNodeId, Integer endNodeId) {
    }

    private record EdgeControls(Point2D start, Point2D end) {
    }

    public record EdgeControlUpdate(int edgeId, Point2D start, Point2D end) {
    }

    private record CubicCurveView(javafx.scene.shape.CubicCurve curve) {
    }

    private record DimensionView(Line extensionStart, Line extensionEnd, Line dimensionLine,
                                 List<Polygon> arrows, Group labelGroup) {
    }

    public static final class PlankRect {
        public final List<Point2D> points;

        public PlankRect(List<Point2D> points) {
            this.points = points;
        }
    }
    public boolean isNodeLayerVisible() {
    return nodeLayer.isVisible();
}

public void setNodeLayerVisible(boolean visible) {
    nodeLayer.setVisible(visible);
}
    public boolean isShapeLayerVisible() {
        return shapeLayer.isVisible();
    }   

    

}

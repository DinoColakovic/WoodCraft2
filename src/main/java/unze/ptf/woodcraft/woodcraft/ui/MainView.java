package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import unze.ptf.woodcraft.woodcraft.dao.DocumentDao;
import unze.ptf.woodcraft.woodcraft.dao.EdgeDao;
import unze.ptf.woodcraft.woodcraft.dao.GuideDao;
import unze.ptf.woodcraft.woodcraft.dao.MaterialDao;
import unze.ptf.woodcraft.woodcraft.dao.NodeDao;
import unze.ptf.woodcraft.woodcraft.dao.ShapeDao;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;
import unze.ptf.woodcraft.woodcraft.service.AuthService;
import unze.ptf.woodcraft.woodcraft.service.EstimationService;
import unze.ptf.woodcraft.woodcraft.service.EstimationSummary;
import unze.ptf.woodcraft.woodcraft.service.GeometryService;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainView {
    private static final double RULER_SIZE = 24;

    private final SessionManager sessionManager;
    private final AuthService authService;
    private final UserDao userDao;
    private final MaterialDao materialDao;
    private final DocumentDao documentDao;
    private final NodeDao nodeDao;
    private final EdgeDao edgeDao;
    private final GuideDao guideDao;
    private final ShapeDao shapeDao;
    private final GeometryService geometryService;
    private final EstimationService estimationService;
    private final SceneNavigator navigator;

    private final BorderPane root = new BorderPane();
    private final CanvasPane canvasPane = new CanvasPane();
    private final RulerPane horizontalRuler = new RulerPane(RulerPane.Orientation.HORIZONTAL);
    private final RulerPane verticalRuler = new RulerPane(RulerPane.Orientation.VERTICAL);

    private final ListView<Material> materialsList = new ListView<>();
    private final ComboBox<Material> defaultMaterial = new ComboBox<>();
    private final ListView<String> summaryList = new ListView<>();
    private final Label totalCostLabel = new Label("Total: $0.00");

    private double scale = 10.0;
    private Document currentDocument;
    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final Map<Integer, Color> materialColors = new HashMap<>();
    private Integer selectedNodeId;
    private Integer selectedShapeId;
    private CanvasPane.Mode currentTool = CanvasPane.Mode.PEN;

    public MainView(SessionManager sessionManager, AuthService authService, UserDao userDao, MaterialDao materialDao,
                    DocumentDao documentDao, NodeDao nodeDao, EdgeDao edgeDao, GuideDao guideDao, ShapeDao shapeDao,
                    GeometryService geometryService, EstimationService estimationService, SceneNavigator navigator) {
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.userDao = userDao;
        this.materialDao = materialDao;
        this.documentDao = documentDao;
        this.nodeDao = nodeDao;
        this.edgeDao = edgeDao;
        this.guideDao = guideDao;
        this.shapeDao = shapeDao;
        this.geometryService = geometryService;
        this.estimationService = estimationService;
        this.navigator = navigator;

        setupLayout();
        loadUserData();
    }

    public Parent getRoot() {
        return root;
    }

    private void setupLayout() {
        MenuBar menuBar = buildMenu();
        ToolBar toolBar = buildToolBar();
        VBox top = new VBox(menuBar, toolBar);
        root.setTop(top);

        BorderPane canvasRegion = new BorderPane();
        horizontalRuler.setHeight(RULER_SIZE);
        verticalRuler.setWidth(RULER_SIZE);

        canvasRegion.setTop(horizontalRuler);
        canvasRegion.setLeft(verticalRuler);
        canvasRegion.setCenter(canvasPane);

        root.setCenter(canvasRegion);
        root.setRight(buildSidebar());

        canvasPane.setOnCanvasClicked(this::handleCanvasClick);
        canvasPane.setOnNodeClicked(this::handleNodeClick);
        canvasPane.setOnShapeClicked(this::handleShapeClick);
        setupGuideDragging();
    }

    private MenuBar buildMenu() {
        Menu file = new Menu("File");
        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(event -> {
            authService.logout();
            navigator.showLogin();
        });
        file.getItems().add(logout);

        Menu edit = new Menu("Edit");
        Menu view = new Menu("View");
        Menu window = new Menu("Window");
        Menu help = new Menu("Help");

        if (sessionManager.getCurrentUser() != null && sessionManager.getCurrentUser().getRole() == Role.ADMIN) {
            Menu admin = new Menu("Admin");
            MenuItem manageUsers = new MenuItem("User Management");
            manageUsers.setOnAction(event -> new UserManagementDialog(userDao).showAndWait());
            admin.getItems().add(manageUsers);
            return new MenuBar(file, edit, view, window, help, admin);
        }

        return new MenuBar(file, edit, view, window, help);
    }

    private ToolBar buildToolBar() {
        ToggleGroup tools = new ToggleGroup();
        ToggleButton selectTool = new ToggleButton("Select");
        selectTool.setToggleGroup(tools);
        ToggleButton penTool = new ToggleButton("Pen");
        penTool.setToggleGroup(tools);
        ToggleButton eraseTool = new ToggleButton("Erase");
        eraseTool.setToggleGroup(tools);
        tools.selectToggle(penTool);
        setTool(CanvasPane.Mode.PEN);

        tools.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == selectTool) {
                setTool(CanvasPane.Mode.SELECT);
            } else if (newVal == eraseTool) {
                setTool(CanvasPane.Mode.ERASE);
            } else {
                setTool(CanvasPane.Mode.PEN);
            }
        });

        Button zoomIn = new Button("Zoom +");
        zoomIn.setOnAction(event -> updateScale(scale + 2));

        Button zoomOut = new Button("Zoom -");
        zoomOut.setOnAction(event -> updateScale(Math.max(2, scale - 2)));

        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(selectTool, penTool, eraseTool, new Separator(), zoomIn, zoomOut);
        return toolBar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f5f5f5;");

        Label materialsLabel = new Label("Materials");
        materialsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        materialsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Material item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + item.getType() + ")");
            }
        });

        Button addMaterial = new Button("Add Material");
        addMaterial.setOnAction(event -> {
            MaterialDialog dialog = new MaterialDialog(sessionManager.getCurrentUser().getId());
            dialog.showAndWait().ifPresent(material -> {
                int id = materialDao.create(material);
                refreshMaterials();
                selectDefaultMaterialById(id);
            });
        });

        Label defaultLabel = new Label("Default Material for Shapes");
        defaultMaterial.setOnAction(event -> updateSummary());

        Label summaryLabel = new Label("Summary");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox summaryBox = new VBox(6, summaryList, totalCostLabel);
        VBox.setVgrow(summaryList, Priority.ALWAYS);

        sidebar.getChildren().addAll(materialsLabel, materialsList, addMaterial, new Separator(),
                defaultLabel, defaultMaterial, new Separator(), summaryLabel, summaryBox);
        VBox.setVgrow(materialsList, Priority.ALWAYS);
        VBox.setVgrow(summaryBox, Priority.ALWAYS);
        return sidebar;
    }

    private void loadUserData() {
        int userId = sessionManager.getCurrentUser().getId();
        currentDocument = documentDao.findFirstByUser(userId)
                .orElseGet(() -> new Document(documentDao.createDocument(userId, "Default Project"), userId, "Default Project"));
        List<NodePoint> nodes = nodeDao.findByDocument(currentDocument.getId());
        List<Edge> edges = edgeDao.findByDocument(currentDocument.getId());
        canvasPane.setNodes(nodes);
        canvasPane.setEdges(edges);
        canvasPane.setGuides(guideDao.findByDocument(currentDocument.getId()));
        loadShapes(nodes);
        refreshMaterials();
        updateSummary();
    }

    private void refreshMaterials() {
        List<Material> materials = materialDao.findByUser(sessionManager.getCurrentUser().getId());
        materialsList.getItems().setAll(materials);
        defaultMaterial.getItems().setAll(materials);
        materialColors.clear();
        for (Material material : materials) {
            Color color = parseColor(material.getColorHex());
            materialColors.put(material.getId(), color);
        }
        canvasPane.setMaterialColors(materialColors);
        if (!materials.isEmpty() && defaultMaterial.getSelectionModel().isEmpty()) {
            defaultMaterial.getSelectionModel().select(0);
        }
    }

    private void selectDefaultMaterialById(int materialId) {
        for (Material material : defaultMaterial.getItems()) {
            if (material.getId() == materialId) {
                defaultMaterial.getSelectionModel().select(material);
                break;
            }
        }
    }

    private void handleCanvasClick(Point2D cmPoint) {
        if (currentDocument == null) {
            return;
        }
        if (currentTool == CanvasPane.Mode.SELECT) {
            clearSelection();
            return;
        }
        if (currentTool == CanvasPane.Mode.PEN) {
            var node = nodeDao.create(currentDocument.getId(), cmPoint.getX(), cmPoint.getY());
            canvasPane.addNode(node);
            if (selectedNodeId != null) {
                handleEdgeCreate(selectedNodeId, node.getId());
            }
            selectNode(node.getId());
        }
    }

    private void handleEdgeCreate(int startNodeId, int endNodeId) {
        if (currentDocument == null) {
            return;
        }
        if (startNodeId == endNodeId) {
            return;
        }
        List<Edge> existingEdges = edgeDao.findByDocument(currentDocument.getId());
        var edge = edgeDao.create(currentDocument.getId(), startNodeId, endNodeId);
        canvasPane.addEdge(edge);
        GeometryService.CycleResult cycleResult = geometryService.detectCycleForEdge(existingEdges, startNodeId, endNodeId);
        if (cycleResult.cycleDetected()) {
            Map<Integer, NodePoint> nodeMap = buildNodeMap();
            Material material = getActiveMaterial();
            Integer materialId = material == null ? null : material.getId();
            ShapePolygon newShape = geometryService.buildShapeFromCycle(currentDocument.getId(), materialId,
                    cycleResult.nodeIds(), nodeMap);
            if (!shapeExists(newShape.getNodeIds())) {
                ShapePolygon saved = shapeDao.createShape(newShape);
                shapes.add(saved);
                canvasPane.addShape(saved);
            }
        }
        updateSummary();
        System.out.println("edge " + startNodeId + "-" + endNodeId + " added, cycleDetected="
                + cycleResult.cycleDetected() + ", shapeCount=" + shapes.size());
    }

    private void handleNodeClick(int nodeId) {
        if (currentTool == CanvasPane.Mode.ERASE) {
            eraseNode(nodeId);
            return;
        }
        if (currentTool == CanvasPane.Mode.SELECT) {
            selectNode(nodeId);
            return;
        }
        if (currentTool == CanvasPane.Mode.PEN) {
            if (selectedNodeId != null && selectedNodeId != nodeId) {
                handleEdgeCreate(selectedNodeId, nodeId);
            }
            selectNode(nodeId);
        }
    }

    private void handleShapeClick(int shapeId) {
        if (currentTool == CanvasPane.Mode.SELECT) {
            selectShape(shapeId);
        }
    }

    private void updateScale(double newScale) {
        scale = newScale;
        canvasPane.setScale(scale);
        horizontalRuler.setScale(scale);
        verticalRuler.setScale(scale);
    }

    private void setupGuideDragging() {
        Line guidePreview = new Line();
        guidePreview.setStroke(Color.rgb(200, 80, 80, 0.7));
        guidePreview.getStrokeDashArray().setAll(6.0, 4.0);
        guidePreview.setVisible(false);
        canvasPane.getChildren().add(guidePreview);

        horizontalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            guidePreview.setVisible(true);
            guidePreview.setStartY(event.getSceneY());
        });

        horizontalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setStartX(0);
            guidePreview.setEndX(canvasPane.getWidth());
            guidePreview.setStartY(local.getY());
            guidePreview.setEndY(local.getY());
        });

        horizontalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setVisible(false);
            if (local.getY() >= 0 && local.getY() <= canvasPane.getHeight()) {
                double positionCm = local.getY() / scale;
                Guide guide = guideDao.create(currentDocument.getId(), Guide.Orientation.HORIZONTAL, positionCm);
                canvasPane.addGuide(guide);
            }
        });

        verticalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> guidePreview.setVisible(true));

        verticalRuler.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setStartY(0);
            guidePreview.setEndY(canvasPane.getHeight());
            guidePreview.setStartX(local.getX());
            guidePreview.setEndX(local.getX());
        });

        verticalRuler.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setVisible(false);
            if (local.getX() >= 0 && local.getX() <= canvasPane.getWidth()) {
                double positionCm = local.getX() / scale;
                Guide guide = guideDao.create(currentDocument.getId(), Guide.Orientation.VERTICAL, positionCm);
                canvasPane.addGuide(guide);
            }
        });
    }

    private void loadShapes(List<NodePoint> nodes) {
        shapes.clear();
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        List<ShapePolygon> stored = shapeDao.findByDocument(currentDocument.getId());
        for (ShapePolygon storedShape : stored) {
            ShapePolygon hydrated = geometryService.buildShapeFromCycle(
                    storedShape.getDocumentId(),
                    storedShape.getMaterialId(),
                    storedShape.getNodeIds(),
                    nodeMap
            );
            shapes.add(new ShapePolygon(
                    storedShape.getId(),
                    hydrated.getDocumentId(),
                    hydrated.getMaterialId(),
                    hydrated.getQuantity(),
                    hydrated.getNodeIds(),
                    hydrated.getNodes(),
                    storedShape.getAreaCm2(),
                    storedShape.getPerimeterCm()
            ));
        }
        canvasPane.setShapes(shapes);
    }

    private void recomputeShapesFromGeometry(List<NodePoint> nodes, List<Edge> edges) {
        shapes.clear();
        shapeDao.deleteByDocument(currentDocument.getId());
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        Material material = getActiveMaterial();
        Integer materialId = material == null ? null : material.getId();
        List<List<Integer>> cycles = geometryService.detectAllCycles(edges);
        for (List<Integer> cycle : cycles) {
            ShapePolygon newShape = geometryService.buildShapeFromCycle(currentDocument.getId(), materialId, cycle, nodeMap);
            ShapePolygon saved = shapeDao.createShape(newShape);
            shapes.add(saved);
        }
        canvasPane.setShapes(shapes);
        updateSummary();
    }

    private Map<Integer, NodePoint> buildNodeMap() {
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodeDao.findByDocument(currentDocument.getId())) {
            nodeMap.put(node.getId(), node);
        }
        return nodeMap;
    }

    private boolean shapeExists(List<Integer> nodeIds) {
        for (ShapePolygon shape : shapes) {
            if (shape.getNodeIds().equals(nodeIds)) {
                return true;
            }
        }
        return false;
    }

    private void eraseNode(int nodeId) {
        edgeDao.deleteByNode(nodeId);
        nodeDao.delete(nodeId);
        List<NodePoint> nodes = nodeDao.findByDocument(currentDocument.getId());
        List<Edge> edges = edgeDao.findByDocument(currentDocument.getId());
        canvasPane.setNodes(nodes);
        canvasPane.setEdges(edges);
        recomputeShapesFromGeometry(nodes, edges);
        clearSelection();
    }

    private void selectNode(int nodeId) {
        selectedNodeId = nodeId;
        selectedShapeId = null;
        canvasPane.setSelectedNode(nodeId);
    }

    private void selectShape(int shapeId) {
        selectedShapeId = shapeId;
        selectedNodeId = null;
        canvasPane.setSelectedShape(shapeId);
    }

    private void clearSelection() {
        selectedNodeId = null;
        selectedShapeId = null;
        canvasPane.clearSelection();
    }

    private Material getActiveMaterial() {
        Material selected = materialsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            return selected;
        }
        return defaultMaterial.getSelectionModel().getSelectedItem();
    }

    private void setTool(CanvasPane.Mode mode) {
        currentTool = mode;
        canvasPane.setMode(mode);
        if (mode == CanvasPane.Mode.SELECT) {
            return;
        }
        if (mode == CanvasPane.Mode.ERASE) {
            clearSelection();
        }
    }

    private Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
            return Color.web("#8FAADC");
        }
    }

    private void updateSummary() {
        summaryList.getItems().clear();
        int index = 1;
        for (ShapePolygon shape : shapes) {
            summaryList.getItems().add(String.format("Shape %d area: %.2f cm²", index++, shape.getAreaCm2()));
        }
        double total = 0;
        List<EstimationSummary> summaries = estimationService.estimate(currentDocument.getId(), 10.0);
        for (EstimationSummary summary : summaries) {
            summaryList.getItems().add(summary.getMaterialName() + " - " + summary.getDetails()
                    + String.format(" ($%.2f)", summary.getCost()));
            total += summary.getCost();
        }
        totalCostLabel.setText(String.format("Total: $%.2f", total));
    }
}

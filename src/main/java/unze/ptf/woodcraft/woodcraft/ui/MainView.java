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
import javafx.scene.control.ToolBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.MaterialType;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;
import unze.ptf.woodcraft.woodcraft.service.AuthService;
import unze.ptf.woodcraft.woodcraft.service.EstimationService;
import unze.ptf.woodcraft.woodcraft.service.EstimationSummary;
import unze.ptf.woodcraft.woodcraft.service.GeometryService;
import unze.ptf.woodcraft.woodcraft.service.PdfExportService;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;
import unze.ptf.woodcraft.woodcraft.util.UnitConverter;

import java.io.File;
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
    private final ListView<String> cutList = new ListView<>();
    private final ListView<String> sheetList = new ListView<>();
    private final Label totalCostLabel = new Label("Ukupno: $0.00");
    private final Label selectedShapeLabel = new Label("Odabrani oblik: nema");
    private final Label selectedShapeCostLabel = new Label();

    private final List<ShapePolygon> shapes = new ArrayList<>();
    private final List<Guide> guides = new ArrayList<>();
    private final PdfExportService pdfExportService = new PdfExportService();

    private double scale = 10.0;
    private Document currentDocument;
    private Integer selectedNodeId;
    private Integer selectedShapeId;
    private Integer lastDrawNodeId;
    private CanvasPane.Mode currentTool = CanvasPane.Mode.DRAW_SHAPE;
    private UnitSystem unitSystem = UnitSystem.CM;

    public MainView(SessionManager sessionManager, AuthService authService, UserDao userDao, MaterialDao materialDao,
                    DocumentDao documentDao, NodeDao nodeDao, EdgeDao edgeDao, GuideDao guideDao, ShapeDao shapeDao,
                    GeometryService geometryService, EstimationService estimationService, SceneNavigator navigator,
                    int documentId) {
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
        this.currentDocument = documentDao.findById(documentId, sessionManager.getCurrentUser().getId()).orElse(null);

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
        horizontalRuler.setPrefHeight(RULER_SIZE);
        verticalRuler.setPrefWidth(RULER_SIZE);

        canvasRegion.setTop(horizontalRuler);
        canvasRegion.setLeft(verticalRuler);
        canvasRegion.setCenter(canvasPane);

        root.setCenter(canvasRegion);
        root.setRight(buildSidebar());

        canvasPane.setOnCanvasClicked(this::handleCanvasClick);
        canvasPane.setOnNodeClicked(this::handleNodeClick);
        canvasPane.setOnShapeClicked(this::handleShapeClick);
        canvasPane.setOnNodeMoveFinished(this::handleNodeMoveFinished);
        canvasPane.setOnDeleteNodes(this::handleDeleteNodes);
        canvasPane.setOnDeleteGuides(this::handleDeleteGuides);
        canvasPane.addEventFilter(ScrollEvent.SCROLL, this::handleZoomScroll);
        setupGuideDragging();
    }

    private MenuBar buildMenu() {
        Menu file = new Menu("Datoteka");
        MenuItem logout = new MenuItem("Odjava");
        MenuItem exportPdf = new MenuItem("Izvoz PDF");
        logout.setOnAction(event -> {
            authService.logout();
            navigator.showLogin();
        });
        exportPdf.setOnAction(event -> exportPdf());
        file.getItems().addAll(exportPdf, logout);

        Menu edit = new Menu("Uredi");
        MenuItem editCanvas = new MenuItem("Postavke platna");
        editCanvas.setOnAction(event -> openCanvasSettings());
        edit.getItems().add(editCanvas);
        Menu view = new Menu("Prikaz");
        MenuItem unitsToggle = new MenuItem("Promijeni jedinice (cm/in)");
        unitsToggle.setOnAction(event -> toggleUnits());
        view.getItems().add(unitsToggle);
        Menu window = new Menu("Prozor");
        Menu help = new Menu("Pomoc");

        if (sessionManager.getCurrentUser() != null && sessionManager.getCurrentUser().getRole() == Role.ADMIN) {
            Menu admin = new Menu("Admin");
            MenuItem manageUsers = new MenuItem("Upravljanje korisnicima");
            manageUsers.setOnAction(event -> new UserManagementDialog(userDao).showAndWait());
            admin.getItems().add(manageUsers);
            return new MenuBar(file, edit, view, window, help, admin);
        }

        return new MenuBar(file, edit, view, window, help);
    }

    private ToolBar buildToolBar() {
        ToggleGroup tools = new ToggleGroup();

        ToggleButton drawShape = new ToggleButton("Crtaj oblik");
        drawShape.setToggleGroup(tools);
        drawShape.setSelected(true);
        drawShape.setOnAction(event -> setTool(CanvasPane.Mode.DRAW_SHAPE));

        ToggleButton moveNode = new ToggleButton("Pomakni cvor");
        moveNode.setToggleGroup(tools);
        moveNode.setOnAction(event -> setTool(CanvasPane.Mode.MOVE_NODE));

        ToggleButton deleteNode = new ToggleButton("Brisi cvorove");
        deleteNode.setToggleGroup(tools);
        deleteNode.setOnAction(event -> setTool(CanvasPane.Mode.DELETE_NODE));

        ToggleButton deleteGuide = new ToggleButton("Brisi vodilice");
        deleteGuide.setToggleGroup(tools);
        deleteGuide.setOnAction(event -> setTool(CanvasPane.Mode.DELETE_GUIDE));

        return new ToolBar(drawShape, moveNode, deleteNode, deleteGuide);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(320);
        sidebar.setMinWidth(320);
        sidebar.setMaxWidth(320);
        sidebar.setStyle("-fx-background-color: #f5f5f5;");

        Label materialsLabel = new Label("Materijali");
        materialsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        materialsList.setCellFactory(listView -> new ListCell<>() {
            private final ImageView thumbnail = new ImageView();

            {
                thumbnail.setFitWidth(32);
                thumbnail.setFitHeight(32);
                thumbnail.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Material item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(item.getName() + " (" + materialTypeLabel(item.getType()) + ")");
                String imagePath = item.getImagePath();
                if (imagePath != null && !imagePath.isBlank()) {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        thumbnail.setImage(new Image(file.toURI().toString(), 32, 32, true, true));
                        setGraphic(thumbnail);
                        return;
                    }
                }
                setGraphic(null);
            }
        });

        Button addMaterial = new Button("Dodaj materijal");
        addMaterial.setOnAction(event -> {
            MaterialDialog dialog = new MaterialDialog(sessionManager.getCurrentUser().getId());
            dialog.showAndWait().ifPresent(material -> {
                int id = materialDao.create(material);
                refreshMaterials();
                selectDefaultMaterialById(id);
            });
        });
        Button editMaterial = new Button("Uredi materijal");
        editMaterial.setOnAction(event -> {
            Material selected = materialsList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            MaterialDialog dialog = new MaterialDialog(sessionManager.getCurrentUser().getId(), selected);
            dialog.showAndWait().ifPresent(material -> {
                materialDao.update(material);
                refreshMaterials();
                selectDefaultMaterialById(material.getId());
            });
        });

        Label defaultLabel = new Label("Zadani materijal za oblike");
        defaultMaterial.setOnAction(event -> recomputeShapes());
        defaultMaterial.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Material item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + materialTypeLabel(item.getType()) + ")");
            }
        });
        defaultMaterial.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Material item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " (" + materialTypeLabel(item.getType()) + ")");
            }
        });

        Label summaryLabel = new Label("Sazetak");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox summaryBox = new VBox(6, summaryList, totalCostLabel);
        VBox.setVgrow(summaryList, Priority.ALWAYS);

        Label cutListLabel = new Label("Lista rezova");
        cutListLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox cutBox = new VBox(6, cutList);
        VBox.setVgrow(cutList, Priority.ALWAYS);

        Label sheetLabel = new Label("Plan ploca");
        sheetLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox sheetBox = new VBox(6, sheetList);
        VBox.setVgrow(sheetList, Priority.ALWAYS);

        Label selectionLabel = new Label("Odabir");
        selectionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        VBox selectionBox = new VBox(4, selectedShapeLabel, selectedShapeCostLabel);

        sidebar.getChildren().addAll(materialsLabel, materialsList, addMaterial, editMaterial, new Separator(),
                defaultLabel, defaultMaterial, new Separator(), summaryLabel, summaryBox,
                new Separator(), cutListLabel, cutBox, new Separator(), sheetLabel, sheetBox,
                new Separator(), selectionLabel, selectionBox);
        VBox.setVgrow(materialsList, Priority.ALWAYS);
        VBox.setVgrow(summaryBox, Priority.ALWAYS);
        VBox.setVgrow(cutBox, Priority.ALWAYS);
        VBox.setVgrow(sheetBox, Priority.ALWAYS);
        return sidebar;
    }

    private void loadUserData() {
        int userId = sessionManager.getCurrentUser().getId();
        if (currentDocument == null) {
            currentDocument = documentDao.findFirstByUser(userId)
                    .orElseGet(() -> {
                        int id = documentDao.createDocument(userId, "Zadani projekt");
                        return documentDao.findById(id, userId).orElse(null);
                    });
        }
        if (currentDocument == null) {
            return;
        }
        unitSystem = currentDocument.getUnitSystem();
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        guides.clear();
        guides.addAll(guideDao.findByDocument(currentDocument.getId()));
        canvasPane.setGuides(guides);
        canvasPane.setCanvasSizeCm(currentDocument.getWidthCm(), currentDocument.getHeightCm());
        refreshMaterials();
        recomputeShapes();
    }

    private void refreshMaterials() {
        List<Material> materials = materialDao.findByUser(sessionManager.getCurrentUser().getId());
        materialsList.getItems().setAll(materials);
        defaultMaterial.getItems().setAll(materials);
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
        if (currentTool != CanvasPane.Mode.DRAW_SHAPE) {
            return;
        }
        handleNodeCreate(clampToCanvas(applyGuideSnapping(cmPoint)));
    }

    private void handleNodeClick(int nodeId) {
        if (currentDocument == null) {
            return;
        }
        if (currentTool == CanvasPane.Mode.DELETE_NODE) {
            eraseNode(nodeId);
            return;
        }
        if (currentTool == CanvasPane.Mode.DRAW_SHAPE) {
            if (lastDrawNodeId != null && lastDrawNodeId != nodeId) {
                handleEdgeCreate(lastDrawNodeId, nodeId);
            }
            lastDrawNodeId = nodeId;
        }
        selectedNodeId = nodeId;
        selectedShapeId = null;
        canvasPane.setSelectedNode(nodeId);
        updateSelectedShapeSummary();
    }

    private void handleShapeClick(int shapeId) {
        selectedShapeId = shapeId;
        selectedNodeId = null;
        canvasPane.setSelectedShape(shapeId);
        updateSelectedShapeSummary();
    }

    private void handleNodeCreate(Point2D cmPoint) {
        if (currentDocument == null) {
            return;
        }
        var node = nodeDao.create(currentDocument.getId(), cmPoint.getX(), cmPoint.getY());
        canvasPane.addNode(node);
        if (lastDrawNodeId != null) {
            handleEdgeCreate(lastDrawNodeId, node.getId());
        }
        lastDrawNodeId = node.getId();
        recomputeShapes();
    }

    private void handleEdgeCreate(int startNodeId, int endNodeId) {
        if (currentDocument == null) {
            return;
        }
        var edge = edgeDao.create(currentDocument.getId(), startNodeId, endNodeId);
        canvasPane.addEdge(edge);
        recomputeShapes();
    }

    private void updateScale(double newScale) {
        scale = clampScale(newScale);
        canvasPane.setScale(scale);
        horizontalRuler.setScale(scale);
        verticalRuler.setScale(scale);
    }

    private void setTool(CanvasPane.Mode mode) {
        currentTool = mode;
        canvasPane.setMode(mode);
        if (mode != CanvasPane.Mode.DRAW_SHAPE) {
            lastDrawNodeId = null;
        }
        if (mode != CanvasPane.Mode.MOVE_NODE) {
            selectedNodeId = null;
            canvasPane.clearSelection();
        }
        if (mode == CanvasPane.Mode.DELETE_NODE || mode == CanvasPane.Mode.DELETE_GUIDE) {
            selectedShapeId = null;
            updateSelectedShapeSummary();
        }
    }

    private void handleNodeMoveFinished(int nodeId, Point2D cmPoint) {
        Point2D snapped = clampToCanvas(applyGuideSnapping(cmPoint));
        nodeDao.updatePosition(nodeId, snapped.getX(), snapped.getY());
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
        if (currentTool == CanvasPane.Mode.MOVE_NODE) {
            canvasPane.setSelectedNode(nodeId);
        }
    }

    private void eraseNode(int nodeId) {
        edgeDao.deleteByNode(nodeId);
        nodeDao.delete(nodeId);
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
    }

    private void handleDeleteNodes(List<Integer> nodeIds) {
        for (Integer nodeId : nodeIds) {
            edgeDao.deleteByNode(nodeId);
            nodeDao.delete(nodeId);
        }
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
    }

    private void handleDeleteGuides(List<Integer> guideIds) {
        for (Integer guideId : guideIds) {
            guideDao.deleteById(guideId);
        }
        guides.removeIf(guide -> guideIds.contains(guide.getId()));
        canvasPane.setGuides(guides);
    }

    private void setupGuideDragging() {
        Line guidePreview = new Line();
        guidePreview.setStroke(Color.rgb(200, 80, 80, 0.7));
        guidePreview.getStrokeDashArray().setAll(6.0, 4.0);
        guidePreview.setVisible(false);
        canvasPane.getChildren().add(guidePreview);

        horizontalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setVisible(true);
            guidePreview.setStartX(0);
            guidePreview.setEndX(canvasPane.getWidth());
            guidePreview.setStartY(local.getY());
            guidePreview.setEndY(local.getY());
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
                Point2D cmPoint = canvasPane.toCanvasCm(local);
                double positionCm = cmPoint.getY();
                Guide guide = guideDao.create(currentDocument.getId(), Guide.Orientation.HORIZONTAL, positionCm);
                guides.add(guide);
                canvasPane.addGuide(guide);
            }
        });

        verticalRuler.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            Point2D local = canvasPane.sceneToLocal(event.getSceneX(), event.getSceneY());
            guidePreview.setVisible(true);
            guidePreview.setStartY(0);
            guidePreview.setEndY(canvasPane.getHeight());
            guidePreview.setStartX(local.getX());
            guidePreview.setEndX(local.getX());
        });

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
                Point2D cmPoint = canvasPane.toCanvasCm(local);
                double positionCm = cmPoint.getX();
                Guide guide = guideDao.create(currentDocument.getId(), Guide.Orientation.VERTICAL, positionCm);
                guides.add(guide);
                canvasPane.addGuide(guide);
            }
        });
    }

    private void recomputeShapes() {
        if (currentDocument == null) {
            return;
        }
        List<ShapePolygon> computed = geometryService.buildShapes(currentDocument.getId(),
                nodeDao.findByDocument(currentDocument.getId()),
                edgeDao.findByDocument(currentDocument.getId()));
        Material material = defaultMaterial.getSelectionModel().getSelectedItem();
        List<ShapePolygon> assigned = computed.stream()
                .map(shape -> new ShapePolygon(-1, shape.getDocumentId(),
                        material == null ? null : material.getId(), shape.getQuantity(), shape.getNodeIds(),
                        shape.getNodes(), shape.getAreaCm2(), shape.getPerimeterCm()))
                .toList();
        shapeDao.replaceShapes(currentDocument.getId(), assigned);
        loadShapesFromDb();
        selectedShapeId = null;
        canvasPane.clearSelection();
        updateSelectedShapeSummary();
        updateSummary();
        updateCutList();
    }

    private void handleZoomScroll(ScrollEvent event) {
        if (event.getDeltaY() == 0) {
            return;
        }
        double factor = event.getDeltaY() > 0 ? 1.1 : 0.9;
        updateScale(clampScale(scale * factor));
        event.consume();
    }

    private double clampScale(double value) {
        return Math.max(2.0, Math.min(80.0, value));
    }

    private Point2D applyGuideSnapping(Point2D cmPoint) {
        if (guides.isEmpty()) {
            return cmPoint;
        }
        double thresholdPx = 8.0;
        double thresholdCm = thresholdPx / scale;
        double x = cmPoint.getX();
        double y = cmPoint.getY();
        Double snapX = null;
        Double snapY = null;
        for (Guide guide : guides) {
            if (guide.getOrientation() == Guide.Orientation.VERTICAL) {
                double dist = Math.abs(x - guide.getPositionCm());
                if (dist <= thresholdCm) {
                    snapX = guide.getPositionCm();
                }
            } else {
                double dist = Math.abs(y - guide.getPositionCm());
                if (dist <= thresholdCm) {
                    snapY = guide.getPositionCm();
                }
            }
        }
        if (snapX != null) {
            x = snapX;
        }
        if (snapY != null) {
            y = snapY;
        }
        return new Point2D(x, y);
    }

    private void loadShapesFromDb() {
        shapes.clear();
        List<ShapePolygon> stored = shapeDao.findByDocument(currentDocument.getId());
        List<NodePoint> nodes = nodeDao.findByDocument(currentDocument.getId());
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
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

    private void updateSelectedShapeSummary() {
        if (selectedShapeId == null) {
            selectedShapeLabel.setText("Odabrani oblik: nema");
            selectedShapeCostLabel.setText("");
            return;
        }
        ShapePolygon shape = findShapeById(selectedShapeId);
        if (shape == null) {
            selectedShapeLabel.setText("Odabrani oblik: nema");
            selectedShapeCostLabel.setText("");
            return;
        }
        double areaCm2 = shape.getAreaCm2();
        double areaDisplay = UnitConverter.fromCm(Math.sqrt(areaCm2), unitSystem);
        double areaDisplay2 = areaDisplay * areaDisplay;
        String unitLabel = unitSystem == UnitSystem.IN ? "in" : "cm";
        selectedShapeLabel.setText(String.format("Odabrani oblik: %.2f %s2", areaDisplay2, unitLabel));
        if (shape.getMaterialId() == null) {
            selectedShapeCostLabel.setText("Materijal: nema");
            return;
        }
        Material material = materialDao.findById(shape.getMaterialId()).orElse(null);
        if (material == null) {
            selectedShapeCostLabel.setText("Materijal: nedostaje");
            return;
        }
        EstimationSummary summary = estimationService.estimateMaterial(material, List.of(shape), 10.0);
        if (summary == null) {
            selectedShapeCostLabel.setText("Materijal: " + material.getName());
            return;
        }
        selectedShapeCostLabel.setText(summary.getDetails() + String.format(" ($%.2f)", summary.getCost()));
    }

    private ShapePolygon findShapeById(int shapeId) {
        for (ShapePolygon shape : shapes) {
            if (shape.getId() == shapeId) {
                return shape;
            }
        }
        return null;
    }

    private void updateSummary() {
        summaryList.getItems().clear();
        sheetList.getItems().clear();
        double total = 0;
        List<EstimationSummary> summaries = estimationService.estimate(currentDocument.getId(), 10.0);
        for (EstimationSummary summary : summaries) {
            summaryList.getItems().add(summary.getMaterialName() + " - " + summary.getDetails()
                    + String.format(" ($%.2f)", summary.getCost()));
            total += summary.getCost();
            if (summary.getDetails().startsWith("Ploce:")) {
                sheetList.getItems().add(summary.getMaterialName() + " - " + summary.getDetails());
            }
        }
        totalCostLabel.setText(String.format("Ukupno: $%.2f", total));
    }

    private void exportPdf() {
        if (currentDocument == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Izvoz PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF datoteke", "*.pdf"));
        File target = chooser.showSaveDialog(root.getScene().getWindow());
        if (target == null) {
            return;
        }
        try {
            pdfExportService.export(
                    currentDocument,
                    nodeDao.findByDocument(currentDocument.getId()),
                    edgeDao.findByDocument(currentDocument.getId()),
                    target
            );
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void updateCutList() {
        cutList.getItems().clear();
        if (currentDocument == null) {
            return;
        }
        String unitLabel = unitSystem == UnitSystem.IN ? "in" : "cm";
        double kerfCm = currentDocument.getKerfMm() / 10.0;
        for (ShapePolygon shape : shapes) {
            if (shape.getNodes() == null || shape.getNodes().isEmpty()) {
                continue;
            }
            double minX = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            for (NodePoint node : shape.getNodes()) {
                minX = Math.min(minX, node.getXCm());
                maxX = Math.max(maxX, node.getXCm());
                minY = Math.min(minY, node.getYCm());
                maxY = Math.max(maxY, node.getYCm());
            }
            double widthCm = (maxX - minX) + kerfCm;
            double heightCm = (maxY - minY) + kerfCm;
            double width = UnitConverter.fromCm(widthCm, unitSystem);
            double height = UnitConverter.fromCm(heightCm, unitSystem);
            String materialName = "Bez materijala";
            String grain = "";
            if (shape.getMaterialId() != null) {
                Material mat = materialDao.findById(shape.getMaterialId()).orElse(null);
                if (mat != null) {
                    materialName = mat.getName();
                    grain = " | godovi: " + grainLabel(mat.getGrainDirection());
                }
            }
            cutList.getItems().add(String.format("%s: %.2f x %.2f %s (kom %d)%s",
                    materialName, width, height, unitLabel, shape.getQuantity(), grain));
        }
    }

    private void openCanvasSettings() {
        if (currentDocument == null) {
            return;
        }
        ProjectDialog dialog = new ProjectDialog("Postavke platna", currentDocument);
        dialog.showAndWait().ifPresent(settings -> {
            documentDao.updateSettings(currentDocument.getId(), settings.getWidthCm(), settings.getHeightCm(),
                    settings.getKerfMm(), settings.getUnitSystem());
            currentDocument = documentDao.findById(currentDocument.getId(), sessionManager.getCurrentUser().getId()).orElse(currentDocument);
            unitSystem = settings.getUnitSystem();
            canvasPane.setCanvasSizeCm(settings.getWidthCm(), settings.getHeightCm());
            recomputeShapes();
        });
    }

    private void toggleUnits() {
        if (currentDocument == null) {
            return;
        }
        UnitSystem next = unitSystem == UnitSystem.CM ? UnitSystem.IN : UnitSystem.CM;
        documentDao.updateSettings(currentDocument.getId(), currentDocument.getWidthCm(), currentDocument.getHeightCm(),
                currentDocument.getKerfMm(), next);
        unitSystem = next;
        currentDocument = documentDao.findById(currentDocument.getId(), sessionManager.getCurrentUser().getId()).orElse(currentDocument);
        updateSelectedShapeSummary();
        updateCutList();
    }

    private Point2D clampToCanvas(Point2D cmPoint) {
        if (currentDocument == null) {
            return cmPoint;
        }
        double x = Math.min(Math.max(0, cmPoint.getX()), currentDocument.getWidthCm());
        double y = Math.min(Math.max(0, cmPoint.getY()), currentDocument.getHeightCm());
        return new Point2D(x, y);
    }

    private String materialTypeLabel(MaterialType type) {
        return switch (type) {
            case SHEET -> "PLOCA";
            case LUMBER -> "GRADA";
        };
    }

    private String grainLabel(unze.ptf.woodcraft.woodcraft.model.GrainDirection direction) {
        return switch (direction) {
            case NONE -> "bez";
            case HORIZONTAL -> "vodoravno";
            case VERTICAL -> "okomito";
        };
    }
}

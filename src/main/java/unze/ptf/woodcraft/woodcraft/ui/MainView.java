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
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import unze.ptf.woodcraft.woodcraft.dao.DocumentDao;
import unze.ptf.woodcraft.woodcraft.dao.DimensionDao;
import unze.ptf.woodcraft.woodcraft.dao.EdgeDao;
import unze.ptf.woodcraft.woodcraft.dao.GuideDao;
import unze.ptf.woodcraft.woodcraft.dao.ManualShapeDao;
import unze.ptf.woodcraft.woodcraft.dao.MaterialDao;
import unze.ptf.woodcraft.woodcraft.dao.NodeDao;
import unze.ptf.woodcraft.woodcraft.dao.ShapeDao;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Dimension;
import unze.ptf.woodcraft.woodcraft.model.DimensionType;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.Guide;
import unze.ptf.woodcraft.woodcraft.model.ManualShape;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainView {
    private static final double RULER_SIZE = 24;
    private static final int HISTORY_LIMIT = 200;

    private final SessionManager sessionManager;
    private final AuthService authService;
    private final UserDao userDao;
    private final MaterialDao materialDao;
    private final DocumentDao documentDao;
    private final DimensionDao dimensionDao;
    private final NodeDao nodeDao;
    private final EdgeDao edgeDao;
    private final GuideDao guideDao;
    private final ShapeDao shapeDao;
    private final ManualShapeDao manualShapeDao;
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
    private final List<Dimension> dimensions = new ArrayList<>();
    private final List<ManualShape> manualShapes = new ArrayList<>();
    private final PdfExportService pdfExportService = new PdfExportService();
    private final ArrayDeque<ProjectSnapshot> undoStack = new ArrayDeque<>();
    private final ArrayDeque<ProjectSnapshot> redoStack = new ArrayDeque<>();
    private boolean historyRestoring;

    private double scale = 10.0;
    private Document currentDocument;
    private Integer selectedNodeId;
    private Integer selectedShapeId;
    private Integer lastDrawNodeId;
    private CanvasPane.Mode currentTool = CanvasPane.Mode.DRAW_SHAPE;
    private UnitSystem unitSystem = UnitSystem.CM;
    private double currentWastePercent = 10.0;

    private final ToggleGroup plankModeGroup = new ToggleGroup();
    private final RadioButton plankByCount = new RadioButton("Po broju");
    private final RadioButton plankByWidth = new RadioButton("Po sirini");
    private final Slider plankCountSlider = new Slider(1, 50, 6);
    private final Label plankCountLabel = new Label("Ploca: 6");
    private final Label plankWidthLabel = new Label("Sirina: -");
    private final Label plankWasteLabel = new Label("Otpad: -");
    private final Label plankCoverageLabel = new Label("Pokrivenost: -");
    private final Label plankHintLabel = new Label("Odaberite oblik za pregled.");
    private final TextField plankWidthField = new TextField("5");
    private final Slider plankAngleSlider = new Slider(0, 359, 0);
    private final Label plankAngleLabel = new Label("Kut: 0°");

    public MainView(SessionManager sessionManager, AuthService authService, UserDao userDao, MaterialDao materialDao,
                    DocumentDao documentDao, DimensionDao dimensionDao, NodeDao nodeDao, EdgeDao edgeDao,
                    GuideDao guideDao, ShapeDao shapeDao, ManualShapeDao manualShapeDao,
                    GeometryService geometryService, EstimationService estimationService,
                    SceneNavigator navigator, int documentId) {
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.userDao = userDao;
        this.materialDao = materialDao;
        this.documentDao = documentDao;
        this.dimensionDao = dimensionDao;
        this.nodeDao = nodeDao;
        this.edgeDao = edgeDao;
        this.guideDao = guideDao;
        this.shapeDao = shapeDao;
        this.manualShapeDao = manualShapeDao;
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
        root.setStyle(
    "-fx-background-color: linear-gradient(to bottom right, #F3F0E8, #FFFFFF);" 
);

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
        canvasPane.setOnNodesMoved(this::handleNodesMoved);
        canvasPane.setOnEdgeControlsChanged(this::handleEdgeControlsChanged);
        canvasPane.setOnDeleteNodes(this::handleDeleteNodes);
        canvasPane.setOnDeleteGuides(this::handleDeleteGuides);
        canvasPane.setOnDimensionCreate(this::handleDimensionCreate);
        canvasPane.setOnDimensionOffsetChanged(this::handleDimensionOffsetChanged);
        canvasPane.setOnDeleteDimensions(this::handleDeleteDimensions);
        canvasPane.setOnGuidesMoved(this::handleGuidesMoved);
        canvasPane.setOnDimensionsMoved(this::handleDimensionsMoved);
        canvasPane.setOnManualShapesMoved(this::handleManualShapesMoved);
        canvasPane.setOnSliceLine(this::handleSliceLine);
        canvasPane.addEventFilter(ScrollEvent.SCROLL, this::handleZoomScroll);
        setupGuideDragging();
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
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

    ToggleButton selectTool = new ToggleButton("Odabir");
    selectTool.setToggleGroup(tools);
    selectTool.setOnAction(event -> setTool(CanvasPane.Mode.SELECT));

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

    ToggleButton dimensionTool = new ToggleButton("Kote");
    dimensionTool.setToggleGroup(tools);
    dimensionTool.setOnAction(event -> setTool(CanvasPane.Mode.DIMENSION));

    ToggleButton sliceTool = new ToggleButton("Rez");
    sliceTool.setToggleGroup(tools);
    sliceTool.setOnAction(event -> setTool(CanvasPane.Mode.SLICE));

    ToggleButton deleteDimension = new ToggleButton("Brisi kote");
    deleteDimension.setToggleGroup(tools);
    deleteDimension.setOnAction(event -> setTool(CanvasPane.Mode.DELETE_DIMENSION));

    ToggleButton deleteGuide = new ToggleButton("Brisi vodilice");
    deleteGuide.setToggleGroup(tools);
    deleteGuide.setOnAction(event -> setTool(CanvasPane.Mode.DELETE_GUIDE));

    hookToolToggle(selectTool);
    hookToolToggle(drawShape);
    hookToolToggle(moveNode);
    hookToolToggle(deleteNode);
    hookToolToggle(dimensionTool);
    hookToolToggle(sliceTool);
    hookToolToggle(deleteDimension);
    hookToolToggle(deleteGuide);

    ToolBar bar = new ToolBar(
            selectTool, drawShape, moveNode, dimensionTool, sliceTool,
            deleteNode, deleteDimension, deleteGuide
    );

    bar.setStyle(
            "-fx-background-color: rgba(255,255,255,0.72);" +
            "-fx-border-color: rgba(42, 40, 40, 0.99);" +
            "-fx-border-width: 0 0 1 0;" +
            "-fx-padding: 4 8 4 8;"
    );

    return bar;
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
        VBox sheetBox = new VBox(8);
        plankByCount.setToggleGroup(plankModeGroup);
        plankByWidth.setToggleGroup(plankModeGroup);
        plankByCount.setSelected(true);
        plankCountSlider.setMajorTickUnit(10);
        plankCountSlider.setMinorTickCount(9);
        plankCountSlider.setBlockIncrement(1);
        plankCountSlider.setSnapToTicks(true);
        plankCountSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            plankCountLabel.setText("Ploca: " + value);
            updatePlankPreview();
        });
        plankModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean byCount = plankByCount.isSelected();
            plankCountSlider.setDisable(!byCount);
            plankWidthField.setDisable(byCount);
            updatePlankPreview();
        });
        plankWidthField.setPrefWidth(80);
        plankWidthField.setDisable(true);
        plankAngleSlider.setMajorTickUnit(90);
        plankAngleSlider.setMinorTickCount(5);
        plankAngleSlider.setBlockIncrement(1);
        plankAngleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            plankAngleLabel.setText("Kut: " + value + "°");
            updatePlankPreview();
        });
        plankWidthField.textProperty().addListener((obs, oldVal, newVal) -> updatePlankPreview());
        plankHintLabel.setStyle("-fx-text-fill: #666;");
        HBox plankModeRow = new HBox(8, plankByCount, plankByWidth);
        HBox plankWidthRow = new HBox(8, new Label("Sirina"), plankWidthField);
        VBox plankAutoBox = new VBox(6, plankModeRow, plankCountLabel, plankCountSlider, plankWidthRow,
                plankAngleLabel, plankAngleSlider, plankWidthLabel, plankWasteLabel, plankCoverageLabel, plankHintLabel);
        sheetBox.getChildren().addAll(plankAutoBox, sheetList);
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
        loadDocumentState(true);
    }

    private void loadDocumentState(boolean recompute) {
        unitSystem = currentDocument.getUnitSystem();
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        guides.clear();
        guides.addAll(guideDao.findByDocument(currentDocument.getId()));
        canvasPane.setGuides(guides);
        dimensions.clear();
        dimensions.addAll(dimensionDao.findByDocument(currentDocument.getId()));
        canvasPane.setDimensions(dimensions);
        manualShapes.clear();
        manualShapes.addAll(manualShapeDao.findByDocument(currentDocument.getId()));
        canvasPane.setManualShapes(manualShapes);
        canvasPane.setUnitSystem(unitSystem);
        canvasPane.setCanvasSizeCm(currentDocument.getWidthCm(), currentDocument.getHeightCm());
        refreshMaterials();
        if (recompute) {
            recomputeShapes();
        } else {
            loadShapesFromDb();
            updateSelectedShapeSummary();
            updatePlankPreview();
            updateSummary();
            updateCutList();
        }
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
        if (currentTool == CanvasPane.Mode.SELECT) {
            selectedNodeId = nodeId;
            selectedShapeId = null;
            updateSelectedShapeSummary();
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
        if (shapeId < 0) {
            selectedShapeId = null;
            selectedNodeId = null;
            updateSelectedShapeSummary();
            return;
        }
        if (currentTool == CanvasPane.Mode.SELECT) {
            selectedShapeId = shapeId;
            selectedNodeId = null;
            updatePlankPreview();
            updateSelectedShapeSummary();
            return;
        }
        selectedShapeId = shapeId;
        selectedNodeId = null;
        canvasPane.setSelectedShape(shapeId);
        updatePlankPreview();
        updateSelectedShapeSummary();
    }

    private void handleNodeCreate(Point2D cmPoint) {
        if (currentDocument == null) {
            return;
        }
        pushHistory();
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
        pushHistory();
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
        if (mode != CanvasPane.Mode.MOVE_NODE && mode != CanvasPane.Mode.SELECT
                && mode != CanvasPane.Mode.SLICE) {
            selectedNodeId = null;
            if (mode != CanvasPane.Mode.SELECT) {
                canvasPane.clearSelection();
            }
        }
        if (mode == CanvasPane.Mode.DELETE_NODE || mode == CanvasPane.Mode.DELETE_GUIDE) {
            selectedShapeId = null;
            updateSelectedShapeSummary();
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Z) {
            undo();
            event.consume();
            return;
        }
        if (event.isControlDown() && event.getCode() == KeyCode.Y) {
            redo();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            deleteSelection();
            event.consume();
        }
    }

    private void pushHistory() {
        if (historyRestoring || currentDocument == null) {
            return;
        }
        undoStack.push(captureSnapshot());
        redoStack.clear();
        while (undoStack.size() > HISTORY_LIMIT) {
            undoStack.removeLast();
        }
    }

    private ProjectSnapshot captureSnapshot() {
        Document doc = documentDao.findById(currentDocument.getId(), sessionManager.getCurrentUser().getId())
                .orElse(currentDocument);
        return new ProjectSnapshot(
                doc,
                nodeDao.findByDocument(doc.getId()),
                edgeDao.findByDocument(doc.getId()),
                guideDao.findByDocument(doc.getId()),
                dimensionDao.findByDocument(doc.getId()),
                shapeDao.findByDocument(doc.getId()),
                manualShapeDao.findByDocument(doc.getId())
        );
    }

    private void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        ProjectSnapshot current = captureSnapshot();
        ProjectSnapshot snapshot = undoStack.pop();
        redoStack.push(current);
        restoreSnapshot(snapshot);
    }

    private void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        ProjectSnapshot current = captureSnapshot();
        ProjectSnapshot snapshot = redoStack.pop();
        undoStack.push(current);
        restoreSnapshot(snapshot);
    }

    private void restoreSnapshot(ProjectSnapshot snapshot) {
        if (snapshot == null || currentDocument == null) {
            return;
        }
        historyRestoring = true;
        try {
            documentDao.updateSettings(currentDocument.getId(), snapshot.document.getWidthCm(),
                    snapshot.document.getHeightCm(), snapshot.document.getKerfMm(),
                    snapshot.document.getUnitSystem());

            dimensionDao.deleteByDocument(currentDocument.getId());
            edgeDao.deleteByDocument(currentDocument.getId());
            nodeDao.deleteByDocument(currentDocument.getId());
            guideDao.deleteByDocument(currentDocument.getId());
            shapeDao.deleteByDocument(currentDocument.getId());
            manualShapeDao.deleteByDocument(currentDocument.getId());

            for (NodePoint node : snapshot.nodes) {
                nodeDao.insertWithId(node);
            }
            for (Edge edge : snapshot.edges) {
                edgeDao.insertWithId(edge);
            }
            for (Guide guide : snapshot.guides) {
                guideDao.insertWithId(guide);
            }
            for (ShapePolygon shape : snapshot.shapes) {
                shapeDao.insertWithId(shape);
            }
            for (Dimension dimension : snapshot.dimensions) {
                dimensionDao.insertWithId(dimension);
            }
            for (ManualShape shape : snapshot.manualShapes) {
                manualShapeDao.insertWithId(shape);
            }
        } finally {
            historyRestoring = false;
        }
        currentDocument = documentDao.findById(currentDocument.getId(), sessionManager.getCurrentUser().getId())
                .orElse(currentDocument);
        loadDocumentState(false);
    }

    private void deleteSelection() {
        List<Integer> nodeIds = canvasPane.getSelectedNodeIds();
        if (!nodeIds.isEmpty()) {
            handleDeleteNodes(nodeIds);
        }
        List<Integer> guideIds = canvasPane.getSelectedGuideIds();
        if (!guideIds.isEmpty()) {
            handleDeleteGuides(guideIds);
        }
        List<Integer> dimensionIds = canvasPane.getSelectedDimensionIds();
        if (!dimensionIds.isEmpty()) {
            handleDeleteDimensions(dimensionIds);
        }
        List<Integer> manualIds = canvasPane.getSelectedManualShapeIds();
        if (!manualIds.isEmpty()) {
            handleDeleteManualShapes(manualIds);
        }
        canvasPane.clearSelection();
    }

    private void handleNodeMoveFinished(int nodeId, Point2D cmPoint) {
        pushHistory();
        Point2D snapped = clampToCanvas(applyGuideSnapping(cmPoint));
        nodeDao.updatePosition(nodeId, snapped.getX(), snapped.getY());
        updateDimensionsForNode(nodeId, snapped);
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        persistEdgeControls();
        recomputeShapes();
        if (currentTool == CanvasPane.Mode.MOVE_NODE) {
            canvasPane.setSelectedNode(nodeId);
        }
    }

    private void handleNodesMoved(List<NodePoint> moved) {
        if (currentDocument == null || moved == null || moved.isEmpty()) {
            return;
        }
        pushHistory();
        for (NodePoint node : moved) {
            Point2D snapped = clampToCanvas(new Point2D(node.getXCm(), node.getYCm()));
            nodeDao.updatePosition(node.getId(), snapped.getX(), snapped.getY());
            updateDimensionsForNode(node.getId(), snapped);
        }
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        persistEdgeControls();
        recomputeShapes();
    }

    private void handleEdgeControlsChanged(CanvasPane.EdgeControlUpdate update) {
        if (update == null) {
            return;
        }
        pushHistory();
        edgeDao.updateControls(update.edgeId(),
                update.start().getX(), update.start().getY(),
                update.end().getX(), update.end().getY());
    }

    private void persistEdgeControls() {
        List<CanvasPane.EdgeControlUpdate> updates = canvasPane.getEdgeControlUpdates();
        for (CanvasPane.EdgeControlUpdate update : updates) {
            edgeDao.updateControls(update.edgeId(),
                    update.start().getX(), update.start().getY(),
                    update.end().getX(), update.end().getY());
        }
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
            dimensionDao.updateEndpoints(dimension.getId(), startX, startY, endX, endY);
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
            changed = true;
        }
        if (changed) {
            canvasPane.setDimensions(dimensions);
        }
    }

    private void eraseNode(int nodeId) {
        pushHistory();
        edgeDao.deleteByNode(nodeId);
        nodeDao.delete(nodeId);
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
    }

    private void handleDeleteNodes(List<Integer> nodeIds) {
        pushHistory();
        for (Integer nodeId : nodeIds) {
            edgeDao.deleteByNode(nodeId);
            nodeDao.delete(nodeId);
        }
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
    }

    private void handleDeleteGuides(List<Integer> guideIds) {
        pushHistory();
        for (Integer guideId : guideIds) {
            guideDao.deleteById(guideId);
        }
        guides.removeIf(guide -> guideIds.contains(guide.getId()));
        canvasPane.setGuides(guides);
    }

    private void handleDeleteDimensions(List<Integer> dimensionIds) {
        pushHistory();
        for (Integer dimensionId : dimensionIds) {
            dimensionDao.deleteById(dimensionId);
        }
        dimensions.removeIf(dimension -> dimensionIds.contains(dimension.getId()));
        canvasPane.setDimensions(dimensions);
    }

    private void handleDeleteManualShapes(List<Integer> manualIds) {
        pushHistory();
        for (Integer manualId : manualIds) {
            manualShapeDao.deleteById(manualId);
        }
        manualShapes.removeIf(shape -> manualIds.contains(shape.getId()));
        canvasPane.setManualShapes(manualShapes);
    }

    private void handleDimensionCreate(CanvasPane.DimensionDraft draft) {
        if (currentDocument == null || draft == null) {
            return;
        }
        pushHistory();
        Dimension dimension = dimensionDao.create(
                currentDocument.getId(),
                draft.start().getX(),
                draft.start().getY(),
                draft.end().getX(),
                draft.end().getY(),
                draft.offset().getX(),
                draft.offset().getY(),
                draft.type() == null ? DimensionType.ALIGNED : draft.type(),
                draft.startNodeId(),
                draft.endNodeId()
        );
        dimensions.add(dimension);
        canvasPane.addDimension(dimension);
    }

    private void handleDimensionOffsetChanged(Integer dimensionId, Point2D offsetCm) {
        if (dimensionId == null || offsetCm == null) {
            return;
        }
        pushHistory();
        dimensionDao.updateOffset(dimensionId, offsetCm.getX(), offsetCm.getY());
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
    }

    private void handleGuidesMoved(List<Guide> moved) {
        if (moved == null || moved.isEmpty()) {
            return;
        }
        pushHistory();
        for (Guide guide : moved) {
            guideDao.updatePosition(guide.getId(), guide.getPositionCm());
            replaceGuideInList(guide);
        }
    }

    private void handleDimensionsMoved(List<Dimension> moved) {
        if (moved == null || moved.isEmpty()) {
            return;
        }
        pushHistory();
        for (Dimension dimension : moved) {
            dimensionDao.updateEndpoints(dimension.getId(),
                    dimension.getStartXCm(), dimension.getStartYCm(),
                    dimension.getEndXCm(), dimension.getEndYCm());
            dimensionDao.updateOffset(dimension.getId(), dimension.getOffsetXCm(), dimension.getOffsetYCm());
            replaceDimensionInList(dimension);
        }
    }

    private void handleManualShapesMoved(List<ManualShape> moved) {
        if (moved == null || moved.isEmpty()) {
            return;
        }
        pushHistory();
        for (ManualShape shape : moved) {
            manualShapeDao.updatePoints(shape.getId(), shape.getPoints());
            replaceManualShapeInList(shape);
        }
    }

    private void handleSliceLine(Point2D start, Point2D end) {
        if (currentDocument == null || start == null || end == null) {
            return;
        }
        pushHistory();
        List<Integer> targetShapes = canvasPane.getSelectedShapeIds();
        if (targetShapes.isEmpty()) {
            return;
        }
        List<NodePoint> allNodes = nodeDao.findByDocument(currentDocument.getId());
        List<Edge> allEdges = edgeDao.findByDocument(currentDocument.getId());
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : allNodes) {
            nodeMap.put(node.getId(), node);
        }
        for (Integer shapeId : targetShapes) {
            ShapePolygon shape = findShapeById(shapeId);
            if (shape == null || shape.getNodes() == null || shape.getNodes().size() < 3) {
                continue;
            }
            List<NodePoint> shapeNodes = shape.getNodes();
            List<IntersectionHit> intersections = new ArrayList<>();
            for (int i = 0; i < shapeNodes.size(); i++) {
                NodePoint a = shapeNodes.get(i);
                NodePoint b = shapeNodes.get((i + 1) % shapeNodes.size());
                Point2D hit = intersectSegments(
                        new Point2D(a.getXCm(), a.getYCm()),
                        new Point2D(b.getXCm(), b.getYCm()),
                        start,
                        end
                );
                if (hit == null) {
                    continue;
                }
                int existingNodeId = nearExistingNode(hit, a, b);
                int nodeId = existingNodeId;
                if (nodeId == -1) {
                    NodePoint created = nodeDao.create(currentDocument.getId(), hit.getX(), hit.getY());
                    nodeId = created.getId();
                    nodeMap.put(nodeId, created);
                }
                int edgeId = findEdgeId(allEdges, a.getId(), b.getId());
                if (edgeId != -1) {
                    edgeDao.deleteById(edgeId);
                    edgeDao.create(currentDocument.getId(), a.getId(), nodeId);
                    edgeDao.create(currentDocument.getId(), nodeId, b.getId());
                }
                intersections.add(new IntersectionHit(nodeId, projectionT(start, end, hit)));
            }
            if (intersections.size() >= 2) {
                intersections.sort((left, right) -> Double.compare(left.t(), right.t()));
                int startNodeId = intersections.get(0).nodeId();
                int endNodeId = intersections.get(intersections.size() - 1).nodeId();
                if (startNodeId != endNodeId && findEdgeId(allEdges, startNodeId, endNodeId) == -1) {
                    edgeDao.create(currentDocument.getId(), startNodeId, endNodeId);
                }
            }
        }
        canvasPane.setNodes(nodeDao.findByDocument(currentDocument.getId()));
        canvasPane.setEdges(edgeDao.findByDocument(currentDocument.getId()));
        recomputeShapes();
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
                pushHistory();
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
                pushHistory();
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
        updatePlankPreview();
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
        List<Edge> edges = edgeDao.findByDocument(currentDocument.getId());
        Map<Integer, NodePoint> nodeMap = new HashMap<>();
        for (NodePoint node : nodes) {
            nodeMap.put(node.getId(), node);
        }
        Map<String, Edge> edgeMap = new HashMap<>();
        for (Edge edge : edges) {
            String key = edge.getStartNodeId() < edge.getEndNodeId()
                    ? edge.getStartNodeId() + "-" + edge.getEndNodeId()
                    : edge.getEndNodeId() + "-" + edge.getStartNodeId();
            edgeMap.put(key, edge);
        }
        for (ShapePolygon storedShape : stored) {
            ShapePolygon hydrated = geometryService.buildShapeFromCycle(
                    storedShape.getDocumentId(),
                    storedShape.getMaterialId(),
                    storedShape.getNodeIds(),
                    nodeMap,
                    edgeMap
            );
            shapes.add(new ShapePolygon(
                    storedShape.getId(),
                    hydrated.getDocumentId(),
                    hydrated.getMaterialId(),
                    hydrated.getQuantity(),
                    hydrated.getNodeIds(),
                    hydrated.getNodes(),
                    hydrated.getAreaCm2(),
                    hydrated.getPerimeterCm()
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
        EstimationSummary summary = estimationService.estimateMaterial(material, List.of(shape), currentWastePercent);
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
        List<EstimationSummary> summaries = estimationService.estimate(currentDocument.getId(), currentWastePercent);
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
    chooser.setTitle("Izvoz PDF (snapshot)");
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF datoteke", "*.pdf"));
    File target = chooser.showSaveDialog(root.getScene().getWindow());
    if (target == null) {
        return;
    }

    try {
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        sp.setFill(javafx.scene.paint.Color.WHITE); // bijela pozadina u PDF-u
        boolean stariPrikazCvorova = canvasPane.isNodeLayerVisible();
        canvasPane.setNodeLayerVisible(false);

        javafx.scene.image.WritableImage fxImage = canvasPane.snapshot(sp, null);

        canvasPane.setNodeLayerVisible(stariPrikazCvorova);

        java.awt.image.BufferedImage buffered = fxImageToBufferedImage(fxImage);

        org.apache.pdfbox.pdmodel.common.PDRectangle portrait = org.apache.pdfbox.pdmodel.common.PDRectangle.A4;
        org.apache.pdfbox.pdmodel.common.PDRectangle landscape =
                new org.apache.pdfbox.pdmodel.common.PDRectangle(portrait.getHeight(), portrait.getWidth());

        double imgRatio = (double) buffered.getWidth() / (double) buffered.getHeight();
        double a4PortraitRatio = portrait.getWidth() / portrait.getHeight();

        org.apache.pdfbox.pdmodel.common.PDRectangle pageSize = (imgRatio > a4PortraitRatio) ? landscape : portrait;

        try (org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(pageSize);
            pdf.addPage(page);

            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage =
                    org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(pdf, buffered);

            float margin = 36f; 
            float titleBlockHeight = 70f; 

            float availableW = pageSize.getWidth() - 2 * margin;
            float availableH = pageSize.getHeight() - 2 * margin - titleBlockHeight;

            float drawW = availableW;
            float drawH = (float) (availableW / imgRatio);

            if (drawH > availableH) {
                drawH = availableH;
                drawW = (float) (availableH * imgRatio);
            }

            float x = (pageSize.getWidth() - drawW) / 2f;
            float y = margin + titleBlockHeight + (availableH - drawH) / 2f;

            try (org.apache.pdfbox.pdmodel.PDPageContentStream content =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {

                content.setNonStrokingColor(new java.awt.Color(255, 255, 255));
                content.addRect(margin, margin, pageSize.getWidth() - 2 * margin, pageSize.getHeight() - 2 * margin);
                content.fill();

                content.drawImage(pdImage, x, y, drawW, drawH);

                float tbX = margin;
                float tbY = margin;
                float tbW = pageSize.getWidth() - 2 * margin;
                float tbH = titleBlockHeight;

                content.setStrokingColor(new java.awt.Color(40, 40, 40));
                content.setLineWidth(1f);
                content.addRect(tbX, tbY, tbW, tbH);
                content.stroke();

                content.beginText();
                content.setNonStrokingColor(new java.awt.Color(40, 40, 40));
                content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                content.newLineAtOffset(tbX + 10, tbY + tbH - 18);
                content.showText("WoodCraft - Export");
                content.endText();

                String docInfo = String.format("Platno: %.1f cm x %.1f cm | Kerf: %.1f mm | Unit: %s",
                        currentDocument.getWidthCm(),
                        currentDocument.getHeightCm(),
                        currentDocument.getKerfMm(),
                        currentDocument.getUnitSystem() == null ? "-" : currentDocument.getUnitSystem().name()
                );

                content.beginText();
                content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(tbX + 10, tbY + tbH - 38);
                content.showText(docInfo);
                content.endText();

                String extra = "Izvezeno: " +
                 java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                " · WoodCraft";                content.beginText();
                content.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(tbX + 10, tbY + 14);
                content.showText(extra);
                content.endText();
            }

            pdf.save(target);
        }

    } catch (Exception exception) {
        exception.printStackTrace();
    }
}
    private static java.awt.image.BufferedImage fxImageToBufferedImage(javafx.scene.image.WritableImage fxImage) {
    int w = (int) fxImage.getWidth();
    int h = (int) fxImage.getHeight();

    java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
            w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB
    );

    javafx.scene.image.PixelReader reader = fxImage.getPixelReader();
    int[] pixels = new int[w * h];
    reader.getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, w);

    buffered.setRGB(0, 0, w, h, pixels, 0, w);
    return buffered;
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
        canvasPane.setUnitSystem(unitSystem);
        updateSelectedShapeSummary();
        updateCutList();
    }

    private void updatePlankPreview() {
        if (currentDocument == null) {
            return;
        }
        ShapePolygon shape = findShapeById(selectedShapeId == null ? -1 : selectedShapeId);
        if (shape == null) {
            canvasPane.setPlankRects(List.of());
            plankWidthLabel.setText("Sirina: -");
            plankWasteLabel.setText("Otpad: -");
            plankCoverageLabel.setText("Pokrivenost: -");
            plankHintLabel.setText("Odaberite oblik za pregled.");
            currentWastePercent = 10.0;
            return;
        }
        Map<String, Edge> edgeMap = buildEdgeMap(edgeDao.findByDocument(currentDocument.getId()));
        List<Point2D> polygon = extractPolygon(shape, edgeMap);
        if (polygon.size() < 3) {
            canvasPane.setPlankRects(List.of());
            currentWastePercent = 10.0;
            return;
        }
        double angleDeg = plankAngleSlider.getValue();
        int count;
        double widthOverride = parseDouble(plankWidthField.getText(), 5);
        if (unitSystem == UnitSystem.IN) {
            widthOverride = UnitConverter.toCm(widthOverride, unitSystem);
        }
        if (plankByWidth.isSelected()) {
            double span = getSpanForAngle(polygon, angleDeg);
            count = (int) Math.ceil(span / Math.max(0.01, widthOverride));
            if (count > 50) {
                count = 50;
                widthOverride = span / count;
                plankHintLabel.setText("Sirina prilagodena zbog ogranicenja 50 ploca.");
            }
            count = Math.max(1, Math.min(50, count));
            plankCountSlider.setValue(count);
            plankCountLabel.setText("Ploca: " + count);
        } else {
            count = (int) Math.round(plankCountSlider.getValue());
        }
        PlankResult result = buildPlankApproximation(polygon, shape.getAreaCm2(), count, angleDeg, widthOverride);
        canvasPane.setPlankRects(result.planks);
        double widthDisplay = UnitConverter.fromCm(result.plankWidthCm, unitSystem);
        String unitLabel = unitSystem == UnitSystem.IN ? "in" : "cm";
        plankWidthLabel.setText(String.format("Sirina: %.2f %s", widthDisplay, unitLabel));
        plankWasteLabel.setText(String.format("Otpad: %.1f%%", result.wastePercent));
        plankCoverageLabel.setText(String.format("Pokrivenost: %.1f%%", result.coveragePercent));
        if (!plankHintLabel.getText().startsWith("Sirina prilagodena")) {
            plankHintLabel.setText("Priblizni prikaz s varijabilnim duljinama.");
        }
        currentWastePercent = result.wastePercent;
    }

    private List<Point2D> extractPolygon(ShapePolygon shape, Map<String, Edge> edgeMap) {
        List<NodePoint> nodes = shape.getNodes();
        if (nodes == null || nodes.size() < 2) {
            return List.of();
        }
        List<Point2D> points = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            NodePoint start = nodes.get(i);
            NodePoint end = nodes.get((i + 1) % nodes.size());
            Edge edge = edgeMap.get(edgeKey(start.getId(), end.getId()));
            if (edge == null
                    || edge.getControlStartXCm() == null
                    || edge.getControlStartYCm() == null
                    || edge.getControlEndXCm() == null
                    || edge.getControlEndYCm() == null) {
                if (points.isEmpty()) {
                    points.add(new Point2D(start.getXCm(), start.getYCm()));
                }
                points.add(new Point2D(end.getXCm(), end.getYCm()));
                continue;
            }
            Point2D control1;
            Point2D control2;
            if (edge.getStartNodeId() == start.getId()) {
                control1 = new Point2D(edge.getControlStartXCm(), edge.getControlStartYCm());
                control2 = new Point2D(edge.getControlEndXCm(), edge.getControlEndYCm());
            } else {
                control1 = new Point2D(edge.getControlEndXCm(), edge.getControlEndYCm());
                control2 = new Point2D(edge.getControlStartXCm(), edge.getControlStartYCm());
            }
            sampleCubic(points,
                    new Point2D(start.getXCm(), start.getYCm()),
                    control1,
                    control2,
                    new Point2D(end.getXCm(), end.getYCm()),
                    18);
        }
        return points;
    }

    private Map<String, Edge> buildEdgeMap(List<Edge> edges) {
        Map<String, Edge> map = new HashMap<>();
        if (edges == null) {
            return map;
        }
        for (Edge edge : edges) {
            map.put(edgeKey(edge.getStartNodeId(), edge.getEndNodeId()), edge);
            map.put(edgeKey(edge.getEndNodeId(), edge.getStartNodeId()), edge);
        }
        return map;
    }

    private String edgeKey(int a, int b) {
        return a + "-" + b;
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

    private void replaceGuideInList(Guide guide) {
        for (int i = 0; i < guides.size(); i++) {
            if (guides.get(i).getId() == guide.getId()) {
                guides.set(i, guide);
                return;
            }
        }
    }

    private void replaceDimensionInList(Dimension dimension) {
        for (int i = 0; i < dimensions.size(); i++) {
            if (dimensions.get(i).getId() == dimension.getId()) {
                dimensions.set(i, dimension);
                return;
            }
        }
    }

    private void replaceManualShapeInList(ManualShape shape) {
        for (int i = 0; i < manualShapes.size(); i++) {
            if (manualShapes.get(i).getId() == shape.getId()) {
                manualShapes.set(i, shape);
                return;
            }
        }
    }

    private List<Point2D> toPointList(ShapePolygon shape) {
        List<Point2D> points = new ArrayList<>();
        if (shape == null) {
            return points;
        }
        List<NodePoint> nodes = shape.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return points;
        }
        for (NodePoint node : nodes) {
            points.add(new Point2D(node.getXCm(), node.getYCm()));
        }
        return points;
    }

    private int findEdgeId(List<Edge> edges, int nodeA, int nodeB) {
        for (Edge edge : edges) {
            if ((edge.getStartNodeId() == nodeA && edge.getEndNodeId() == nodeB)
                    || (edge.getStartNodeId() == nodeB && edge.getEndNodeId() == nodeA)) {
                return edge.getId();
            }
        }
        return -1;
    }

    private Point2D intersectSegments(Point2D p1, Point2D p2, Point2D q1, Point2D q2) {
        Point2D r = p2.subtract(p1);
        Point2D s = q2.subtract(q1);
        double rxs = (r.getX() * s.getY()) - (r.getY() * s.getX());
        double qpxr = ((q1.getX() - p1.getX()) * r.getY()) - ((q1.getY() - p1.getY()) * r.getX());
        if (Math.abs(rxs) < 1e-8) {
            return null;
        }
        double t = ((q1.getX() - p1.getX()) * s.getY() - (q1.getY() - p1.getY()) * s.getX()) / rxs;
        double u = qpxr / rxs;
        if (t < 0 || t > 1 || u < 0 || u > 1) {
            return null;
        }
        return new Point2D(p1.getX() + t * r.getX(), p1.getY() + t * r.getY());
    }

    private int nearExistingNode(Point2D hit, NodePoint a, NodePoint b) {
        double epsilon = 0.2;
        if (hit.distance(a.getXCm(), a.getYCm()) <= epsilon) {
            return a.getId();
        }
        if (hit.distance(b.getXCm(), b.getYCm()) <= epsilon) {
            return b.getId();
        }
        return -1;
    }

    private double projectionT(Point2D a, Point2D b, Point2D p) {
        Point2D ab = b.subtract(a);
        double denom = ab.dotProduct(ab);
        if (denom < 1e-8) {
            return 0;
        }
        return p.subtract(a).dotProduct(ab) / denom;
    }

    private record IntersectionHit(int nodeId, double t) {
    }

    private List<List<Point2D>> splitPolygonByLine(List<Point2D> polygon, Point2D lineStart, Point2D lineEnd) {
        if (polygon == null || polygon.size() < 3) {
            return List.of();
        }
        List<Point2D> left = clipPolygonByLine(polygon, lineStart, lineEnd, true);
        List<Point2D> right = clipPolygonByLine(polygon, lineStart, lineEnd, false);
        if (left.size() < 3 || right.size() < 3) {
            return List.of();
        }
        return List.of(left, right);
    }

    private List<Point2D> clipPolygonByLine(List<Point2D> polygon, Point2D lineStart, Point2D lineEnd,
                                            boolean keepLeft) {
        List<Point2D> output = new ArrayList<>();
        if (polygon.isEmpty()) {
            return output;
        }
        Point2D prev = polygon.get(polygon.size() - 1);
        double prevSide = lineSide(lineStart, lineEnd, prev);
        boolean prevInside = keepLeft ? prevSide >= -1e-6 : prevSide <= 1e-6;
        for (Point2D curr : polygon) {
            double currSide = lineSide(lineStart, lineEnd, curr);
            boolean currInside = keepLeft ? currSide >= -1e-6 : currSide <= 1e-6;
            if (currInside) {
                if (!prevInside) {
                    Point2D intersection = intersectSegmentLine(prev, curr, lineStart, lineEnd);
                    if (intersection != null) {
                        output.add(intersection);
                    }
                }
                output.add(curr);
            } else if (prevInside) {
                Point2D intersection = intersectSegmentLine(prev, curr, lineStart, lineEnd);
                if (intersection != null) {
                    output.add(intersection);
                }
            }
            prev = curr;
            prevInside = currInside;
        }
        return output;
    }

    private double lineSide(Point2D a, Point2D b, Point2D p) {
        double abx = b.getX() - a.getX();
        double aby = b.getY() - a.getY();
        double apx = p.getX() - a.getX();
        double apy = p.getY() - a.getY();
        return (abx * apy) - (aby * apx);
    }

    private Point2D intersectSegmentLine(Point2D p1, Point2D p2, Point2D a, Point2D b) {
        Point2D r = b.subtract(a);
        Point2D s = p2.subtract(p1);
        double rxs = (r.getX() * s.getY()) - (r.getY() * s.getX());
        if (Math.abs(rxs) < 1e-6) {
            return null;
        }
        Point2D qp = p1.subtract(a);
        double t = ((qp.getX() * s.getY()) - (qp.getY() * s.getX())) / rxs;
        double u = ((qp.getX() * r.getY()) - (qp.getY() * r.getX())) / rxs;
        if (u < -1e-6 || u > 1 + 1e-6) {
            return null;
        }
        return new Point2D(a.getX() + t * r.getX(), a.getY() + t * r.getY());
    }

    private PlankResult buildPlankApproximation(List<Point2D> polygon, double shapeAreaCm2, int count,
                                                double angleDeg, double widthOverride) {
        Point2D centroid = polygonCentroid(polygon);
        List<Point2D> rotated = rotatePolygon(polygon, centroid, -Math.toRadians(angleDeg));
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point2D point : rotated) {
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }
        double span = maxX - minX;
        double width = plankByWidth.isSelected() ? widthOverride : span / count;
        if (width <= 0.01) {
            width = span / Math.max(1, count);
        }
        List<CanvasPane.PlankRect> planks = new ArrayList<>();
        double totalRectArea = 0;
        double totalIntersectArea = 0;

        for (int i = 0; i < count; i++) {
            double x0 = minX + i * width;
            double x1 = (i == count - 1) ? maxX : x0 + width;
            List<Point2D> clipped = clipPolygonToRect(rotated, x0, x1, minY, maxY);
            if (clipped.size() < 3) {
                continue;
            }
            double area = Math.abs(polygonArea(clipped));
            if (area <= 0.0001) {
                continue;
            }
            double clipMinY = Double.POSITIVE_INFINITY;
            double clipMaxY = Double.NEGATIVE_INFINITY;
            for (Point2D point : clipped) {
                clipMinY = Math.min(clipMinY, point.getY());
                clipMaxY = Math.max(clipMaxY, point.getY());
            }
            double plankLength = clipMaxY - clipMinY;
            if (plankLength <= 0 || width <= 0) {
                continue;
            }
            List<Point2D> rectPoints = List.of(
                    new Point2D(x0, clipMinY),
                    new Point2D(x1, clipMinY),
                    new Point2D(x1, clipMaxY),
                    new Point2D(x0, clipMaxY)
            );
            List<Point2D> worldPoints = rotatePolygon(rectPoints, centroid, Math.toRadians(angleDeg));
            planks.add(new CanvasPane.PlankRect(worldPoints));
            totalRectArea += width * plankLength;
            totalIntersectArea += area;
        }
        double wastePercent = shapeAreaCm2 > 0 ? (Math.max(0, totalRectArea - totalIntersectArea) / shapeAreaCm2) * 100.0 : 0;
        double coveragePercent = shapeAreaCm2 > 0 ? (totalIntersectArea / shapeAreaCm2) * 100.0 : 0;
        return new PlankResult(planks, width, wastePercent, coveragePercent);
    }

    private double getSpanForAngle(List<Point2D> polygon, double angleDeg) {
        Point2D centroid = polygonCentroid(polygon);
        List<Point2D> rotated = rotatePolygon(polygon, centroid, -Math.toRadians(angleDeg));
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (Point2D point : rotated) {
            minX = Math.min(minX, point.getX());
            maxX = Math.max(maxX, point.getX());
        }
        return maxX - minX;
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private List<Point2D> clipPolygonToRect(List<Point2D> polygon, double minX, double maxX, double minY, double maxY) {
        List<Point2D> output = polygon;
        output = clipAgainstLine(output, point -> point.getX() >= minX, (a, b) -> intersectX(a, b, minX));
        output = clipAgainstLine(output, point -> point.getX() <= maxX, (a, b) -> intersectX(a, b, maxX));
        output = clipAgainstLine(output, point -> point.getY() >= minY, (a, b) -> intersectY(a, b, minY));
        output = clipAgainstLine(output, point -> point.getY() <= maxY, (a, b) -> intersectY(a, b, maxY));
        return output;
    }

    private List<Point2D> clipAgainstLine(List<Point2D> input, java.util.function.Predicate<Point2D> inside,
                                          java.util.function.BiFunction<Point2D, Point2D, Point2D> intersection) {
        List<Point2D> output = new ArrayList<>();
        if (input.isEmpty()) {
            return output;
        }
        Point2D prev = input.get(input.size() - 1);
        boolean prevInside = inside.test(prev);
        for (Point2D curr : input) {
            boolean currInside = inside.test(curr);
            if (currInside) {
                if (!prevInside) {
                    output.add(intersection.apply(prev, curr));
                }
                output.add(curr);
            } else if (prevInside) {
                output.add(intersection.apply(prev, curr));
            }
            prev = curr;
            prevInside = currInside;
        }
        return output;
    }

    private Point2D intersectX(Point2D a, Point2D b, double x) {
        if (Math.abs(b.getX() - a.getX()) < 1e-6) {
            return new Point2D(x, a.getY());
        }
        double t = (x - a.getX()) / (b.getX() - a.getX());
        double y = a.getY() + t * (b.getY() - a.getY());
        return new Point2D(x, y);
    }

    private Point2D intersectY(Point2D a, Point2D b, double y) {
        if (Math.abs(b.getY() - a.getY()) < 1e-6) {
            return new Point2D(a.getX(), y);
        }
        double t = (y - a.getY()) / (b.getY() - a.getY());
        double x = a.getX() + t * (b.getX() - a.getX());
        return new Point2D(x, y);
    }

    private double polygonArea(List<Point2D> points) {
        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            Point2D a = points.get(i);
            Point2D b = points.get((i + 1) % points.size());
            sum += (a.getX() * b.getY()) - (b.getX() * a.getY());
        }
        return 0.5 * sum;
    }

    private List<Point2D> rotatePolygon(List<Point2D> points, Point2D center, double radians) {
        List<Point2D> rotated = new ArrayList<>();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (Point2D point : points) {
            double dx = point.getX() - center.getX();
            double dy = point.getY() - center.getY();
            double x = center.getX() + (dx * cos - dy * sin);
            double y = center.getY() + (dx * sin + dy * cos);
            rotated.add(new Point2D(x, y));
        }
        return rotated;
    }

    private Point2D polygonCentroid(List<Point2D> points) {
        double area = polygonArea(points);
        if (Math.abs(area) < 1e-6) {
            double sumX = 0;
            double sumY = 0;
            for (Point2D point : points) {
                sumX += point.getX();
                sumY += point.getY();
            }
            return new Point2D(sumX / points.size(), sumY / points.size());
        }
        double cx = 0;
        double cy = 0;
        for (int i = 0; i < points.size(); i++) {
            Point2D a = points.get(i);
            Point2D b = points.get((i + 1) % points.size());
            double cross = (a.getX() * b.getY()) - (b.getX() * a.getY());
            cx += (a.getX() + b.getX()) * cross;
            cy += (a.getY() + b.getY()) * cross;
        }
        double factor = 1.0 / (6.0 * area);
        return new Point2D(cx * factor, cy * factor);
    }

    private record ProjectSnapshot(Document document, List<NodePoint> nodes, List<Edge> edges, List<Guide> guides,
                                   List<Dimension> dimensions, List<ShapePolygon> shapes,
                                   List<ManualShape> manualShapes) {
    }

    private record PlankResult(List<CanvasPane.PlankRect> planks, double plankWidthCm,
                               double wastePercent, double coveragePercent) {
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
    private void hookToolToggle(ToggleButton b) {
    styleToolToggle(b, false);

    b.setOnMouseEntered(e -> styleToolToggle(b, true));
    b.setOnMouseExited(e -> styleToolToggle(b, false));

    b.selectedProperty().addListener((obs, o, n) -> styleToolToggle(b, b.isHover()));
    
    b.setPrefHeight(32);
    b.setMinHeight(32);
    
}  
   

        private void styleToolToggle(ToggleButton b, boolean hover) {
    boolean selected = b.isSelected();

    String bg;
    String border;
    String text;

    if (selected) {
        bg = "#6E8F6A";
        border = "#6E8F6A";
        text = "white";
    } else {
        bg = hover ? "rgba(110,143,106,0.18)" : "rgba(255,255,255,0.80)";
        border = hover ? "#6E8F6A" : "rgba(0,0,0,0.25)";
        text = "#2C2C2C";
    }

    b.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + text + ";" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 700;" +
            "-fx-background-radius: 4;" +   
            "-fx-border-radius: 4;" +       
            "-fx-border-color: " + border + ";" +
            "-fx-border-width: 1;" +
            "-fx-padding: 4 10 4 10;" +      
            "-fx-cursor: hand;"
    );
}

     }
package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import unze.ptf.woodcraft.woodcraft.dao.*;
import unze.ptf.woodcraft.woodcraft.io.ProjectPackageService;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;
import unze.ptf.woodcraft.woodcraft.util.UnitConverter;

import java.io.File;
import java.util.List;

public class ProjectListView {
    private final BorderPane root = new BorderPane();

    public ProjectListView(SessionManager sessionManager, DocumentDao documentDao, SceneNavigator navigator) {

        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F3F0E8, #FFFFFF);");

        VBox card = new VBox(16);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(40, 42, 36, 42));
        card.setMaxWidth(900);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.94);" +
                        "-fx-background-radius: 24;" +
                        "-fx-border-radius: 24;" +
                        "-fx-border-color: rgba(60,60,60,0.10);" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 35, 0.35, 0, 14);"
        );

        Label title = new Label("Vaši projekti");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #2C2C2C;");

        Label subtitle = new Label("Odaberite projekat za otvaranje, kreirajte novi ili obrišite postojeći.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B6B6B;");

        ListView<Document> list = new ListView<>();
        list.setPrefHeight(460);
        list.setMaxWidth(Double.MAX_VALUE);

        list.setStyle(
                "-fx-background-color: #FBFAF6;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-radius: 16;" +
                        "-fx-border-color: rgba(0,0,0,0.12);" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 2;"
        );

        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                UnitSystem unit = item.getUnitSystem();
                double width = UnitConverter.fromCm(item.getWidthCm(), unit);
                double height = UnitConverter.fromCm(item.getHeightCm(), unit);
                String unitLabel = unit == UnitSystem.IN ? "in" : "cm";

                setText(item.getName() + " (" + format(width) + " x " + format(height) + " " + unitLabel + ")");
                setStyle("-fx-font-size: 14px; -fx-padding: 12 14;");
            }
        });

        final int userId = sessionManager.getCurrentUser().getId();
        List<Document> docs = documentDao.findByUser(userId);
        list.getItems().setAll(docs);

        final ProjectPackageService packageService = new ProjectPackageService(
                documentDao,
                new NodeDao(),
                new EdgeDao(),
                new GuideDao(),
                new ShapeDao(),
                new ManualShapeDao(),
                new DimensionDao(),
                new MaterialDao()
        );

        Button newButton = new Button("Novi projekt");
        setupPrimaryButton(newButton);

        newButton.setOnAction(event -> {
            ProjectDialog dialog = new ProjectDialog("Novi projekat", null);
            dialog.showAndWait().ifPresent(settings -> {
                int id = documentDao.createDocument(
                        userId,
                        settings.getName(),
                        settings.getWidthCm(),
                        settings.getHeightCm(),
                        settings.getKerfMm(),
                        settings.getUnitSystem()
                );
                navigator.showMain(id);
            });
        });

        Button openButton = new Button("Otvori");
        setupPrimaryButton(openButton);

        openButton.setOnAction(event -> {
            Document selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigator.showMain(selected.getId());
            }
        });

        Button exportButton = new Button("Izvezi");
        setupPrimaryButton(exportButton);

        exportButton.setOnAction(event -> {
            Document selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Spremi projekat kao paket");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("WoodCraft paket (*.wcp.zip)", "*.wcp.zip")
            );
            chooser.setInitialFileName(selected.getName().replaceAll("\\s+", "_") + ".wcp.zip");

            File out = chooser.showSaveDialog(root.getScene().getWindow());
            if (out == null) return;

            try {
                packageService.exportDocument(selected.getId(), userId, out.toPath());
                new Alert(Alert.AlertType.INFORMATION, "Izvoz uspješan!").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Greška pri izvozu:\n" + ex.getMessage()).showAndWait();
            }
        });

        Button importButton = new Button("Uvezi");
        setupPrimaryButton(importButton);

        importButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Odaberi paket za uvoz");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("WoodCraft paket (*.wcp.zip)", "*.wcp.zip")
            );

            File in = chooser.showOpenDialog(root.getScene().getWindow());
            if (in == null) return;

            try {
                String suggestedName = packageService.readSuggestedName(in.toPath());
                String finalName = resolveNameCollision(documentDao, userId, suggestedName);

                packageService.importPackage(in.toPath(), userId, finalName);

                list.getItems().setAll(documentDao.findByUser(userId));

                new Alert(
            Alert.AlertType.INFORMATION,
                 "Uvoz uspješan! Projekat je dodan na listu.\n\n" +
                 "Odaberite projekat i kliknite 'Otvori'."
                ).showAndWait();


            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Greška pri uvozu:\n" + ex.getMessage()).showAndWait();
            }
        });

        Button deleteButton = new Button("Obriši");
        setupDangerButton(deleteButton);

        deleteButton.setOnAction(event -> {
            Document selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Obrisati projekt \"" + selected.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Potvrda brisanja");
            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    documentDao.deleteByIdCascade(selected.getId());
                    list.getItems().setAll(documentDao.findByUser(userId));
                }
            });
        });

        HBox actions = new HBox(12, newButton, openButton, exportButton, importButton, deleteButton);
        actions.setAlignment(Pos.CENTER);

        HBox.setHgrow(newButton, Priority.ALWAYS);
        HBox.setHgrow(openButton, Priority.ALWAYS);
        HBox.setHgrow(exportButton, Priority.ALWAYS);
        HBox.setHgrow(importButton, Priority.ALWAYS);
        HBox.setHgrow(deleteButton, Priority.ALWAYS);

        card.getChildren().addAll(
                title,
                subtitle,
                spacer(8),
                list,
                spacer(10),
                actions
        );

        root.setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);
        BorderPane.setMargin(card, new Insets(32, 28, 70, 28));
    }

    public Parent getRoot() {
        return root;
    }

    private String format(double value) {
        return String.format("%.2f", value).replaceAll("\\.00$", "");
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static void setupPrimaryButton(Button b) {
        b.setMinHeight(46);
        b.setMaxWidth(Double.MAX_VALUE);

        stylePrimaryButton(b, false);
        b.setOnMouseEntered(e -> stylePrimaryButton(b, true));
        b.setOnMouseExited(e -> stylePrimaryButton(b, false));
    }

    private static void setupDangerButton(Button b) {
        b.setMinHeight(46);
        b.setMaxWidth(Double.MAX_VALUE);

        styleDangerButton(b, false);
        b.setOnMouseEntered(e -> styleDangerButton(b, true));
        b.setOnMouseExited(e -> styleDangerButton(b, false));
    }

    private static void stylePrimaryButton(Button b, boolean hover) {
        String base = hover ? "#547357" : "#6E8F6A";
        b.setStyle(
                "-fx-background-color: " + base + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;"
        );
    }

    private static void styleDangerButton(Button b, boolean hover) {
        String base = hover ? "#B54949" : "#C85C5C";
        b.setStyle(
                "-fx-background-color: " + base + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 800;" +
                        "-fx-background-radius: 16;" +
                        "-fx-cursor: hand;"
        );
    }

    // ✅ FIX: name se mijenja u while petlji, pa prije lambda napravimo final kopiju
    private static String resolveNameCollision(DocumentDao documentDao, int userId, String suggested) {
        String name = (suggested == null || suggested.isBlank()) ? "Uvezeni projekat" : suggested.trim();

        while (true) {
            final String nameToCheck = name; // <-- OVO rješava "effectively final" problem

            boolean exists = documentDao.findByUser(userId).stream()
                    .anyMatch(d -> d.getName().equalsIgnoreCase(nameToCheck));

            if (!exists) return name;

            TextInputDialog dialog = new TextInputDialog(name + " (1)");
            dialog.setHeaderText("Naziv projekta već postoji");
            dialog.setContentText("Unesite novi naziv projekta:");
            var result = dialog.showAndWait();

            if (result.isEmpty() || result.get().isBlank()) {
                throw new IllegalStateException("Uvoz otkazan (nije unesen novi naziv).");
            }
            name = result.get().trim();
        }
    }
}

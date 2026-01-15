package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unze.ptf.woodcraft.woodcraft.dao.DocumentDao;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;
import unze.ptf.woodcraft.woodcraft.util.UnitConverter;

import java.util.List;

public class ProjectListView {
    private final BorderPane root = new BorderPane();

    public ProjectListView(SessionManager sessionManager, DocumentDao documentDao, SceneNavigator navigator) {
        Label title = new Label("Your Projects");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        ListView<Document> list = new ListView<>();
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                UnitSystem unit = item.getUnitSystem();
                double width = UnitConverter.fromCm(item.getWidthCm(), unit);
                double height = UnitConverter.fromCm(item.getHeightCm(), unit);
                String unitLabel = unit == UnitSystem.IN ? "in" : "cm";
                setText(item.getName() + " (" + format(width) + " x " + format(height) + " " + unitLabel + ")");
            }
        });

        List<Document> docs = documentDao.findByUser(sessionManager.getCurrentUser().getId());
        list.getItems().setAll(docs);

        Button openButton = new Button("Open");
        openButton.setOnAction(event -> {
            Document selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                navigator.showMain(selected.getId());
            }
        });

        Button newButton = new Button("New Project");
        newButton.setOnAction(event -> {
            ProjectDialog dialog = new ProjectDialog("New Project", null);
            dialog.showAndWait().ifPresent(settings -> {
                int id = documentDao.createDocument(
                        sessionManager.getCurrentUser().getId(),
                        settings.getName(),
                        settings.getWidthCm(),
                        settings.getHeightCm(),
                        settings.getKerfMm(),
                        settings.getUnitSystem()
                );
                navigator.showMain(id);
            });
        });

        HBox actions = new HBox(10, newButton, openButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, title, list, actions);
        content.setPadding(new Insets(20));
        content.setPrefWidth(520);

        root.setCenter(content);
    }

    public Parent getRoot() {
        return root;
    }

    private String format(double value) {
        return String.format("%.2f", value).replaceAll("\\.00$", "");
    }
}

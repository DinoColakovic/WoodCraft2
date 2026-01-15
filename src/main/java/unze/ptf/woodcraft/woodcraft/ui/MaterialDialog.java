package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import unze.ptf.woodcraft.woodcraft.model.GrainDirection;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.MaterialType;

public class MaterialDialog extends Dialog<Material> {
    public MaterialDialog(int userId) {
        this(userId, null);
    }

    public MaterialDialog(int userId, Material existing) {
        setTitle(existing == null ? "Add Material" : "Edit Material");
        setHeaderText("Define a material for estimation.");

        ButtonType saveButton = new ButtonType("Save", ButtonType.OK.getButtonData());
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        ComboBox<MaterialType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(MaterialType.SHEET, MaterialType.LUMBER);
        typeBox.getSelectionModel().select(MaterialType.SHEET);

        TextField sheetWidthField = new TextField("244");
        TextField sheetHeightField = new TextField("122");
        TextField sheetPriceField = new TextField("0");
        TextField pricePerSquareField = new TextField("0");
        TextField pricePerLinearField = new TextField("0");
        TextField edgeBandingField = new TextField("0");
        ComboBox<GrainDirection> grainBox = new ComboBox<>();
        grainBox.getItems().addAll(GrainDirection.NONE, GrainDirection.HORIZONTAL, GrainDirection.VERTICAL);
        grainBox.getSelectionModel().select(GrainDirection.NONE);
        TextField imagePathField = new TextField();
        imagePathField.setPromptText("Optional image file path");

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Material Image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            var window = getDialogPane().getScene() == null ? null : getDialogPane().getScene().getWindow();
            var selected = chooser.showOpenDialog(window);
            if (selected != null) {
                imagePathField.setText(selected.getAbsolutePath());
            }
        });

        HBox imageRow = new HBox(8, imagePathField, browseButton);

        if (existing != null) {
            nameField.setText(existing.getName());
            typeBox.getSelectionModel().select(existing.getType());
            sheetWidthField.setText(format(existing.getSheetWidthCm()));
            sheetHeightField.setText(format(existing.getSheetHeightCm()));
            sheetPriceField.setText(format(existing.getSheetPrice()));
            pricePerSquareField.setText(format(existing.getPricePerSquareMeter()));
            pricePerLinearField.setText(format(existing.getPricePerLinearMeter()));
            edgeBandingField.setText(format(existing.getEdgeBandingCostPerMeter()));
            grainBox.getSelectionModel().select(existing.getGrainDirection());
            if (existing.getImagePath() != null) {
                imagePathField.setText(existing.getImagePath());
            }
        }

        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Type"), typeBox);
        grid.addRow(2, new Label("Sheet width (cm)"), sheetWidthField);
        grid.addRow(3, new Label("Sheet height (cm)"), sheetHeightField);
        grid.addRow(4, new Label("Sheet price"), sheetPriceField);
        grid.addRow(5, new Label("Price per m2"), pricePerSquareField);
        grid.addRow(6, new Label("Price per linear m"), pricePerLinearField);
        grid.addRow(7, new Label("Edge banding $/m"), edgeBandingField);
        grid.addRow(8, new Label("Grain"), grainBox);
        grid.addRow(9, new Label("Image"), imageRow);

        getDialogPane().setContent(grid);

        setResultConverter(button -> {
            if (button == saveButton) {
                int id = existing == null ? -1 : existing.getId();
                return new Material(
                        id,
                        userId,
                        nameField.getText().trim(),
                        typeBox.getValue(),
                        parseDouble(sheetWidthField.getText()),
                        parseDouble(sheetHeightField.getText()),
                        parseDouble(sheetPriceField.getText()),
                        parseDouble(pricePerSquareField.getText()),
                        parseDouble(pricePerLinearField.getText()),
                        imagePathField.getText().isBlank() ? null : imagePathField.getText().trim(),
                        grainBox.getValue(),
                        parseDouble(edgeBandingField.getText())
                );
            }
            return null;
        });
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String format(double value) {
        return String.format("%.2f", value).replaceAll("\\.00$", "");
    }
}

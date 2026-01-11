package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.MaterialType;

public class MaterialDialog extends Dialog<Material> {
    public MaterialDialog(int userId) {
        setTitle("Add Material");
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
        ColorPicker colorPicker = new ColorPicker(Color.web("#8FAADC"));

        TextField sheetWidthField = new TextField("244");
        TextField sheetHeightField = new TextField("122");
        TextField sheetPriceField = new TextField("0");
        TextField pricePerSquareField = new TextField("0");
        TextField pricePerLinearField = new TextField("0");

        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Type"), typeBox);
        grid.addRow(2, new Label("Color"), colorPicker);
        grid.addRow(3, new Label("Sheet width (cm)"), sheetWidthField);
        grid.addRow(4, new Label("Sheet height (cm)"), sheetHeightField);
        grid.addRow(5, new Label("Sheet price"), sheetPriceField);
        grid.addRow(6, new Label("Price per m²"), pricePerSquareField);
        grid.addRow(7, new Label("Price per linear m"), pricePerLinearField);

        getDialogPane().setContent(grid);

        setResultConverter(button -> {
            if (button == saveButton) {
                return new Material(
                        -1,
                        userId,
                        nameField.getText().trim(),
                        typeBox.getValue(),
                        toHex(colorPicker.getValue()),
                        parseDouble(sheetWidthField.getText()),
                        parseDouble(sheetHeightField.getText()),
                        parseDouble(sheetPriceField.getText()),
                        parseDouble(pricePerSquareField.getText()),
                        parseDouble(pricePerLinearField.getText())
                );
            }
            return null;
        });
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}

package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.UnitSystem;
import unze.ptf.woodcraft.woodcraft.util.UnitConverter;

public class ProjectDialog extends Dialog<DocumentSettings> {
    public ProjectDialog(String title, Document existing) {
        setTitle(title);
        setHeaderText("Project settings");

        ButtonType saveButton = new ButtonType("Save", ButtonType.OK.getButtonData());
        getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        TextField widthField = new TextField();
        TextField heightField = new TextField();
        TextField kerfField = new TextField("3");
        ComboBox<UnitSystem> unitBox = new ComboBox<>();
        unitBox.getItems().addAll(UnitSystem.CM, UnitSystem.IN);
        unitBox.getSelectionModel().select(UnitSystem.CM);

        if (existing != null) {
            nameField.setText(existing.getName());
            unitBox.getSelectionModel().select(existing.getUnitSystem());
            widthField.setText(format(UnitConverter.fromCm(existing.getWidthCm(), existing.getUnitSystem())));
            heightField.setText(format(UnitConverter.fromCm(existing.getHeightCm(), existing.getUnitSystem())));
            kerfField.setText(format(existing.getKerfMm()));
        } else {
            nameField.setText("New Project");
            widthField.setText("244");
            heightField.setText("122");
        }

        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Units"), unitBox);
        grid.addRow(2, new Label("Width"), widthField);
        grid.addRow(3, new Label("Height"), heightField);
        grid.addRow(4, new Label("Kerf (mm)"), kerfField);

        getDialogPane().setContent(grid);

        setResultConverter(button -> {
            if (button == saveButton) {
                UnitSystem unit = unitBox.getValue();
                double width = parseDouble(widthField.getText(), UnitConverter.fromCm(244, unit));
                double height = parseDouble(heightField.getText(), UnitConverter.fromCm(122, unit));
                double widthCm = UnitConverter.toCm(width, unit);
                double heightCm = UnitConverter.toCm(height, unit);
                double kerfMm = parseDouble(kerfField.getText(), 3);
                return new DocumentSettings(nameField.getText().trim(), widthCm, heightCm, kerfMm, unit);
            }
            return null;
        });
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String format(double value) {
        return String.format("%.2f", value).replaceAll("\\.00$", "");
    }
}

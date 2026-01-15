package unze.ptf.woodcraft.woodcraft.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class RulerPane extends Pane {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private static final double MAX_CANVAS_SIZE = 8192;

    private final Canvas canvas = new Canvas();
    private Orientation orientation = Orientation.HORIZONTAL;
    private double scale = 10.0;

    public RulerPane() {
        getChildren().add(canvas);
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());
    }

    public RulerPane(Orientation orientation) {
        this();
        this.orientation = orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        draw();
    }

    public void setHeight(double h) {
        super.setPrefHeight(h);
        draw();
    }

    public void setWidth(double w) {
        super.setPrefWidth(w);
        draw();
    }

    public void setScale(double scale) {
        this.scale = scale;
        draw();
    }

    public double getScale() {
        return scale;
    }

    @Override
    protected void layoutChildren() {
        double width = clamp(getWidth());
        double height = clamp(getHeight());
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, MAX_CANVAS_SIZE);
    }

    private void draw() {
        layoutChildren();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.GRAY);
        gc.setFill(Color.DIMGRAY);
        gc.setLineWidth(1);
        if (orientation == Orientation.HORIZONTAL) {
            for (int cm = 0; cm <= width / scale; cm++) {
                double x = cm * scale;
                double tick = cm % 10 == 0 ? 12 : 6;
                gc.strokeLine(x, height, x, height - tick);
                if (cm % 10 == 0) {
                    gc.fillText(Integer.toString(cm), x + 2, 12);
                }
            }
        } else {
            for (int cm = 0; cm <= height / scale; cm++) {
                double y = cm * scale;
                double tick = cm % 10 == 0 ? 12 : 6;
                gc.strokeLine(width, y, width - tick, y);
                if (cm % 10 == 0) {
                    gc.fillText(Integer.toString(cm), 2, y + 10);
                }
            }
        }
    }
}

package unze.ptf.woodcraft.woodcraft.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class RulerPane extends Canvas {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private Orientation orientation = Orientation.HORIZONTAL;
    private double scale = 10.0;

    public RulerPane() {
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

    @Override
    public void setHeight(double h) {
        super.setHeight(h);
        draw();
    }

    @Override
    public void setWidth(double w) {
        super.setWidth(w);
        draw();
    }

    public void setScale(double scale) {
        this.scale = scale;
        draw();
    }

    public double getScale() {
        return scale;
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        gc.setStroke(Color.GRAY);
        gc.setFill(Color.DIMGRAY);
        gc.setLineWidth(1);
        if (orientation == Orientation.HORIZONTAL) {
            double width = getWidth();
            for (int cm = 0; cm <= width / scale; cm++) {
                double x = cm * scale;
                double tick = cm % 10 == 0 ? 12 : 6;
                gc.strokeLine(x, getHeight(), x, getHeight() - tick);
                if (cm % 10 == 0) {
                    gc.fillText(Integer.toString(cm), x + 2, 12);
                }
            }
        } else {
            double height = getHeight();
            for (int cm = 0; cm <= height / scale; cm++) {
                double y = cm * scale;
                double tick = cm % 10 == 0 ? 12 : 6;
                gc.strokeLine(getWidth(), y, getWidth() - tick, y);
                if (cm % 10 == 0) {
                    gc.fillText(Integer.toString(cm), 2, y + 10);
                }
            }
        }
    }
}

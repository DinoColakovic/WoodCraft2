package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RulerPane extends Region {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private static final double MAJOR_TICK = 12;
    private static final double MINOR_TICK = 6;
    private static final Font LABEL_FONT = Font.font(10);
    private static final double MAX_CANVAS_SIZE = 4096;

    private final Canvas canvas = new Canvas();
    private Orientation orientation = Orientation.HORIZONTAL;
    private double scale = 10.0; // pixels per "cm"

    public RulerPane() {
        getChildren().add(canvas);
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());
        draw();
    }

    public RulerPane(Orientation orientation) {
        this();
        this.orientation = orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
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
        canvas.relocate(0, 0);
    }

    private double clamp(double value) {
        if (!Double.isFinite(value) || value <= 0) {
            return 1;
        }
        return Math.min(value, MAX_CANVAS_SIZE);
    }

    private void draw() {
        double w = clamp(getWidth());
        double h = clamp(getHeight());

        if (w <= 0 || h <= 0 || scale <= 0) {
            return;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        gc.setFill(Color.DIMGRAY);
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1);
        gc.setFont(LABEL_FONT);
        gc.setTextBaseline(VPos.TOP);

        if (orientation == Orientation.HORIZONTAL) {
            int maxCm = (int) Math.ceil(w / scale);
            for (int cm = 0; cm <= maxCm; cm++) {
                double x = cm * scale;
                double tick = (cm % 10 == 0) ? MAJOR_TICK : MINOR_TICK;

                gc.strokeLine(x, h, x, h - tick);

                if (cm % 10 == 0) {
                    double labelY = Math.max(0, h - tick - 12);
                    gc.fillText(Integer.toString(cm), x + 2, labelY);
                }
            }
        } else {
            int maxCm = (int) Math.ceil(h / scale);
            for (int cm = 0; cm <= maxCm; cm++) {
                double y = cm * scale;
                double tick = (cm % 10 == 0) ? MAJOR_TICK : MINOR_TICK;

                gc.strokeLine(w, y, w - tick, y);

                if (cm % 10 == 0) {
                    double labelX = 2;
                    double labelY = Math.max(0, y - 6);
                    gc.fillText(Integer.toString(cm), labelX, labelY);
                }
            }
        }
    }
}

package unze.ptf.woodcraft.woodcraft.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.VPos;

public class RulerPane extends Pane {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private Orientation orientation = Orientation.HORIZONTAL;
    private double scale = 10.0; // pixels per "cm"
    private static final double MAJOR_TICK = 12;
    private static final double MINOR_TICK = 6;
    private static final Font LABEL_FONT = Font.font(10);

    public RulerPane() {
        // redraw whenever canvas size changes
        widthProperty().addListener((obs, oldVal, newVal) -> draw());
        heightProperty().addListener((obs, oldVal, newVal) -> draw());

        // automatically bind to Region parent sizes when added to a Region
        parentProperty().addListener((obs, oldParent, newParent) -> {
            // unbind from old parent if it was a Region
            if (oldParent instanceof Region) {
                widthProperty().unbind();
                heightProperty().unbind();
            }
            if (newParent instanceof Region) {
                Region r = (Region) newParent;
                // Bind the canvas to fill the parent region
                widthProperty().bind(r.widthProperty());
                heightProperty().bind(r.heightProperty());
            }
        });

        // initial draw (in many cases size will be zero until layout, but harmless)
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
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0 || scale <= 0) {
            return;
        }

        GraphicsContext gc = getGraphicsContext2D();
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

                // tick from bottom up
                gc.strokeLine(x, h, x, h - tick);

                if (cm % 10 == 0) {
                    // place label a bit above the bottom edge (tweak as needed)
                    double labelY = Math.max(0, h - tick - 12);
                    gc.fillText(Integer.toString(cm), x + 2, labelY);
                }
            }
        } else { // VERTICAL
            int maxCm = (int) Math.ceil(h / scale);
            for (int cm = 0; cm <= maxCm; cm++) {
                double y = cm * scale;
                double tick = (cm % 10 == 0) ? MAJOR_TICK : MINOR_TICK;

                // tick from right to left
                gc.strokeLine(w, y, w - tick, y);

                if (cm % 10 == 0) {
                    // place label a few pixels from left edge (tweak as needed)
                    double labelX = 2;
                    double labelY = Math.max(0, y - 6); // center around tick
                    gc.fillText(Integer.toString(cm), labelX, labelY);
                }
            }
        }
    }
}

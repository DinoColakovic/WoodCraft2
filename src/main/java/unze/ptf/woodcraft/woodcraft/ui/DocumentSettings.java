package unze.ptf.woodcraft.woodcraft.ui;

import unze.ptf.woodcraft.woodcraft.model.UnitSystem;

public class DocumentSettings {
    private final String name;
    private final double widthCm;
    private final double heightCm;
    private final double kerfMm;
    private final UnitSystem unitSystem;

    public DocumentSettings(String name, double widthCm, double heightCm, double kerfMm, UnitSystem unitSystem) {
        this.name = name;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.kerfMm = kerfMm;
        this.unitSystem = unitSystem;
    }

    public String getName() {
        return name;
    }

    public double getWidthCm() {
        return widthCm;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public double getKerfMm() {
        return kerfMm;
    }

    public UnitSystem getUnitSystem() {
        return unitSystem;
    }
}

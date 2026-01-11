package unze.ptf.woodcraft.woodcraft.model;

public class Material {
    private final int id;
    private final int userId;
    private final String name;
    private final MaterialType type;
    private final String colorHex;
    private final double sheetWidthCm;
    private final double sheetHeightCm;
    private final double sheetPrice;
    private final double pricePerSquareMeter;
    private final double pricePerLinearMeter;

    public Material(int id, int userId, String name, MaterialType type, String colorHex, double sheetWidthCm,
                    double sheetHeightCm,
                    double sheetPrice, double pricePerSquareMeter, double pricePerLinearMeter) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.colorHex = colorHex;
        this.sheetWidthCm = sheetWidthCm;
        this.sheetHeightCm = sheetHeightCm;
        this.sheetPrice = sheetPrice;
        this.pricePerSquareMeter = pricePerSquareMeter;
        this.pricePerLinearMeter = pricePerLinearMeter;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public MaterialType getType() {
        return type;
    }

    public String getColorHex() {
        return colorHex;
    }

    public double getSheetWidthCm() {
        return sheetWidthCm;
    }

    public double getSheetHeightCm() {
        return sheetHeightCm;
    }

    public double getSheetPrice() {
        return sheetPrice;
    }

    public double getPricePerSquareMeter() {
        return pricePerSquareMeter;
    }

    public double getPricePerLinearMeter() {
        return pricePerLinearMeter;
    }

    public double getSheetAreaCm2() {
        return sheetWidthCm * sheetHeightCm;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

package unze.ptf.woodcraft.woodcraft.model;

public class Material {
    private final int id;
    private final int userId;
    private final String name;
    private final MaterialType type;
    private final double sheetWidthCm;
    private final double sheetHeightCm;
    private final double sheetPrice;
    private final double pricePerSquareMeter;
    private final double pricePerLinearMeter;
    private final String imagePath;
    private final GrainDirection grainDirection;
    private final double edgeBandingCostPerMeter;
    private String colorHex = "#8FAADC";

    public Material(int id, int userId, String name, MaterialType type, double sheetWidthCm, double sheetHeightCm,
                    double sheetPrice, double pricePerSquareMeter, double pricePerLinearMeter, String imagePath,
                    GrainDirection grainDirection, double edgeBandingCostPerMeter) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.sheetWidthCm = sheetWidthCm;
        this.sheetHeightCm = sheetHeightCm;
        this.sheetPrice = sheetPrice;
        this.pricePerSquareMeter = pricePerSquareMeter;
        this.pricePerLinearMeter = pricePerLinearMeter;
        this.imagePath = imagePath;
        this.grainDirection = grainDirection == null ? GrainDirection.NONE : grainDirection;
        this.edgeBandingCostPerMeter = edgeBandingCostPerMeter;
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

    public String getImagePath() {
        return imagePath;
    }

    public GrainDirection getGrainDirection() {
        return grainDirection;
    }

    public double getEdgeBandingCostPerMeter() {
        return edgeBandingCostPerMeter;
    }

    public double getSheetAreaCm2() {
        return sheetWidthCm * sheetHeightCm;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String hex) {
        this.colorHex = hex;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}

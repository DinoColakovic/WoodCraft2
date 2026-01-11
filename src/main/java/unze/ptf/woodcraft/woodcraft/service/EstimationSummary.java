package unze.ptf.woodcraft.woodcraft.service;

public class EstimationSummary {
    private final String materialName;
    private final String details;
    private final double cost;

    public EstimationSummary(String materialName, String details, double cost) {
        this.materialName = materialName;
        this.details = details;
        this.cost = cost;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getDetails() {
        return details;
    }

    public double getCost() {
        return cost;
    }
}

package unze.ptf.woodcraft.woodcraft.model;

import java.util.List;

public class ShapePolygon {
    private final int id;
    private final int documentId;
    private final Integer materialId;
    private final int quantity;
    private final List<NodePoint> nodes;
    private final double areaCm2;
    private final double perimeterCm;

    public ShapePolygon(int id, int documentId, Integer materialId, int quantity, List<NodePoint> nodes,
                        double areaCm2, double perimeterCm) {
        this.id = id;
        this.documentId = documentId;
        this.materialId = materialId;
        this.quantity = quantity;
        this.nodes = nodes;
        this.areaCm2 = areaCm2;
        this.perimeterCm = perimeterCm;
    }

    public int getId() {
        return id;
    }

    public int getDocumentId() {
        return documentId;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public int getQuantity() {
        return quantity;
    }

    public List<NodePoint> getNodes() {
        return nodes;
    }

    public double getAreaCm2() {
        return areaCm2;
    }

    public double getPerimeterCm() {
        return perimeterCm;
    }
}

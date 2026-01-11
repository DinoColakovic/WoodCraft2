package unze.ptf.woodcraft.woodcraft.model;

public class NodePoint {
    private final int id;
    private final int documentId;
    private final double xCm;
    private final double yCm;

    public NodePoint(int id, int documentId, double xCm, double yCm) {
        this.id = id;
        this.documentId = documentId;
        this.xCm = xCm;
        this.yCm = yCm;
    }

    public int getId() {
        return id;
    }

    public int getDocumentId() {
        return documentId;
    }

    public double getXCm() {
        return xCm;
    }

    public double getYCm() {
        return yCm;
    }
}

package unze.ptf.woodcraft.woodcraft.model;

public class Guide {
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }

    private final int id;
    private final int documentId;
    private final Orientation orientation;
    private final double positionCm;

    public Guide(int id, int documentId, Orientation orientation, double positionCm) {
        this.id = id;
        this.documentId = documentId;
        this.orientation = orientation;
        this.positionCm = positionCm;
    }

    public int getId() {
        return id;
    }

    public int getDocumentId() {
        return documentId;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public double getPositionCm() {
        return positionCm;
    }
}

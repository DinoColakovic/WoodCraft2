package unze.ptf.woodcraft.woodcraft.model;

public class Edge {
    private final int id;
    private final int documentId;
    private final int startNodeId;
    private final int endNodeId;

    public Edge(int id, int documentId, int startNodeId, int endNodeId) {
        this.id = id;
        this.documentId = documentId;
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
    }

    public int getId() {
        return id;
    }

    public int getDocumentId() {
        return documentId;
    }

    public int getStartNodeId() {
        return startNodeId;
    }

    public int getEndNodeId() {
        return endNodeId;
    }
}

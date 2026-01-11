package unze.ptf.woodcraft.woodcraft.model;

public class Document {
    private final int id;
    private final int userId;
    private final String name;

    public Document(int id, int userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
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
}

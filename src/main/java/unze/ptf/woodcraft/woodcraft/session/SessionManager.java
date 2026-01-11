package unze.ptf.woodcraft.woodcraft.session;

import unze.ptf.woodcraft.woodcraft.model.User;

public class SessionManager {
    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }
}

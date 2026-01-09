package unze.ptf.woodcraft.woodcraft.service;

import org.mindrot.jbcrypt.BCrypt;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.model.User;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;

import java.util.Optional;

public class AuthService {
    private final UserDao userDao;
    private final SessionManager sessionManager;

    public AuthService(UserDao userDao, SessionManager sessionManager) {
        this.userDao = userDao;
        this.sessionManager = sessionManager;
    }

    public Optional<User> login(String username, String password) {
        Optional<User> user = userDao.findByUsername(username);
        if (user.isPresent() && BCrypt.checkpw(password, user.get().getPasswordHash())) {
            sessionManager.setCurrentUser(user.get());
            return user;
        }
        return Optional.empty();
    }

    public User register(String username, String password, Role role) {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        int userId = userDao.createUser(username, hash, role);
        User user = new User(userId, username, hash, role);
        sessionManager.setCurrentUser(user);
        return user;
    }

    public void logout() {
        sessionManager.setCurrentUser(null);
    }
}

package unze.ptf.woodcraft.woodcraft.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import unze.ptf.woodcraft.woodcraft.dao.DocumentDao;
import unze.ptf.woodcraft.woodcraft.dao.EdgeDao;
import unze.ptf.woodcraft.woodcraft.dao.GuideDao;
import unze.ptf.woodcraft.woodcraft.dao.MaterialDao;
import unze.ptf.woodcraft.woodcraft.dao.NodeDao;
import unze.ptf.woodcraft.woodcraft.dao.ShapeDao;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.service.AuthService;
import unze.ptf.woodcraft.woodcraft.service.EstimationService;
import unze.ptf.woodcraft.woodcraft.service.GeometryService;
import unze.ptf.woodcraft.woodcraft.session.SessionManager;

import java.io.IOException;

public class SceneNavigator {
    private final Stage stage;
    private final SessionManager sessionManager;
    private final AuthService authService;
    private final UserDao userDao;
    private final MaterialDao materialDao;
    private final DocumentDao documentDao;
    private final NodeDao nodeDao;
    private final EdgeDao edgeDao;
    private final GuideDao guideDao;
    private final ShapeDao shapeDao;
    private final GeometryService geometryService;
    private final EstimationService estimationService;

    public SceneNavigator(Stage stage, SessionManager sessionManager, AuthService authService, UserDao userDao,
                          MaterialDao materialDao, DocumentDao documentDao, NodeDao nodeDao, EdgeDao edgeDao,
                          GuideDao guideDao, ShapeDao shapeDao, GeometryService geometryService,
                          EstimationService estimationService) {
        this.stage = stage;
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.userDao = userDao;
        this.materialDao = materialDao;
        this.documentDao = documentDao;
        this.nodeDao = nodeDao;
        this.edgeDao = edgeDao;
        this.guideDao = guideDao;
        this.shapeDao = shapeDao;
        this.geometryService = geometryService;
        this.estimationService = estimationService;
    }

    public void showInitialScene() {
        if (userDao.countUsers() == 0) {
            showSignup();
        } else {
            showLogin();
        }
        stage.setTitle("WoodCraft Planner");
        stage.show();
    }

    public void showLogin() {
        LoginView view = new LoginView(authService, this);
        Scene scene = new Scene(view.getRoot(), 620, 420);
        stage.setScene(scene);
    }

    public void showSignup() {
        SignupView view = new SignupView(authService, this, userDao);
        Scene scene = new Scene(view.getRoot(), 620, 450);
        stage.setScene(scene);
    }

    public void showMain() {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource("MainView.fxml"));
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(sessionManager, authService, materialDao, documentDao, nodeDao, edgeDao,
                        guideDao, shapeDao, geometryService, estimationService);
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create controller", exception);
            }
        });
        try {
            Scene scene = new Scene(loader.load(), 1200, 800);
            stage.setScene(scene);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load main view", exception);
        }
    }
}

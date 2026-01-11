package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.service.AuthService;

public class SignupView {
    private final BorderPane root = new BorderPane();

    public SignupView(AuthService authService, SceneNavigator navigator, UserDao userDao) {
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20));

        Label title = new Label("Create Account");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm password");

        Label message = new Label();
        message.setStyle("-fx-text-fill: #b00020;");

        Label note = new Label();
        if (userDao.countUsers() == 0) {
            note.setText("First account will be created as ADMIN.");
        }

        Button createButton = new Button("Create account");
        createButton.setDefaultButton(true);
        createButton.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                message.setText("Enter username and password.");
                return;
            }
            if (!password.equals(confirm)) {
                message.setText("Passwords do not match.");
                return;
            }
            if (userDao.findByUsername(username).isPresent()) {
                message.setText("Username already exists.");
                return;
            }
            Role role = userDao.countUsers() == 0 ? Role.ADMIN : Role.USER;
            authService.register(username, password, role);
            navigator.showMain();
        });

        Button loginLink = new Button("Back to login");
        loginLink.setOnAction(event -> navigator.showLogin());

        form.getChildren().addAll(title, usernameField, passwordField, confirmField, note, createButton, message, loginLink);
        root.setCenter(form);
        root.setPadding(new Insets(20));
    }

    public Parent getRoot() {
        return root;
    }
}

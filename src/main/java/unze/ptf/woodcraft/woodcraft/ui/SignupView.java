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

        Label title = new Label("Izradi racun");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Korisnicko ime");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Lozinka");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Potvrdi lozinku");

        Label message = new Label();
        message.setStyle("-fx-text-fill: #b00020;");

        Label note = new Label();
        if (userDao.countUsers() == 0) {
            note.setText("Prvi racun ce biti ADMIN.");
        }

        Button createButton = new Button("Izradi racun");
        createButton.setDefaultButton(true);
        createButton.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                message.setText("Unesite korisnicko ime i lozinku.");
                return;
            }
            if (!password.equals(confirm)) {
                message.setText("Lozinke se ne podudaraju.");
                return;
            }
            if (userDao.findByUsername(username).isPresent()) {
                message.setText("Korisnicko ime vec postoji.");
                return;
            }
            Role role = userDao.countUsers() == 0 ? Role.ADMIN : Role.USER;
            authService.register(username, password, role);
            navigator.showProjects();
        });

        Button loginLink = new Button("Natrag na prijavu");
        loginLink.setOnAction(event -> navigator.showLogin());

        form.getChildren().addAll(title, usernameField, passwordField, confirmField, note, createButton, message, loginLink);
        root.setCenter(form);
        root.setPadding(new Insets(20));
    }

    public Parent getRoot() {
        return root;
    }
}

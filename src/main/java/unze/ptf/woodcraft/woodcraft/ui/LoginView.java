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
import unze.ptf.woodcraft.woodcraft.service.AuthService;

public class LoginView {
    private final BorderPane root = new BorderPane();

    public LoginView(AuthService authService, SceneNavigator navigator) {
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20));

        Label title = new Label("WoodCraft Prijava");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Korisnicko ime");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Lozinka");

        Label message = new Label();
        message.setStyle("-fx-text-fill: #b00020;");

        Button loginButton = new Button("Prijava");
        loginButton.setDefaultButton(true);
        loginButton.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                message.setText("Unesite korisnicko ime i lozinku.");
                return;
            }
            if (authService.login(username, password).isPresent()) {
                navigator.showProjects();
            } else {
                message.setText("Neispravni podaci.");
            }
        });

        Button signupLink = new Button("Izradi racun");
        signupLink.setOnAction(event -> navigator.showSignup());

        form.getChildren().addAll(title, usernameField, passwordField, loginButton, message, signupLink);

        root.setCenter(form);
        root.setPadding(new Insets(20));
    }

    public Parent getRoot() {
        return root;
    }
}

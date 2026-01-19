package unze.ptf.woodcraft.woodcraft.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import unze.ptf.woodcraft.woodcraft.service.AuthService;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class LoginView {

    private final BorderPane root = new BorderPane();

    public LoginView(AuthService authService, SceneNavigator navigator) {

        // 🌿 Pozadina
        root.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #F3F0E8, #FFFFFF);"
        );

        // ===========================
        // 🧱 Card
        // ===========================
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(30, 28, 26, 28));
        card.setMaxWidth(460);

        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                "-fx-background-radius: 22;" +
                "-fx-border-radius: 22;" +
                "-fx-border-color: rgba(60,60,60,0.10);" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 26, 0.25, 0, 10);"
        );

        // ===========================
        // 🪵 Logo
        // ===========================
        ImageView logo = new ImageView();
        logo.setPreserveRatio(true);
        logo.setSmooth(true);

        boolean loaded = loadLogo(logo);
        if (!loaded) {
            logo.setVisible(false);
            logo.setManaged(false);
        }

        logo.fitWidthProperty().bind(
                Bindings.createDoubleBinding(() -> {
                    double w = root.getWidth();
                    double desired = w * 0.32;
                    return clamp(desired, 150, 320);
                }, root.widthProperty())
        );

        Region logoSpacer = new Region();
        logoSpacer.minHeightProperty().bind(
                Bindings.createDoubleBinding(
                        () -> clamp(root.getHeight() * 0.018, 4, 12),
                        root.heightProperty()
                )
        );

        // ===========================
        // 📝 Tekst
        // ===========================
        Label title = new Label("Dobrodošli u WoodCraft");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2C2C2C;");

        Label subtitle = new Label("Prijavite se kako biste nastavili");
        subtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #6B6B6B;");

        // ===========================
        // ✏️ Polja
        // ===========================
        TextField username = new TextField();
        username.setPromptText("Korisničko ime");
        styleInput(username);

        PasswordField password = new PasswordField();
        password.setPromptText("Lozinka");
        styleInput(password);

        Label message = new Label();
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #B00020; -fx-font-size: 12px;");

        // ===========================
        // 🌿 Dugme
        // ===========================
        Button login = new Button("Prijava");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setMinHeight(46);
        stylePrimaryButton(login, false);

        login.setOnMouseEntered(e -> stylePrimaryButton(login, true));
        login.setOnMouseExited(e -> stylePrimaryButton(login, false));

        login.setOnAction(e -> {
            String u = username.getText().trim();
            String p = password.getText();

            if (u.isEmpty() || p.isEmpty()) {
                message.setText("Unesite korisničko ime i lozinku.");
                return;
            }

            if (authService.login(u, p).isPresent()) {
                message.setText("");
                navigator.showProjects();
            } else {
                message.setText("Neispravni podaci. Pokušajte ponovo.");
            }
        });

        // ===========================
        // 🔗 Registracija
        // ===========================
        Label noAcc = new Label("Nemate račun?");
        noAcc.setStyle("-fx-text-fill: #6B6B6B; -fx-font-size: 12px;");

        Button signup = new Button("Izradite račun");
        signup.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: #6E8F6A;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 0;" +
                "-fx-cursor: hand;"
        );

        signup.setOnMouseEntered(e ->
                signup.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #547357;" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 0;" +
                        "-fx-underline: true;" +
                        "-fx-cursor: hand;"
                )
        );

        signup.setOnMouseExited(e ->
                signup.setStyle(
                        "-fx-background-color: transparent;" +
                        "-fx-text-fill: #6E8F6A;" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 0;" +
                        "-fx-underline: false;" +
                        "-fx-cursor: hand;"
                )
        );

        signup.setOnAction(e -> navigator.showSignup());

        VBox footer = new VBox(3, noAcc, signup);
        footer.setAlignment(Pos.CENTER);

        // ===========================
        // Složeno
        // ===========================
        card.getChildren().addAll(
                logo,
                logoSpacer,
                title,
                subtitle,
                spacer(8),
                username,
                password,
                spacer(2),
                login,
                message,
                spacer(8),
                footer
        );

        BorderPane.setAlignment(card, Pos.CENTER);
        BorderPane.setMargin(card, new Insets(30, 22, 60, 22));
        root.setCenter(card);
    }

    public Parent getRoot() {
        return root;
    }

    // =================== HELPERS ===================

    private static void styleInput(TextField field) {
        field.setMaxWidth(Double.MAX_VALUE);
        field.setMinHeight(44);

        String normal =
                "-fx-background-color: #FBFAF6;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: rgba(0,0,0,0.12);" +
                "-fx-border-width: 1;" +
                "-fx-padding: 11 12;" +
                "-fx-font-size: 13.5px;";

        String focused =
                "-fx-background-color: #FFFFFF;" +
                "-fx-background-radius: 14;" +
                "-fx-border-radius: 14;" +
                "-fx-border-color: #6E8F6A;" +
                "-fx-border-width: 1.4;" +
                "-fx-padding: 11 12;" +
                "-fx-font-size: 13.5px;";

        field.setStyle(normal);
        field.focusedProperty().addListener(
                (obs, oldVal, isFocused) -> field.setStyle(isFocused ? focused : normal)
        );
    }

    private static void stylePrimaryButton(Button b, boolean hover) {
        String base = hover ? "#547357" : "#6E8F6A";
        b.setStyle(
                "-fx-background-color: " + base + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 14;" +
                "-fx-cursor: hand;"
        );
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean loadLogo(ImageView logo) {
        try {
            URL resUrl = LoginView.class.getResource("/images/logo.png");
            if (resUrl != null) {
                logo.setImage(new Image(resUrl.toExternalForm()));
                return true;
            }
        } catch (Exception ignored) {}

        try {
            String dir = System.getProperty("user.dir");
            File file = new File(dir + File.separator + "src" + File.separator +
                    "main" + File.separator + "resources" + File.separator +
                    "images" + File.separator + "logo.png");
            if (file.exists()) {
                logo.setImage(new Image(new FileInputStream(file)));
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }
}

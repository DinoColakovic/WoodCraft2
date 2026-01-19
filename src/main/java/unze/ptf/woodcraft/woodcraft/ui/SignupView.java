package unze.ptf.woodcraft.woodcraft.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.service.AuthService;

import java.net.URL;

public class SignupView {

    private final BorderPane root = new BorderPane();

    public SignupView(AuthService authService, SceneNavigator navigator, UserDao userDao) {

        // 🌿 pozadina
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F3F0E8, #FFFFFF);");

        // ===========================
        // 🧱 Card
        // ===========================
        VBox card = new VBox(12);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(30, 28, 26, 28));
        card.setMaxWidth(460);

        // ✅ ključ: card ne raste u visinu (sprječava “bijelo do dna”)
        card.setMaxHeight(Region.USE_PREF_SIZE);

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

        // ===========================
        // 📝 Tekst
        // ===========================
        Label title = new Label("Izrada računa");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2C2C2C;");

        Label subtitle = new Label("Popunite podatke za kreiranje računa");
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

        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Potvrda lozinke");
        styleInput(confirm);

        Label message = new Label();
        message.setWrapText(true);
        message.setStyle("-fx-text-fill:#B00020; -fx-font-size:12px;");

        // ===========================
        // 🌿 Dugme - Izradi račun (sa hover)
        // ===========================
        Button create = new Button("Izradi račun");
        create.setDefaultButton(true);
        create.setMaxWidth(Double.MAX_VALUE);
        create.setMinHeight(46);
        stylePrimaryButton(create, false);

        create.setOnMouseEntered(e -> stylePrimaryButton(create, true));
        create.setOnMouseExited(e -> stylePrimaryButton(create, false));

        // ✅ logika (ostaje ista, samo malo urednija poruka)
        create.setOnAction(e -> {
            String u = username.getText().trim();
            String p = password.getText();
            String c = confirm.getText();

            if (u.isEmpty() || p.isEmpty()) {
                message.setText("Unesite korisničko ime i lozinku.");
                return;
            }
            if (!p.equals(c)) {
                message.setText("Lozinke se ne podudaraju.");
                return;
            }
            if (userDao.findByUsername(u).isPresent()) {
                message.setText("Korisničko ime već postoji.");
                return;
            }

            Role role = userDao.countUsers() == 0 ? Role.ADMIN : Role.USER;
            authService.register(u, p, role);
            navigator.showProjects();
        });

        // ===========================
        // 🔗 Link - Prijavite se (sa hover)
        // ===========================
        Button back = new Button("Prijavite se");
        setLinkStyle(back, false);

        back.setOnMouseEntered(e -> setLinkStyle(back, true));
        back.setOnMouseExited(e -> setLinkStyle(back, false));

        back.setOnAction(e -> navigator.showLogin());

        // ===========================
        // Složeno
        // ===========================
        card.getChildren().addAll(
                logo,
                title,
                subtitle,
                spacer(8),
                username,
                password,
                confirm,
                create,
                message,
                spacer(10),
                back
        );

        root.setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);

        // ✅ ovo “skrati” donji prostor i podigne card
        BorderPane.setMargin(card, new Insets(30, 22, 140, 22));
    }

    public Parent getRoot() {
        return root;
    }

    // ================= HELPERS =================

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

    private static void setLinkStyle(Button b, boolean hover) {
        b.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: " + (hover ? "#547357" : "#6E8F6A") + ";" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 0;" +
                "-fx-underline: " + (hover ? "true" : "false") + ";" +
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
            URL url = SignupView.class.getResource("/images/logo.png");
            if (url != null) {
                logo.setImage(new Image(url.toExternalForm()));
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}

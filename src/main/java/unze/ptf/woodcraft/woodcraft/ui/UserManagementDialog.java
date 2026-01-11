package unze.ptf.woodcraft.woodcraft.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.mindrot.jbcrypt.BCrypt;
import unze.ptf.woodcraft.woodcraft.dao.UserDao;
import unze.ptf.woodcraft.woodcraft.model.Role;
import unze.ptf.woodcraft.woodcraft.model.User;

public class UserManagementDialog extends Dialog<Void> {
    public UserManagementDialog(UserDao userDao) {
        setTitle("User Management");
        setHeaderText("Create users, reset passwords, and assign roles.");
        getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        ListView<User> userList = new ListView<>();
        userList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername() + " (" + item.getRole() + ")");
            }
        });
        userList.getItems().setAll(userDao.findAll());

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<Role> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(Role.USER, Role.ADMIN);
        roleBox.getSelectionModel().select(Role.USER);

        Button createUser = new Button("Create User");
        createUser.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                return;
            }
            if (userDao.findByUsername(username).isPresent()) {
                return;
            }
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            userDao.createUser(username, hash, roleBox.getValue());
            userList.getItems().setAll(userDao.findAll());
            usernameField.clear();
            passwordField.clear();
        });

        PasswordField resetPasswordField = new PasswordField();
        resetPasswordField.setPromptText("New password");
        Button resetPassword = new Button("Reset Password");
        resetPassword.setOnAction(event -> {
            User user = userList.getSelectionModel().getSelectedItem();
            if (user == null || resetPasswordField.getText().isEmpty()) {
                return;
            }
            String hash = BCrypt.hashpw(resetPasswordField.getText(), BCrypt.gensalt());
            userDao.updatePassword(user.getId(), hash);
            resetPasswordField.clear();
        });

        ComboBox<Role> updateRoleBox = new ComboBox<>();
        updateRoleBox.getItems().addAll(Role.USER, Role.ADMIN);
        Button updateRole = new Button("Update Role");
        updateRole.setOnAction(event -> {
            User user = userList.getSelectionModel().getSelectedItem();
            Role role = updateRoleBox.getSelectionModel().getSelectedItem();
            if (user == null || role == null) {
                return;
            }
            userDao.updateRole(user.getId(), role);
            userList.getItems().setAll(userDao.findAll());
        });

        GridPane createGrid = new GridPane();
        createGrid.setHgap(10);
        createGrid.setVgap(10);
        createGrid.addRow(0, new Label("New user"));
        createGrid.addRow(1, new Label("Username"), usernameField);
        createGrid.addRow(2, new Label("Password"), passwordField);
        createGrid.addRow(3, new Label("Role"), roleBox);
        createGrid.add(createUser, 1, 4);

        GridPane manageGrid = new GridPane();
        manageGrid.setHgap(10);
        manageGrid.setVgap(10);
        manageGrid.addRow(0, new Label("Selected user"));
        manageGrid.addRow(1, new Label("New password"), resetPasswordField, resetPassword);
        manageGrid.addRow(2, new Label("Role"), updateRoleBox, updateRole);

        VBox right = new VBox(15, createGrid, manageGrid);
        right.setPadding(new Insets(10));

        HBox content = new HBox(10, userList, right);
        content.setPadding(new Insets(10));
        HBox.setHgrow(userList, Priority.ALWAYS);

        getDialogPane().setContent(content);
    }
}

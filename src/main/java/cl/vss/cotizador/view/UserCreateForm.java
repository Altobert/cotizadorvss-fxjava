package cl.vss.cotizador.view;

import cl.vss.cotizador.service.UsuarioDAO;
import cl.vss.cotizador.util.DBConnection;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;

public class UserCreateForm {

    private Stage stage;

    public UserCreateForm() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Crear Usuario");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        Label lblNombre = new Label("Nombre:");
        TextField txtNombre = new TextField();

        Label lblCorreo = new Label("Correo:");
        TextField txtCorreo = new TextField();

        Label lblPassword = new Label("Contraseña:");
        PasswordField txtPassword = new PasswordField();

        Label lblConfirm = new Label("Confirmar:");
        PasswordField txtConfirm = new PasswordField();

        Label lblRol = new Label("Rol:");
        ComboBox<String> comboRol = new ComboBox<>();
        comboRol.getItems().addAll("ADMIN", "USER");
        comboRol.setValue("USER");

        Label lblActivo = new Label("Activo:");
        CheckBox chkActivo = new CheckBox();
        chkActivo.setSelected(true);

        Button btnGuardar = new Button("Guardar");

        grid.add(lblNombre, 0, 0);
        grid.add(txtNombre, 1, 0);

        grid.add(lblCorreo, 0, 1);
        grid.add(txtCorreo, 1, 1);

        grid.add(lblPassword, 0, 2);
        grid.add(txtPassword, 1, 2);

        grid.add(lblConfirm, 0, 3);
        grid.add(txtConfirm, 1, 3);

        grid.add(lblRol, 0, 4);
        grid.add(comboRol, 1, 4);

        grid.add(lblActivo, 0, 5);
        grid.add(chkActivo, 1, 5);

        grid.add(btnGuardar, 1, 6);

        btnGuardar.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            String correo = txtCorreo.getText().trim();
            String pass = txtPassword.getText();
            String confirm = txtConfirm.getText();
            String rol = comboRol.getValue();
            boolean activo = chkActivo.isSelected();

            if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Todos los campos son obligatorios.").show();
                return;
            }

            if (!pass.equals(confirm)) {
                new Alert(Alert.AlertType.WARNING, "Las contraseñas no coinciden.").show();
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                UsuarioDAO dao = new UsuarioDAO(conn);

                // 👇 ESTE ES EL MÉTODO CORRECTO DE TU DAO
                dao.crearUsuario(nombre, correo, rol, activo, pass);

                new Alert(Alert.AlertType.INFORMATION, "Usuario creado correctamente.").show();
                stage.close();

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error al crear usuario: " + ex.getMessage()).show();
            }
        });

        stage.setScene(new Scene(grid, 350, 350));
    }

    public void mostrar() {
        stage.showAndWait();
    }
}

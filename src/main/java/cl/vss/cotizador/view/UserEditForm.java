package cl.vss.cotizador.view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import cl.vss.cotizador.model.Usuario;
import cl.vss.cotizador.service.UsuarioDAO;
import cl.vss.cotizador.util.DBConnection;

import java.sql.Connection;

public class UserEditForm {

    private Usuario usuario;

    public UserEditForm(Usuario usuario) {
        this.usuario = usuario;
    }

    public void mostrar() {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Editar usuario");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);

        // Campos
        TextField txtNombre = new TextField(usuario.getNombre());
        TextField txtCorreo = new TextField(usuario.getCorreo());

        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("admin", "usuario");
        cbRol.setValue(usuario.getRol());

        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(usuario.isActivo());

        // Botón guardar
        Button btnGuardar = new Button("Guardar cambios");

        btnGuardar.setOnAction(e -> {
            try (Connection conn = DBConnection.getConnection()) {
                UsuarioDAO dao = new UsuarioDAO(conn);

                usuario.setNombre(txtNombre.getText());
                usuario.setCorreo(txtCorreo.getText());
                usuario.setRol(cbRol.getValue());
                usuario.setActivo(chkActivo.isSelected());

                dao.actualizarUsuario(usuario);

                window.close();

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error al actualizar usuario").show();
            }
        });

        // Layout
        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);

        grid.add(new Label("Correo:"), 0, 1);
        grid.add(txtCorreo, 1, 1);

        grid.add(new Label("Rol:"), 0, 2);
        grid.add(cbRol, 1, 2);

        grid.add(chkActivo, 1, 3);

        grid.add(btnGuardar, 1, 4);

        Scene scene = new Scene(grid, 400, 250);
        window.setScene(scene);
        window.showAndWait();
    }
}

package cl.vss.cotizador.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;

import cl.vss.cotizador.model.Usuario;
import cl.vss.cotizador.service.UsuarioDAO;
import cl.vss.cotizador.util.DBConnection;

import java.sql.Connection;
import java.util.List;

public class UserCrudView {

    private BorderPane root;
    private TableView<Usuario> tabla;
    private TextField txtBuscar;

    public UserCrudView() {
        root = new BorderPane();
        root.setPadding(new Insets(15));

        // ============================
        // TOP: Barra de búsqueda
        // ============================
        HBox top = new HBox(10);
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por nombre o correo...");
        Button btnBuscar = new Button("Buscar");
        top.getChildren().addAll(txtBuscar, btnBuscar);

        // ============================
        // CENTER: Tabla de usuarios
        // ============================
        tabla = new TableView<>();

        // 👇 Clase CSS especial para restaurar selección SOLO aquí
        tabla.getStyleClass().add("tabla-usuarios");

        // --- Columnas ---
        TableColumn<Usuario, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colId.setPrefWidth(60);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getNombre()));
        colNombre.setPrefWidth(200);

        TableColumn<Usuario, String> colCorreo = new TableColumn<>("Correo");
        colCorreo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getCorreo()));
        colCorreo.setPrefWidth(220);

        TableColumn<Usuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getRol()));
        colRol.setPrefWidth(120);

        tabla.getColumns().addAll(colId, colNombre, colCorreo, colRol);

        // ============================
        // BOTTOM: Botones CRUD
        // ============================
        HBox bottom = new HBox(10);
        Button btnCrear = new Button("Crear");
        Button btnEditar = new Button("Editar");
        Button btnEliminar = new Button("Eliminar");
        bottom.getChildren().addAll(btnCrear, btnEditar, btnEliminar);

        // Layout final
        root.setTop(top);
        root.setCenter(tabla);
        root.setBottom(bottom);

        // ============================
        // Eventos
        // ============================
        btnBuscar.setOnAction(e -> buscar());
        btnCrear.setOnAction(e -> crear());
        btnEditar.setOnAction(e -> editar());
        btnEliminar.setOnAction(e -> eliminar());

        // Cargar usuarios al iniciar
        cargarUsuarios();
    }

    public Pane getRoot() {
        return root;
    }

    // ============================
    // Cargar todos los usuarios
    // ============================
    private void cargarUsuarios() {
        try (Connection conn = DBConnection.getConnection()) {
            UsuarioDAO dao = new UsuarioDAO(conn);
            List<Usuario> lista = dao.listarUsuarios();
            tabla.getItems().setAll(lista);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ============================
    // Buscar usuarios
    // ============================
    private void buscar() {
        String filtro = txtBuscar.getText().trim().toLowerCase();

        if (filtro.isEmpty()) {
            cargarUsuarios();
            return;
        }

        tabla.getItems().removeIf(u ->
                !u.getNombre().toLowerCase().contains(filtro) &&
                !u.getCorreo().toLowerCase().contains(filtro)
        );
    }

    // ============================
    // Crear usuario
    // ============================
    private void crear() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Crear usuario");
        a.setContentText("Aquí conectaremos el formulario de creación.");
        a.showAndWait();
    }

    // ============================
    // Editar usuario
    // ============================
    private void editar() {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para editar.").show();
            return;
        }

        UserEditForm form = new UserEditForm(seleccionado);
        form.mostrar();

        cargarUsuarios();
    }

    // ============================
    // Eliminar usuario
    // ============================
    private void eliminar() {
        Usuario seleccionado = tabla.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            new Alert(Alert.AlertType.WARNING, "Selecciona un usuario para eliminar.").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("¿Eliminar usuario?");
        confirm.setContentText("Correo: " + seleccionado.getCorreo());

        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                try (Connection conn = DBConnection.getConnection()) {
                    UsuarioDAO dao = new UsuarioDAO(conn);
                    dao.eliminarUsuario(seleccionado.getId());
                    cargarUsuarios();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}

package cl.vss.cotizador.demo;

import cl.vss.cotizador.service.UsuarioDAO;
import cl.vss.cotizador.model.Usuario;
import cl.vss.cotizador.util.Sesion;
import cl.vss.cotizador.util.DBConnection;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import cl.vss.cotizador.Main;

import java.sql.Connection;
import java.sql.SQLException;

public class LoginController {
    private UsuarioDAO usuarioDAO;

    public LoginController(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public void mostrarLogin(Stage stage) {
        // Campos de entrada
        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("Correo electrónico");
        // 👨‍💻 Credenciales por defecto para desarrollo
        txtCorreo.setText("albertocarloss@gmail.com");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña");
        // 👨‍💻 Credenciales por defecto para desarrollo
        txtPassword.setText("Asm*031244");

        Button btnLogin = new Button("Ingresar");
        Label lblMensaje = new Label();

        // Acción del botón login
        btnLogin.setOnAction(e -> {
            Usuario usuario = usuarioDAO.validarLogin(txtCorreo.getText(), txtPassword.getText());
            if (usuario != null) {

                lblMensaje.setText("Bienvenido " + usuario.getNombre());
                lblMensaje.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

                Sesion.setUsuarioActual(usuario);

                new Thread(() -> {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException ignored) {}

                    javafx.application.Platform.runLater(() -> {
                        stage.close();
                        Stage mainStage = new Stage();
                        try {
                            new Main().start(mainStage);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }).start();

            } else {
                lblMensaje.setText("Usuario o contraseña incorrectos");
                lblMensaje.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        });

        // Layout principal
        VBox root = new VBox(12, txtCorreo, txtPassword, btnLogin, lblMensaje);
        root.setStyle("-fx-padding: 25; -fx-alignment: center;");

        Scene scene = new Scene(root, 480, 320);

        // ENTER en cualquier parte del login → ejecuta el botón
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                btnLogin.fire();
            }
        });

        stage.setTitle("Login - Cotizador VSS");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    // 👉 Formulario modal para crear usuario (lo usa el menú Administración)
    public void mostrarFormularioRegistro(Stage owner) {
        Stage registroStage = new Stage();
        registroStage.setTitle("Crear nuevo usuario");

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");

        TextField txtCorreo = new TextField();
        txtCorreo.setPromptText("Correo electrónico");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText("Contraseña");

        ComboBox<String> cbRol = new ComboBox<>();
        cbRol.getItems().addAll("admin", "usuario");
        cbRol.setPromptText("Rol");

        Button btnRegistrar = new Button("Registrar");
        Label lblMensaje = new Label();

        btnRegistrar.setOnAction(e -> {
            if (txtNombre.getText().isEmpty() || txtCorreo.getText().isEmpty() ||
                txtPassword.getText().isEmpty() || cbRol.getValue() == null) {
                lblMensaje.setText("❌ Debes completar todos los campos");
                lblMensaje.setStyle("-fx-text-fill: red;");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                UsuarioDAO usuarioDAO = new UsuarioDAO(conn);

                if (usuarioDAO.buscarPorCorreo(txtCorreo.getText()) != null) {
                    lblMensaje.setText("⚠️ El usuario ya existe");
                    lblMensaje.setStyle("-fx-text-fill: orange;");
                } else {
                    String hash = org.mindrot.jbcrypt.BCrypt.hashpw(
                        txtPassword.getText(), org.mindrot.jbcrypt.BCrypt.gensalt()
                    );
                    usuarioDAO.insertarUsuario(
                        txtNombre.getText(),
                        txtCorreo.getText(),
                        cbRol.getValue(),
                        hash
                    );
                    lblMensaje.setText("✅ Usuario creado correctamente");
                    lblMensaje.setStyle("-fx-text-fill: green;");
                    registroStage.close();
                }
            } catch (SQLException ex) {
                lblMensaje.setText("❌ Error SQL: " + ex.getMessage());
                lblMensaje.setStyle("-fx-text-fill: red;");
                ex.printStackTrace();
            }
        });

        VBox registroLayout = new VBox(12, txtNombre, txtCorreo, txtPassword, cbRol, btnRegistrar, lblMensaje);
        registroLayout.setStyle("-fx-padding: 25; -fx-alignment: center;");

        registroStage.setScene(new Scene(registroLayout, 420, 320));
        registroStage.setResizable(false);
        registroStage.centerOnScreen();
        registroStage.initOwner(owner);
        registroStage.showAndWait();
    }
}

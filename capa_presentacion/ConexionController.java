package presentacion;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import modelo.DatabaseConfig;
import logica.DatabaseConnection;

public class ConexionController {
    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private TextField txtDatabase;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ChoiceBox<String> cbTables;
    @FXML private Button btnTest;
    @FXML private Button btnConnect;

    private DatabaseConnection dbConn = new DatabaseConnection();

    @FXML
    public void initialize() {
        txtHost.setText("localhost");
        txtPort.setText("5432");
    }

    @FXML
    private void handleTest() {
        DatabaseConfig config = buildConfig();
        boolean ok = dbConn.connect(config);
        Alert a = new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                ok ? "Conexión OK" : "Error al conectar");
        a.showAndWait();
        if (ok) {
            String[] tables = dbConn.getTables();
            cbTables.setItems(FXCollections.observableArrayList(tables));
        }
    }

    @FXML
    private void handleConnect() {
        DatabaseConfig config = buildConfig();
        if (!dbConn.isConnected()) {
            if (!dbConn.connect(config)) {
                new Alert(Alert.AlertType.ERROR, "No se pudo conectar").showAndWait();
                return;
            }
        }
        String selected = cbTables.getValue();
        if (selected == null || selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una tabla").showAndWait();
            return;
        }
        config.setSelectedTable(selected);
        openMainView(config);
    }

    private DatabaseConfig buildConfig() {
        DatabaseConfig c = new DatabaseConfig();
        c.setHost(txtHost.getText());
        c.setPort(txtPort.getText());
        c.setDatabaseName(txtDatabase.getText());
        c.setUsername(txtUsername.getText());
        c.setPassword(txtPassword.getText());
        return c;
    }

    private void openMainView(DatabaseConfig config) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainView.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.init(dbConn, config.getSelectedTable());

            Stage stage = new Stage();
            stage.setTitle("Vista - " + config.getSelectedTable());
            stage.setScene(new Scene(root, 800, 600));
            stage.show();

            // cerrar ventana de conexión
            Stage current = (Stage) btnConnect.getScene().getWindow();
            current.close();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir vista principal: " + e.getMessage()).showAndWait();
        }
    }
}

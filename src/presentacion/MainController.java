package presentacion;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.Stage;
import javafx.stage.Modality;
import modelo.TableData;
import modelo.ColumnInfo;
import logica.DatabaseConnection;

import java.util.ArrayList;
import java.util.Optional;

public class MainController {
    @FXML private TableView<ObservableList<Object>> tableView;
    @FXML private Label lblStatus;
    @FXML private TextField txtBuscar;

    private DatabaseConnection dbConn;
    private String tableName;

    public void init(DatabaseConnection conn, String tableName) {
        this.dbConn = conn;
        this.tableName = tableName;
        loadData();
    }

    @FXML
    public void loadData() {
        TableData tableData = dbConn.loadTableData(tableName);
        tableView.getColumns().clear();
        ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();

        ArrayList<ColumnInfo> cols = tableData.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            final int colIndex = i;
            TableColumn<ObservableList<Object>, Object> col = new TableColumn<>(cols.get(i).getName());
            col.setCellValueFactory(cellData -> {
                ObservableList<Object> row = cellData.getValue();
                Object val = (colIndex < row.size()) ? row.get(colIndex) : null;
                return new SimpleObjectProperty<>(val);
            });
            tableView.getColumns().add(col);
        }

        for (ArrayList<Object> row : tableData.getRows()) {
            data.add(FXCollections.observableArrayList(row));
        }

        tableView.setItems(data);
        lblStatus.setText("Conectado - tabla: " + tableName + " - registros: " + data.size());
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleNuevo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FormularioView.fxml"));
            Parent root = loader.load();
            FormularioController fc = loader.getController();
            Stage s = new Stage();
            s.setTitle("Nuevo registro - " + tableName);
            fc.init(s, tableName, dbConn.loadTableStructure(tableName), dbConn, false, null);
            s.setScene(new Scene(root));
            s.initModality(Modality.APPLICATION_MODAL);
            s.showAndWait();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleEditar() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una fila").showAndWait();
            return;
        }

        try {
            ArrayList<Object> rowValues = new ArrayList<>(tableView.getItems().get(idx));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("FormularioView.fxml"));
            Parent root = loader.load();
            FormularioController fc = loader.getController();
            Stage s = new Stage();
            s.setTitle("Editar registro - " + tableName);
            fc.init(s, tableName, dbConn.loadTableStructure(tableName), dbConn, true, rowValues);
            s.setScene(new Scene(root));
            s.initModality(Modality.APPLICATION_MODAL);
            s.showAndWait();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al abrir formulario: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleEliminar() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una fila").showAndWait();
            return;
        }

        try {
            ArrayList<Object> rowValues = new ArrayList<>(tableView.getItems().get(idx));

            TableData td = dbConn.loadTableStructure(tableName);
            String pkCol = null;
            Object pkVal = null;
            for (int i = 0; i < td.getColumnCount(); i++) {
                if (td.getColumns().get(i).isPrimaryKey()) {
                    pkCol = td.getColumns().get(i).getName();
                    pkVal = rowValues.get(i);
                    break;
                }
            }

            if (pkCol == null) {
                new Alert(Alert.AlertType.ERROR, "Tabla sin clave primaria. Eliminación no soportada.").showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar registro con " + pkCol + " = " + pkVal + "?",
                ButtonType.OK, ButtonType.CANCEL);

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                boolean ok = dbConn.deleteRecord(tableName, pkCol, pkVal);
                if (ok) {
                    new Alert(Alert.AlertType.INFORMATION, "Registro eliminado correctamente").showAndWait();
                    loadData();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Error al eliminar el registro").showAndWait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error al eliminar: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleBuscar() {
        String q = txtBuscar.getText().trim();
        if (q.isEmpty()) {
            loadData();
            return;
        }

        // Usar la primera columna como criterio de búsqueda
        String searchColumn = dbConn.loadTableStructure(tableName).getColumns().get(0).getName();
        TableData result = dbConn.searchRecords(tableName, searchColumn, q);

        tableView.getColumns().clear();
        ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();

        ArrayList<ColumnInfo> cols = result.getColumns();
        for (int i = 0; i < cols.size(); i++) {
            final int colIndex = i;
            TableColumn<ObservableList<Object>, Object> col = new TableColumn<>(cols.get(i).getName());
            col.setCellValueFactory(cellData -> {
                ObservableList<Object> row = cellData.getValue();
                Object val = (colIndex < row.size()) ? row.get(colIndex) : null;
                return new SimpleObjectProperty<>(val);
            });
            tableView.getColumns().add(col);
        }

        for (ArrayList<Object> row : result.getRows()) {
            data.add(FXCollections.observableArrayList(row));
        }

        tableView.setItems(data);
        lblStatus.setText("Resultados de búsqueda: " + data.size() + " registros");
    }

    // NUEVO MÉTODO: Leer por clave primaria
    @FXML
    private void handleReadByPk() {
        TableData td = dbConn.loadTableStructure(tableName);
        String pkCol = null;
        for (ColumnInfo col : td.getColumns()) {
            if (col.isPrimaryKey()) {
                pkCol = col.getName();
                break;
            }
        }
        if (pkCol == null) {
            new Alert(Alert.AlertType.WARNING, "La tabla no tiene una clave primaria definida.").showAndWait();
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Leer por clave primaria");
        dialog.setHeaderText("Ingrese el valor de " + pkCol);
        dialog.setContentText("Valor:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String valStr = result.get().trim();

            // Determinar el tipo de la PK para parsear correctamente
            String pkType = null;
            for (ColumnInfo col : td.getColumns()) {
                if (col.getName().equals(pkCol)) {
                    pkType = col.getType();
                    break;
                }
            }

            Object pkValue;
            try {
                pkValue = DatabaseConnection.parseValue(valStr, pkType);
            } catch (IllegalArgumentException e) {
                new Alert(Alert.AlertType.ERROR, "Valor inválido para tipo " + pkType + ": " + e.getMessage()).showAndWait();
                return;
            }

            TableData resultData = dbConn.loadRowByPk(tableName, pkCol, pkValue);
            if (resultData.getRowCount() == 0) {
                new Alert(Alert.AlertType.INFORMATION, "No se encontró ningún registro con ese valor.").showAndWait();
                return;
            }

            // Mostrar solo la fila encontrada
            tableView.getColumns().clear();
            ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();

            ArrayList<ColumnInfo> cols = resultData.getColumns();
            for (int i = 0; i < cols.size(); i++) {
                final int colIndex = i;
                TableColumn<ObservableList<Object>, Object> col = new TableColumn<>(cols.get(i).getName());
                col.setCellValueFactory(cellData -> {
                    ObservableList<Object> row = cellData.getValue();
                    Object val = (colIndex < row.size()) ? row.get(colIndex) : null;
                    return new SimpleObjectProperty<>(val);
                });
                tableView.getColumns().add(col);
            }

            for (ArrayList<Object> row : resultData.getRows()) {
                data.add(FXCollections.observableArrayList(row));
            }

            tableView.setItems(data);
            lblStatus.setText("Resultado de búsqueda por PK: 1 registro");
        }
    }
}
package presentacion;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleObjectProperty;
import modelo.TableData;
import modelo.ColumnInfo;
import logica.DatabaseConnection;

import java.lang.classfile.Label;
import java.util.ArrayList;

import javax.swing.table.TableColumn;
import javax.swing.text.TableView;

public class MainController {
    @FXML private TableView<ObservableList<Object>> tableView;
    @FXML private Label lblStatus;

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
                Object val = null;
                if (colIndex < row.size()) val = row.get(colIndex);
                return new SimpleObjectProperty<>(val);
            });
            tableView.getColumns().add(col);
        }

        for (ArrayList<Object> row : tableData.getRows()) {
            ObservableList<Object> obsRow = FXCollections.observableArrayList(row);
            data.add(obsRow);
        }

        tableView.setItems(data);
        lblStatus.setText("Conectado - tabla: " + tableName + " - registros: " + data.size());
    }

    // (Eliminado: este bloque estaba fuera de cualquier método y causaba errores de sintaxis)

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleNuevo() throws Exception {
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
    }

    @FXML
    private void handleEditar() throws Exception {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una fila").showAndWait();
            return;
        }
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
    }

    @FXML
    private void handleEliminar() {
        int idx = tableView.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            new Alert(Alert.AlertType.WARNING, "Seleccione una fila").showAndWait();
            return;
        }
        ArrayList<Object> rowValues = new ArrayList<>(tableView.getItems().get(idx));

        // buscar PK
        String pkCol = null;
        Object pkVal = null;
        TableData td = dbConn.loadTableStructure(tableName);
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar registro con " + pkCol + " = " + pkVal + "?", ButtonType.OK, ButtonType.CANCEL);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean ok = dbConn.deleteRecord(tableName, pkCol, pkVal);
            new Alert(ok ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR, ok ? "Eliminado" : "Error al eliminar").showAndWait();
            if (ok) loadData();
        }
    }

    @FXML
    private void handleBuscar() {
        // Aquí asumimos búsqueda en la primera columna
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText("Buscar en la primera columna");
        d.setContentText("Valor:");
        d.showAndWait().ifPresent(q -> {
            TableData result = dbConn.searchRecords(tableName, dbConn.loadTableStructure(tableName).getColumns().get(0).getName(), q);
            // actualizar vista con result
            tableView.getColumns().clear();
            ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();

            ArrayList<ColumnInfo> cols = result.getColumns();
            for (int i = 0; i < cols.size(); i++) {
                final int colIndex = i;
                TableColumn<ObservableList<Object>, Object> col = new TableColumn<>(cols.get(i).getName());
                col.setCellValueFactory(cellData -> {
                    ObservableList<Object> row = cellData.getValue();
                    Object val = null;
                    if (colIndex < row.size()) val = row.get(colIndex);
                    return new SimpleObjectProperty<>(val);
                });
                tableView.getColumns().add(col);
            }

            for (ArrayList<Object> row : result.getRows()) {
                ObservableList<Object> obsRow = FXCollections.observableArrayList(row);
                data.add(obsRow);
            }

            tableView.setItems(data);
            lblStatus.setText("Resultados: " + data.size());
        });
    }
}

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modelo.ColumnInfo;
import modelo.TableData;
import logica.DatabaseConnection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FormularioController {

    @FXML private VBox fieldsContainer;

    private Stage stage;
    private String tableName;
    private TableData tableStructure;
    private DatabaseConnection dbConn;
    private boolean editMode;
    private ArrayList<Object> rowValues;
    private Map<String, Control> fieldMap = new HashMap<>();

    public void init(Stage stage, String tableName, TableData tableStructure,
                     DatabaseConnection dbConn, boolean editMode, ArrayList<Object> rowValues) {
        this.stage = stage;
        this.tableName = tableName;
        this.tableStructure = tableStructure;
        this.dbConn = dbConn;
        this.editMode = editMode;
        this.rowValues = rowValues;

        buildForm();
    }

    private void buildForm() {
        fieldsContainer.getChildren().clear();
        fieldMap.clear();

        int colIndex = 0;
        for (ColumnInfo col : tableStructure.getColumns()) {
            if (!editMode && col.isAutoIncrement()) {
                colIndex++;
                continue;
            }

            Label label = new Label(col.getName());
            label.setStyle("-fx-font-weight: bold;");

            Control input = createInputControl(col);
            fieldMap.put(col.getName(), input);

            if (editMode && rowValues != null && colIndex < rowValues.size()) {
                Object value = rowValues.get(colIndex);
                setControlValue(input, value);
                if (col.isPrimaryKey() || col.isAutoIncrement()) {
                    input.setDisable(true);
                }
            }

            VBox fieldBox = new VBox(4, label, input);
            fieldsContainer.getChildren().add(fieldBox);
            colIndex++;
        }
    }

    private Control createInputControl(ColumnInfo col) {
        String type = col.getType().toUpperCase();
        if (type.contains("INT") || type.contains("DECIMAL") || type.contains("NUMERIC") ||
            type.contains("FLOAT") || type.contains("DOUBLE")) {
            TextField tf = new TextField();
            tf.setPromptText("Número");
            return tf;
        } else if (type.contains("DATE") || type.contains("TIMESTAMP")) {
            DatePicker dp = new DatePicker();
            dp.setPromptText("AAAA-MM-DD");
            return dp;
        } else if (type.contains("BOOLEAN") || type.contains("BIT")) {
            CheckBox cb = new CheckBox();
            return cb;
        } else {
            TextField tf = new TextField();
            tf.setPromptText("Texto");
            return tf;
        }
    }

    private void setControlValue(Control control, Object value) {
        if (value == null) return;
        try {
            if (control instanceof TextField) {
                ((TextField) control).setText(value.toString());
            } else if (control instanceof DatePicker) {
                DatePicker dp = (DatePicker) control;
                if (value instanceof java.sql.Date) {
                    dp.setValue(((java.sql.Date) value).toLocalDate());
                } else if (value instanceof java.sql.Timestamp) {
                    dp.setValue(((java.sql.Timestamp) value).toLocalDateTime().toLocalDate());
                } else if (value instanceof LocalDate) {
                    dp.setValue((LocalDate) value);
                } else {
                    dp.setValue(LocalDate.parse(value.toString()));
                }
            } else if (control instanceof CheckBox) {
                ((CheckBox) control).setSelected(Boolean.parseBoolean(value.toString()));
            }
        } catch (Exception e) {
            System.err.println("Error asignando valor: " + e.getMessage());
        }
    }

    private Object getControlValue(Control control) {
        if (control instanceof TextField) {
            String text = ((TextField) control).getText().trim();
            return text.isEmpty() ? null : text;
        } else if (control instanceof DatePicker) {
            return ((DatePicker) control).getValue();
        } else if (control instanceof CheckBox) {
            return ((CheckBox) control).isSelected();
        }
        return null;
    }

    @FXML
    private void handleAccept() {
        ArrayList<String> columnas = new ArrayList<>();
        ArrayList<Object> valores = new ArrayList<>();

        for (ColumnInfo col : tableStructure.getColumns()) {
            if (!editMode && col.isAutoIncrement()) continue;
            Control control = fieldMap.get(col.getName());
            if (control == null) continue;
            columnas.add(col.getName());
            valores.add(getControlValue(control));
        }

        boolean exito;
        if (editMode) {
            String pkCol = null;
            Object pkVal = null;
            for (int i = 0; i < tableStructure.getColumnCount(); i++) {
                ColumnInfo col = tableStructure.getColumns().get(i);
                if (col.isPrimaryKey()) {
                    pkCol = col.getName();
                    pkVal = rowValues.get(i);
                    break;
            }}
            if (pkCol == null) {
                new Alert(Alert.AlertType.ERROR, "No se encontró clave primaria").showAndWait();
                return;
            }

            ArrayList<String> columnasUpdate = new ArrayList<>();
            ArrayList<Object> valoresUpdate = new ArrayList<>();
            for (int i = 0; i < columnas.size(); i++) {
                if (!columnas.get(i).equals(pkCol)) {
                    columnasUpdate.add(columnas.get(i));
                    valoresUpdate.add(valores.get(i));
                }
            }
            exito = dbConn.updateRecord(tableName, valoresUpdate, columnasUpdate, pkCol, pkVal);
        } else {
            exito = dbConn.insertRecord(tableName, valores, columnas);
        }

        if (exito) {
            new Alert(Alert.AlertType.INFORMATION, "Operación exitosa").showAndWait();
            stage.close();
        } else {
            new Alert(Alert.AlertType.ERROR, "Error en la operación").showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        stage.close();
    }
}
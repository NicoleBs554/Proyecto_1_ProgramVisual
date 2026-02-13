package modelo;

import java.util.ArrayList;

public class TableData {
    private ArrayList<ColumnInfo> columns;
    private ArrayList<ArrayList<Object>> rows;
    
    public TableData() {
        columns = new ArrayList<>();
        rows = new ArrayList<>();
    }
    
    // Métodos simples para agregar datos
    public void addColumn(ColumnInfo column) {
        columns.add(column);
    }
    
    public void addRow(ArrayList<Object> row) {
        rows.add(row);
    }
    
    public void clear() {
        columns.clear();
        rows.clear();
    }
    
    // Getters
    public ArrayList<ColumnInfo> getColumns() { return columns; }
    public ArrayList<ArrayList<Object>> getRows() { return rows; }
    
    // Obtener número de columnas y filas
    public int getColumnCount() { return columns.size(); }
    public int getRowCount() { return rows.size(); }
    
    // Obtener nombre de columna por índice
    public String getColumnName(int index) {
        if (index >= 0 && index < columns.size()) {
            return columns.get(index).getName();
        }
        return "";
    }
    
    // Obtener valor de celda
    public Object getValueAt(int row, int col) {
        if (row >= 0 && row < rows.size() && col >= 0 && col < columns.size()) {
            return rows.get(row).get(col);
        }
        return null;
    }
}
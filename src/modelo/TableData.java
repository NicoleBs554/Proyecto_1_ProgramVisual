package modelo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TableData {
    private ArrayList<ColumnInfo> columns;
    private ArrayList<ArrayList<Object>> rows;

    public TableData() {
        columns = new ArrayList<>();
        rows = new ArrayList<>();
    }

    // --- Métodos originales ---
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

    public ArrayList<ColumnInfo> getColumns() { return columns; }
    public ArrayList<ArrayList<Object>> getRows() { return rows; }

    public int getColumnCount() { return columns.size(); }
    public int getRowCount() { return rows.size(); }

    public String getColumnName(int index) {
        if (index >= 0 && index < columns.size()) {
            return columns.get(index).getName();
        }
        return "";
    }

    public Object getValueAt(int row, int col) {
        if (row >= 0 && row < rows.size() && col >= 0 && col < columns.size()) {
            return rows.get(row).get(col);
        }
        return null;
    }

    public Map<String, Integer> getColumnIndexMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            map.put(columns.get(i).getName(), i);
        }
        return map;
    }

    public ColumnInfo getPrimaryKeyColumn() {
        for (ColumnInfo col : columns) {
            if (col.isPrimaryKey()) return col;
        }
        return null;
    }

    public int getPrimaryKeyIndex() {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).isPrimaryKey()) return i;
        }
        return -1;
    }

    public String getFormattedValueAt(int row, int col) {
        Object val = getValueAt(row, col);
        return val == null ? "" : val.toString();
    }
}
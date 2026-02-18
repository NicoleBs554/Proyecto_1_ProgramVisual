package logica;

import modelo.DatabaseConfig;
import modelo.ColumnInfo;
import modelo.TableData;

import java.sql.*;
import java.util.*;

public class DatabaseConnection {
    private Connection connection;
    private final Map<String, TableData> structureCache = new HashMap<>();

    public DatabaseConnection() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL no encontrado: " + e.getMessage());
        }
    }

    public boolean connect(DatabaseConfig config) {
        try {
            String url = config.getConnectionUrl();
            connection = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            structureCache.clear();
            return true;
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public String[] getTables() {
        List<String> tables = new ArrayList<>();
        if (!isConnected()) return new String[0];
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener tablas: " + e.getMessage());
        }
        return tables.toArray(new String[0]);
    }

    public TableData loadTableStructure(String tableName) {
        if (structureCache.containsKey(tableName)) {
            return structureCache.get(tableName);
        }

        TableData tableData = new TableData();
        if (!isConnected()) return tableData;

        try {
            DatabaseMetaData meta = connection.getMetaData();
            Set<String> pkColumns = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(null, null, tableName)) {
                while (pkRs.next()) {
                    pkColumns.add(pkRs.getString("COLUMN_NAME"));
                }
            }

            try (ResultSet colRs = meta.getColumns(null, null, tableName, null)) {
                while (colRs.next()) {
                    String name = colRs.getString("COLUMN_NAME");
                    String type = colRs.getString("TYPE_NAME");
                    boolean isPk = pkColumns.contains(name);
                    boolean isAuto = false;
                    try {
                        isAuto = "YES".equalsIgnoreCase(colRs.getString("IS_AUTOINCREMENT"));
                    } catch (SQLException ignored) {}
                    if (!isAuto) {
                        String def = colRs.getString("COLUMN_DEF");
                        if (def != null && def.toLowerCase().contains("nextval")) {
                            isAuto = true;
                        }
                    }
                    tableData.addColumn(new ColumnInfo(name, type, isPk, isAuto));
                }
            }
            structureCache.put(tableName, tableData);
        } catch (SQLException e) {
            System.err.println("Error al cargar estructura: " + e.getMessage());
        }
        return tableData;
    }

    public String getColumnType(String tableName, String columnName) {
        TableData td = loadTableStructure(tableName);
        for (ColumnInfo col : td.getColumns()) {
            if (col.getName().equals(columnName)) return col.getType();
        }
        return null;
    }

    public static Object parseValue(String value, String sqlTypeName) {
        if (value == null || value.trim().isEmpty()) return null;
        String upper = sqlTypeName.toUpperCase();
        try {
            if (upper.contains("INT") || upper.contains("SERIAL")) {
                return Long.parseLong(value);
            } else if (upper.contains("BOOL")) {
                return Boolean.parseBoolean(value);
            } else if (upper.contains("UUID")) {
                return UUID.fromString(value);
            } else if (upper.contains("DATE")) {
                return java.sql.Date.valueOf(value);
            } else if (upper.contains("TIMESTAMP")) {
                return java.sql.Timestamp.valueOf(value);
            } else if (upper.contains("DECIMAL") || upper.contains("NUMERIC") || upper.contains("DOUBLE") || upper.contains("FLOAT")) {
                return new java.math.BigDecimal(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Valor inválido para tipo " + sqlTypeName + ": " + value, e);
        }
    }

    public TableData loadTableData(String tableName) {
        TableData tableData = loadTableStructure(tableName);
        if (!isConnected()) return tableData;

        String sql = "SELECT * FROM " + tableName + " LIMIT 100";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ArrayList<Object> row = new ArrayList<>();
                for (int i = 1; i <= tableData.getColumnCount(); i++) {
                    row.add(rs.getObject(i));
                }
                tableData.addRow(row);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar datos: " + e.getMessage());
        }
        return tableData;
    }

    public TableData loadRowByPk(String tableName, String pkColumn, Object pkValue) {
        TableData tableData = loadTableStructure(tableName);
        if (!isConnected()) return tableData;

        String sql = "SELECT * FROM " + tableName + " WHERE " + pkColumn + " = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setObject(1, pkValue);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ArrayList<Object> row = new ArrayList<>();
                    for (int i = 1; i <= tableData.getColumnCount(); i++) {
                        row.add(rs.getObject(i));
                    }
                    tableData.addRow(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al leer por PK: " + e.getMessage());
        }
        return tableData;
    }

    public boolean insertRecord(String tableName, ArrayList<Object> values, ArrayList<String> columns) {
        if (!isConnected()) return false;
        StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder placeholders = new StringBuilder("VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                query.append(", ");
                placeholders.append(", ");
            }
            query.append(columns.get(i));
            placeholders.append("?");
        }
        query.append(") ").append(placeholders).append(")");

        try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean updateRecord(String tableName, ArrayList<Object> values, ArrayList<String> columns,
                                String whereColumn, Object whereValue) {
        if (!isConnected()) return false;
        StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) query.append(", ");
            query.append(columns.get(i)).append(" = ?");
        }
        query.append(" WHERE ").append(whereColumn).append(" = ?");

        try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.setObject(values.size() + 1, whereValue);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRecord(String tableName, String whereColumn, Object whereValue) {
        if (!isConnected()) return false;
        String sql = "DELETE FROM " + tableName + " WHERE " + whereColumn + " = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setObject(1, whereValue);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar: " + e.getMessage());
            return false;
        }
    }

    public TableData searchRecords(String tableName, String searchColumn, String searchValue) {
        TableData tableData = loadTableStructure(tableName);
        if (!isConnected()) return tableData;

        String sql = "SELECT * FROM " + tableName + " WHERE CAST(" + searchColumn + " AS TEXT) LIKE ? LIMIT 100";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + searchValue + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ArrayList<Object> row = new ArrayList<>();
                    for (int i = 1; i <= tableData.getColumnCount(); i++) {
                        row.add(rs.getObject(i));
                    }
                    tableData.addRow(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en búsqueda: " + e.getMessage());
        }
        return tableData;
    }
}
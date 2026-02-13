package logica;

import java.util.ArrayList;
import modelo.DatabaseConfig;
import modelo.ColumnInfo;
import modelo.TableData;
import java.sql.*;

public class DatabaseConnection {
    private Connection connection;
    private DatabaseConfig config;
    
    public DatabaseConnection() {
        // Cargar driver PostgreSQL al inicio
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver PostgreSQL cargado correctamente");
        } catch (ClassNotFoundException e) {
            System.err.println("Error al cargar driver PostgreSQL: " + e.getMessage());
        }
    }
    
    // Conectar a la base de datos
    public boolean connect(DatabaseConfig config) {
        this.config = config;
        
        try {
            String url = config.getConnectionUrl();
            connection = DriverManager.getConnection(
                url, config.getUsername(), config.getPassword());
            
            System.out.println("Conexión exitosa a: " + url);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }
    
    // Obtener lista de tablas
    public String[] getTables() {
        ArrayList<String> tables = new ArrayList<>();
        
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                tables.add(tableName);
            }
            rs.close();
            
        } catch (SQLException e) {
            System.err.println("Error al obtener tablas: " + e.getMessage());
        }
        
        return tables.toArray(new String[0]);
    }
    
    // Cargar estructura de una tabla específica (ej: "materias")
    public TableData loadTableStructure(String tableName) {
        TableData tableData = new TableData();
        
        if (!ensureConnection()) return tableData;

        try {
            // Obtener información de columnas
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);

            // Obtener claves primarias
            ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, tableName);
            ArrayList<String> pkColumns = new ArrayList<>();

            while (primaryKeys.next()) {
                pkColumns.add(primaryKeys.getString("COLUMN_NAME"));
            }
            primaryKeys.close();

            // Procesar cada columna
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                boolean isPK = pkColumns.contains(columnName);

                // Intentar detectar autoincrement (driver-dependent)
                boolean isAuto = false;
                try {
                    String auto = columns.getString("IS_AUTOINCREMENT");
                    if (auto != null && auto.equalsIgnoreCase("YES")) isAuto = true;
                } catch (Exception ex) {
                    // driver no soporta ese campo; seguir sin autoincrement
                }

                tableData.addColumn(new ColumnInfo(columnName, columnType, isPK, isAuto));
            }
            columns.close();

        } catch (SQLException e) {
            System.err.println("Error al cargar estructura: " + e.getMessage());
        }
        
        return tableData;
    }

    // Helper: asegurar que la conexión está disponible
    private boolean ensureConnection() {
        if (connection == null) {
            System.err.println("No hay conexión establecida.");
            return false;
        }
        try {
            if (connection.isClosed()) {
                System.err.println("La conexión está cerrada.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error verificando conexión: " + e.getMessage());
            return false;
        }
        return true;
    }

    // Helper: obtener nombres de columnas insertables (excluye PK autoincrement)
    public ArrayList<String> getInsertableColumnNames(String tableName) {
        TableData td = loadTableStructure(tableName);
        ArrayList<String> cols = new ArrayList<>();
        for (ColumnInfo c : td.getColumns()) {
            if (c.isPrimaryKey() && c.isAutoIncrement()) continue;
            cols.add(c.getName());
        }
        return cols;
    }

    // Sobrecarga: INSERT construyendo columnas automáticamente (excluye PK autoincrement)
    public boolean insertRecord(String tableName, ArrayList<Object> values) {
        ArrayList<String> cols = getInsertableColumnNames(tableName);
        if (cols.size() != values.size()) {
            System.err.println("Número de valores no coincide con columnas insertables.");
            return false;
        }
        return insertRecord(tableName, values, cols);
    }
    
    // Cargar datos de una tabla
    public TableData loadTableData(String tableName) {
        TableData tableData = loadTableStructure(tableName);
        
        try {
            Statement stmt = connection.createStatement();
            String query = "SELECT * FROM " + tableName + " LIMIT 100";
            ResultSet rs = stmt.executeQuery(query);
            
            // Procesar cada fila
            while (rs.next()) {
                ArrayList<Object> row = new ArrayList<>();
                
                for (int i = 0; i < tableData.getColumnCount(); i++) {
                    row.add(rs.getObject(i + 1));
                }
                
                tableData.addRow(row);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error al cargar datos: " + e.getMessage());
        }
        
        return tableData;
    }
    
    // INSERT - Crear nuevo registro
    public boolean insertRecord(String tableName, ArrayList<Object> values, 
                               ArrayList<String> columnNames) {
        try {
            // Construir consulta INSERT
            StringBuilder query = new StringBuilder("INSERT INTO " + tableName + " (");
            StringBuilder placeholders = new StringBuilder("VALUES (");
            
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                    placeholders.append(", ");
                }
                query.append(columnNames.get(i));
                placeholders.append("?");
            }
            
            query.append(") ").append(placeholders).append(")");
            
            // Preparar statement
            PreparedStatement pstmt = connection.prepareStatement(query.toString());
            
            // Asignar valores
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            
            // Ejecutar
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al insertar: " + e.getMessage());
            return false;
        }
    }
    
    // UPDATE - Actualizar registro
    public boolean updateRecord(String tableName, ArrayList<Object> values, 
                               ArrayList<String> columnNames, 
                               String whereColumn, Object whereValue) {
        try {
            // Construir consulta UPDATE
            StringBuilder query = new StringBuilder("UPDATE " + tableName + " SET ");
            
            for (int i = 0; i < columnNames.size(); i++) {
                if (i > 0) {
                    query.append(", ");
                }
                query.append(columnNames.get(i)).append(" = ?");
            }
            
            query.append(" WHERE ").append(whereColumn).append(" = ?");
            
            // Preparar statement
            PreparedStatement pstmt = connection.prepareStatement(query.toString());
            
            // Asignar valores SET
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            
            // Asignar valor WHERE
            pstmt.setObject(values.size() + 1, whereValue);
            
            // Ejecutar
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al actualizar: " + e.getMessage());
            return false;
        }
    }
    
    // DELETE - Eliminar registro
    public boolean deleteRecord(String tableName, String whereColumn, Object whereValue) {
        try {
            String query = "DELETE FROM " + tableName + " WHERE " + whereColumn + " = ?";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setObject(1, whereValue);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error al eliminar: " + e.getMessage());
            return false;
        }
    }
    
    // Buscar registros (funcionalidad extra)
    public TableData searchRecords(String tableName, String searchColumn, String searchValue) {
        TableData tableData = loadTableStructure(tableName);
        
        try {
            String query = "SELECT * FROM " + tableName + 
                          " WHERE CAST(" + searchColumn + " AS TEXT) LIKE ? LIMIT 100";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, "%" + searchValue + "%");
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                ArrayList<Object> row = new ArrayList<>();
                
                for (int i = 0; i < tableData.getColumnCount(); i++) {
                    row.add(rs.getObject(i + 1));
                }
                
                tableData.addRow(row);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error en búsqueda: " + e.getMessage());
        }
        
        return tableData;
    }
    
    // Cerrar conexión
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexión cerrada");
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión: " + e.getMessage());
            }
        }
    }
    
    // Verificar si está conectado
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    // Getters
    public Connection getConnection() { return connection; }
    public DatabaseConfig getConfig() { return config; }
}
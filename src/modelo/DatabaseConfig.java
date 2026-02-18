package modelo;

public class DatabaseConfig {
    private String host;
    private String port;
    private String databaseName;
    private String username;
    private String password;
    private String selectedTable;

    public DatabaseConfig() {
        this.host = "localhost";
        this.port = "5432"; // Puerto por defecto PostgreSQL
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSelectedTable() { return selectedTable; }
    public void setSelectedTable(String selectedTable) { this.selectedTable = selectedTable; }

    public String getConnectionUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }

    /**
     * Verifica que los campos obligatorios no estén vacíos.
     */
    public boolean isValid() {
        return host != null && !host.trim().isEmpty() &&
               port != null && !port.trim().isEmpty() &&
               databaseName != null && !databaseName.trim().isEmpty() &&
               username != null && !username.trim().isEmpty();
    }

    /**
     * Crea una copia independiente de esta configuración.
     */
    public DatabaseConfig copy() {
        DatabaseConfig copy = new DatabaseConfig();
        copy.host = this.host;
        copy.port = this.port;
        copy.databaseName = this.databaseName;
        copy.username = this.username;
        copy.password = this.password;
        copy.selectedTable = this.selectedTable;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("DatabaseConfig{host='%s', port='%s', database='%s', user='%s', table='%s'}",
                host, port, databaseName, username, selectedTable);
    }
}
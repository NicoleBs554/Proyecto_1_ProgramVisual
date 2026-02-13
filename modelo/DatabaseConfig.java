package modelo;

public class DatabaseConfig {
    private String host;
    private String port;
    private String databaseName;
    private String username;
    private String password;
    private String selectedTable;
    
    // Constructor simple
    public DatabaseConfig() {
        this.host = "localhost";
        this.port = "5432"; // Puerto por defecto PostgreSQL
    }
    
    // Getters y Setters simples
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
    
    // URL para PostgreSQL
    public String getConnectionUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
    }
}
package modelo;

public class ColumnInfo {
    private String name;
    private String type; // "INTEGER", "VARCHAR", etc.
    private boolean isPrimaryKey;
    private boolean isAutoIncrement;
    
    public ColumnInfo(String name, String type, boolean isPrimaryKey) {
        this(name, type, isPrimaryKey, false);
    }

    public ColumnInfo(String name, String type, boolean isPrimaryKey, boolean isAutoIncrement) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
    }
    
    // Getters simples
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public boolean isAutoIncrement() { return isAutoIncrement; }

    public void setAutoIncrement(boolean isAutoIncrement) { this.isAutoIncrement = isAutoIncrement; }
}
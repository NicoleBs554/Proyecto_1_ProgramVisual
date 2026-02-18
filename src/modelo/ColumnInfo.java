package modelo;

public class ColumnInfo {
    private String name;
    private String type;
    private boolean isPrimaryKey;
    private boolean isAutoIncrement;
    private int columnSize;

    // Constructor simplificado (asume que no es autoincrementable)
    public ColumnInfo(String name, String type, boolean isPrimaryKey) {
        this(name, type, isPrimaryKey, false);
    }

    // Constructor completo
    public ColumnInfo(String name, String type, boolean isPrimaryKey, boolean isAutoIncrement) {
        this.name = name;
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
        this.columnSize = 0;
    }

    // --- Getters y setters originales ---
    public String getName() { return name; }
    public String getType() { return type; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public boolean isAutoIncrement() { return isAutoIncrement; }
    public void setAutoIncrement(boolean isAutoIncrement) { this.isAutoIncrement = isAutoIncrement; }

    public int getColumnSize() { return columnSize; }
    public void setColumnSize(int columnSize) { this.columnSize = columnSize; }

    // --- Nuevos métodos útiles ---

    /**
     * Devuelve la clase Java aproximada para el tipo SQL de la columna.
     * Útil para saber qué tipo de objeto esperar al leer valores.
     */
    public Class<?> getJavaClass() {
        String upper = type.toUpperCase();
        if (upper.contains("INT") || upper.contains("SERIAL")) return Long.class;
        if (upper.contains("BOOL")) return Boolean.class;
        if (upper.contains("UUID")) return java.util.UUID.class;
        if (upper.contains("DATE")) return java.sql.Date.class;
        if (upper.contains("TIMESTAMP")) return java.sql.Timestamp.class;
        if (upper.contains("DECIMAL") || upper.contains("NUMERIC")) return java.math.BigDecimal.class;
        return String.class;
    }

    /**
     * Indica si la columna es de tipo numérico (entero, decimal, etc.).
     */
    public boolean isNumeric() {
        String upper = type.toUpperCase();
        return upper.contains("INT") || upper.contains("DECIMAL") || upper.contains("NUMERIC") ||
               upper.contains("FLOAT") || upper.contains("DOUBLE") || upper.contains("SERIAL");
    }

    /**
     * Indica si la columna es de tipo fecha (DATE, TIMESTAMP, etc.).
     */
    public boolean isDateLike() {
        String upper = type.toUpperCase();
        return upper.contains("DATE") || upper.contains("TIMESTAMP");
    }
}
package presentacion;

import modelo.DatabaseConfig;
import logica.DatabaseConnection;
import modelo.TableData;
import modelo.ColumnInfo;

import java.util.ArrayList;
import java.util.Scanner;

public class ConsoleApp {
    private static DatabaseConnection db;
    private static DatabaseConfig cfg;
    private static Scanner sc = new Scanner(System.in);
    private static String currentTable = null;

    public static void main(String[] args) {
        db = new DatabaseConnection();
        cfg = new DatabaseConfig();

        System.out.println("Aplicación de consola - gestor simple de PostgreSQL");

        while (true) {
            System.out.println("\nMenú:\n1) Configurar conexión\n2) Conectar\n3) Listar tablas\n4) Seleccionar tabla\n5) Mostrar tabla\n6) Insertar registro\n7) Actualizar registro\n8) Eliminar registro\n9) Buscar\n0) Salir");
            System.out.print("Elige opción: ");
            String opt = sc.nextLine().trim();
            try {
                switch (opt) {
                    case "1": configMenu(); break;
                    case "2": connect(); break;
                    case "3": listTables(); break;
                    case "4": selectTable(); break;
                    case "5": showTable(); break;
                    case "6": insertRecord(); break;
                    case "7": updateRecord(); break;
                    case "8": deleteRecord(); break;
                    case "9": search(); break;
                    case "0": System.out.println("Saliendo..."); db.disconnect(); return;
                    default: System.out.println("Opción inválida");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private static void configMenu() {
        System.out.print("Host (actual " + cfg.getHost() + "): ");
        String h = sc.nextLine().trim(); if (!h.isEmpty()) cfg.setHost(h);
        System.out.print("Puerto (actual " + cfg.getPort() + "): ");
        String p = sc.nextLine().trim(); if (!p.isEmpty()) cfg.setPort(p);
        System.out.print("Nombre BD: "); cfg.setDatabaseName(sc.nextLine().trim());
        System.out.print("Usuario: "); cfg.setUsername(sc.nextLine().trim());
        System.out.print("Password: "); cfg.setPassword(sc.nextLine().trim());
    }

    private static void connect() {
        if (db.connect(cfg)) System.out.println("Conectado a " + cfg.getConnectionUrl());
        else System.out.println("Fallo al conectar");
    }

    private static void listTables() {
        String[] tables = db.getTables();
        System.out.println("Tablas:\n");
        for (int i = 0; i < tables.length; i++) System.out.println((i+1) + ") " + tables[i]);
    }

    private static void selectTable() {
        System.out.print("Nombre de la tabla a seleccionar: ");
        String t = sc.nextLine().trim();
        if (t.isEmpty()) { System.out.println("Nombre vacío"); return; }
        currentTable = t; cfg.setSelectedTable(t);
        System.out.println("Tabla seleccionada: " + currentTable);
    }

    private static void showTable() {
        if (!checkTable()) return;
        TableData td = db.loadTableData(currentTable);
        printTableData(td);
    }

    private static void insertRecord() {
        if (!checkTable()) return;
        ArrayList<String> cols = db.getInsertableColumnNames(currentTable);
        ArrayList<Object> values = new ArrayList<>();
        System.out.println("Insertar en: " + currentTable);
        for (String c : cols) {
            System.out.print(c + ": ");
            values.add(sc.nextLine());
        }
        boolean ok = db.insertRecord(currentTable, values);
        System.out.println(ok ? "Insertado correctamente" : "Error al insertar");
    }

    private static void updateRecord() {
        if (!checkTable()) return;
        TableData td = db.loadTableData(currentTable);
        printTableData(td);
        System.out.print("Índice de fila a actualizar (1.." + td.getRowCount() + "): ");
        int idx = Integer.parseInt(sc.nextLine()) - 1;
        if (idx < 0 || idx >= td.getRowCount()) { System.out.println("Índice inválido"); return; }

        // preparar valores para actualizar: usar columnas insertables
        ArrayList<String> cols = db.getInsertableColumnNames(currentTable);
        ArrayList<Object> values = new ArrayList<>();
        for (String c : cols) {
            System.out.print(c + " (nuevo, vacío=sin cambio): ");
            String v = sc.nextLine();
            values.add(v.isEmpty() ? null : v);
        }

        // obtener PK
        String pk = null; Object pkVal = null;
        for (int i = 0; i < td.getColumnCount(); i++) {
            ColumnInfo ci = td.getColumns().get(i);
            if (ci.isPrimaryKey()) { pk = ci.getName(); pkVal = td.getValueAt(idx, i); break; }
        }
        if (pk == null) { System.out.println("No hay PK definida"); return; }

        // limpiar valores null -> no enviar; construir listas
        ArrayList<String> sendCols = new ArrayList<>();
        ArrayList<Object> sendVals = new ArrayList<>();
        for (int i = 0; i < cols.size(); i++) {
            Object v = values.get(i);
            if (v != null) { sendCols.add(cols.get(i)); sendVals.add(v); }
        }
        if (sendCols.isEmpty()) { System.out.println("Sin cambios"); return; }

        boolean ok = db.updateRecord(currentTable, sendVals, sendCols, pk, pkVal);
        System.out.println(ok ? "Actualizado" : "Error al actualizar");
    }

    private static void deleteRecord() {
        if (!checkTable()) return;
        TableData td = db.loadTableData(currentTable);
        printTableData(td);
        System.out.print("Índice de fila a eliminar (1.." + td.getRowCount() + "): ");
        int idx = Integer.parseInt(sc.nextLine()) - 1;
        if (idx < 0 || idx >= td.getRowCount()) { System.out.println("Índice inválido"); return; }

        String pk = null; Object pkVal = null;
        for (int i = 0; i < td.getColumnCount(); i++) {
            ColumnInfo ci = td.getColumns().get(i);
            if (ci.isPrimaryKey()) { pk = ci.getName(); pkVal = td.getValueAt(idx, i); break; }
        }
        if (pk == null) { System.out.println("No hay PK definida"); return; }

        System.out.print("Confirmar eliminar fila " + (idx+1) + "? (s/n): ");
        String r = sc.nextLine().trim().toLowerCase();
        if (!r.equals("s")) { System.out.println("Cancelado"); return; }

        boolean ok = db.deleteRecord(currentTable, pk, pkVal);
        System.out.println(ok ? "Eliminado" : "Error al eliminar");
    }

    private static void search() {
        if (!checkTable()) return;
        System.out.print("Termino de búsqueda: ");
        String q = sc.nextLine();
        TableData td = db.searchRecords(currentTable, db.loadTableStructure(currentTable).getColumns().get(0).getName(), q);
        printTableData(td);
    }

    private static boolean checkTable() {
        if (currentTable == null) { System.out.println("Seleccione primero una tabla"); return false; }
        if (!db.isConnected()) { System.out.println("No conectado"); return false; }
        return true;
    }

    private static void printTableData(TableData td) {
        // cabeceras
        for (int i = 0; i < td.getColumnCount(); i++) {
            System.out.print(td.getColumnName(i) + "\t");
        }
        System.out.println();
        // filas
        for (int r = 0; r < td.getRowCount(); r++) {
            for (int c = 0; c < td.getColumnCount(); c++) {
                Object v = td.getValueAt(r, c);
                System.out.print((v==null?"null":v.toString()) + "\t");
            }
            System.out.println();
        }
        System.out.println("Registros: " + td.getRowCount());
    }
}

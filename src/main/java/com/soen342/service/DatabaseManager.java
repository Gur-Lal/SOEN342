package com.soen342.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager handles SQLite database initialization and table creation
 * for the EU Rail Trip Planner application.
 */
public class DatabaseManager {
    
    private static final String DB_URL = "jdbc:sqlite:railway.db";
    private Connection connection;
    
    /**
     * Constructor - establishes database connection
     */
    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Database connection established.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
        }
    }
    
    /**
     * Initializes all database tables
     */
    public void initializeTables() {
        createConnectionsTable();
        createTripsTable();
        createClientsTable();
        createBookingsTable();
        createReservationsTable();
        createTicketsTable();
        System.out.println("All tables initialized successfully.");
    }
    
    /**
     * Creates the Connections table
     * Stores rail connection information loaded from CSV
     */
    private void createConnectionsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS connections (
                route_id TEXT PRIMARY KEY,
                departure_city TEXT NOT NULL,
                arrival_city TEXT NOT NULL,
                departure_time TEXT NOT NULL,
                arrival_time TEXT NOT NULL,
                train_type TEXT NOT NULL,
                days_of_operation TEXT NOT NULL,
                first_class_rate REAL NOT NULL,
                second_class_rate REAL NOT NULL
            )
        """;
        
        executeSQL(sql, "Connections table");
    }
    
    /**
     * Creates the Trips table
     * Stores trip information with references to connections
     * Connection list stored as comma-separated route IDs (e.g., "R001,R002,R003")
     */
    private void createTripsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS trips (
                trip_id TEXT PRIMARY KEY,
                total_time TEXT NOT NULL,
                total_fc_rate REAL NOT NULL,
                total_sc_rate REAL NOT NULL,
                connection1_id TEXT NOT NULL,
                connection2_id TEXT,
                connection3_id TEXT,
                FOREIGN KEY (connection1_id) REFERENCES connections(route_id),
                FOREIGN KEY (connection2_id) REFERENCES connections(route_id),
                FOREIGN KEY (connection3_id) REFERENCES connections(route_id)
            )
        """;
        
        executeSQL(sql, "Trips table");
    }
    
    /**
     * Creates the Clients table
     * Stores customer/passenger information
     */
    private void createClientsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS clients (
                client_pk INTEGER PRIMARY KEY AUTOINCREMENT,
                license_id TEXT NOT NULL UNIQUE,
                first_name TEXT NOT NULL,
                last_name TEXT NOT NULL,
                age INTEGER NOT NULL
            )
        """;
        
        executeSQL(sql, "Clients table");
    }
    
    /**
     * Creates the Bookings table
     * Stores booking information with reference to trip
     */
    private void createBookingsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id TEXT PRIMARY KEY,
                trip_id TEXT NOT NULL,
                FOREIGN KEY (trip_id) REFERENCES trips(trip_id)
            )
        """;
        
        executeSQL(sql, "Bookings table");
    }
    
    /**
     * Creates the Reservations table
     * Links clients to trips through bookings
     */
    private void createReservationsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reservations (
                reservation_id TEXT PRIMARY KEY,
                client_pk INTEGER NOT NULL,
                trip_id TEXT NOT NULL,
                booking_id TEXT NOT NULL,
                FOREIGN KEY (client_pk) REFERENCES clients(client_pk),
                FOREIGN KEY (trip_id) REFERENCES trips(trip_id),
                FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
            )
        """;
        
        executeSQL(sql, "Reservations table");
    }
    
    /**
     * Creates the Tickets table
     * Stores ticket information linked to reservations and clients
     */
    private void createTicketsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS tickets (
                ticket_id TEXT PRIMARY KEY,
                reservation_id TEXT NOT NULL,
                client_pk INTEGER NOT NULL,
                FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id),
                FOREIGN KEY (client_pk) REFERENCES clients(client_pk)
            )
        """;
        
        executeSQL(sql, "Tickets table");
    }
    
    /**
     * Helper method to execute SQL statements with error handling
     */
    private void executeSQL(String sql, String tableName) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println(tableName + " created successfully.");
        } catch (SQLException e) {
            System.err.println("Error creating " + tableName + ": " + e.getMessage());
        }
    }
    
    /**
     * Drops all tables (useful for reset/testing)
     */
    public void dropAllTables() {
        String[] tables = {"tickets", "reservations", "bookings", "clients", "trips", "connections"};
        
        try (Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                stmt.execute("DROP TABLE IF EXISTS " + table);
                System.out.println(table + " table dropped.");
            }
            System.out.println("All tables dropped successfully.");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }
    
    /**
     * Closes the database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Gets the active database connection
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * Gets the maximum counter value from a table's ID column
     * Extracts the numeric part from IDs like "BOOK0001", "TRIP0023", etc.
     */
    public int getMaxCounter(String tableName, String idColumn, String prefix) {
        String sql = String.format("SELECT MAX(CAST(SUBSTR(%s, %d) AS INTEGER)) FROM %s", 
                                   idColumn, prefix.length() + 1, tableName);
        
        try (Statement stmt = connection.createStatement();
             var rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int maxValue = rs.getInt(1);
                return maxValue; // Returns 0 if no records exist
            }
        } catch (SQLException e) {
            System.err.println("Error getting max counter for " + tableName + ": " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Gets the next counter value for bookings
     */
    public int getNextBookingCounter() {
        return getMaxCounter("bookings", "booking_id", "BOOK");
    }
    
    /**
     * Gets the next counter value for trips
     */
    public int getNextTripCounter() {
        return getMaxCounter("trips", "trip_id", "TRIP");
    }
    
    /**
     * Gets the next counter value for reservations
     */
    public int getNextReservationCounter() {
        return getMaxCounter("reservations", "reservation_id", "RES");
    }
    
    /**
     * Gets the next counter value for tickets
     */
    public int getNextTicketCounter() {
        return getMaxCounter("tickets", "ticket_id", "TICK");
    }
    
    /**
     * Initializes all static counters in domain classes from database
     * Call this method at application startup after creating DatabaseManager
     */
    public void initializeCounters() {
        // Note: This method would need to be called from App.java
        // and the domain classes need static setter methods for their counters
        System.out.println("Initializing counters from database...");
        System.out.println("  Booking counter: " + getNextBookingCounter());
        System.out.println("  Trip counter: " + getNextTripCounter());
        System.out.println("  Reservation counter: " + getNextReservationCounter());
        System.out.println("  Ticket counter: " + getNextTicketCounter());
    }
    
    /**
     * Main method for testing table creation
     */
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        
        // Optional: Drop existing tables for fresh start
        // dbManager.dropAllTables();
        
        // Create all tables
        dbManager.initializeTables();
        
        // Initialize counters from existing data
        dbManager.initializeCounters();
        
        // Close connection when done
        dbManager.closeConnection();
    }
}
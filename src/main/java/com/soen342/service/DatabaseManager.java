package com.soen342.service;
import java.sql.*;

public final class DatabaseManager {
    private static final String JDBC_URL = "jdbc:sqlite:train_system.db"; // SQLite DB file

    private DatabaseManager() {}

    
    public static void init() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON"); 

            // UPDATED Connection table (now matches CSV + domain model)
            s.execute("""
                CREATE TABLE IF NOT EXISTS Connection (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    routeID TEXT,
                    departureCity TEXT NOT NULL,
                    arrivalCity TEXT NOT NULL,
                    departureTime TEXT,
                    arrivalTime TEXT,
                    trainType TEXT,
                    daysOfOperation TEXT,
                    firstClassRate REAL,
                    secondClassRate REAL
                );
            """);

            
            s.execute("""
                CREATE TABLE IF NOT EXISTS Client (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    age INTEGER,
                    identifier TEXT UNIQUE
                );
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS Trip (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clientId INTEGER,
                    totalPrice REAL,
                    FOREIGN KEY (clientId) REFERENCES Client(id)
                );
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS Reservation (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tripId INTEGER,
                    connectionId INTEGER,
                    FOREIGN KEY (tripId) REFERENCES Trip(id),
                    FOREIGN KEY (connectionId) REFERENCES Connection(id)
                );
            """);

            s.execute("""
                CREATE TABLE IF NOT EXISTS Ticket (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    reservationId INTEGER,
                    code TEXT UNIQUE,
                    FOREIGN KEY (reservationId) REFERENCES Reservation(id)
                );
            """);

            System.out.println("Database schema verified/created successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}

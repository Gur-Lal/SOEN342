package com.soen342.service;

import java.sql.*;

public final class DatabaseManager {
    private static final String JDBC_URL = "jdbc:sqlite:train_system.db";

    private DatabaseManager() {}

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public static void init() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");

            // CITIES TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS Cities (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL
                );
            """);

            //  TRAINS TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS Trains (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    type TEXT UNIQUE NOT NULL
                );
            """);

            // CLIENT TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS Client (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    firstName TEXT NOT NULL,
                    lastName TEXT NOT NULL,
                    age INTEGER
                );
            """);

            // CONNECTIONS TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS Connections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    routeId TEXT,
                    departureCityId INTEGER NOT NULL,
                    arrivalCityId INTEGER NOT NULL,
                    departureTime TEXT,
                    arrivalTime TEXT,
                    trainId INTEGER NOT NULL,
                    daysOfOperation TEXT,
                    firstClassPrice REAL,
                    secondClassPrice REAL,
                    isNextDay INTEGER DEFAULT 0,
                    FOREIGN KEY (departureCityId) REFERENCES Cities(id),
                    FOREIGN KEY (arrivalCityId) REFERENCES Cities(id),
                    FOREIGN KEY (trainId) REFERENCES Trains(id)
                );
            """);

            // BOOKED TRIPS TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS BookedTrips (
                    bookedTripId INTEGER PRIMARY KEY AUTOINCREMENT,
                    tripID TEXT UNIQUE,
                    bookingDate TEXT,
                    departureDate TEXT,
                    arrivalDate TEXT,
                    isFirstClass INTEGER DEFAULT 0
                );
            """);

            // RESERVATIONS TABLE
            s.execute("""
                CREATE TABLE IF NOT EXISTS Reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tripId INTEGER NOT NULL,
                    clientId INTEGER NOT NULL,
                    connectionRouteId INTEGER NOT NULL,
                    isFirstClass INTEGER DEFAULT 0,
                    FOREIGN KEY (tripId) REFERENCES BookedTrips(bookedTripId),
                    FOREIGN KEY (clientId) REFERENCES Client(id),
                    FOREIGN KEY (connectionRouteId) REFERENCES Connections(id)
                );
            """);

            //POUR DEBUGGING

           // System.out.println("Database schema created");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
}

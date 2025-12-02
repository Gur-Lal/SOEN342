package com.soen342.service;

import com.soen342.domain.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object for handling booking-related database operations
 */
public class BookingDAO {
    
    private Connection connection;
    
    public BookingDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Saves a complete booking with all its reservations, clients, tickets, and trip
     * This is a transactional operation - either everything saves or nothing saves
     */
    public boolean saveBooking(Booking booking) {
        try {
            connection.setAutoCommit(false); // Start transaction
            
            // 1. Save the trip if it doesn't exist
            saveTripIfNotExists(booking.getTrip());
            
            // 2. Save the booking
            saveBookingRecord(booking);
            
            // 3. Save all reservations with their clients and tickets
            for (Reservation reservation : booking.getReservations()) {
                saveClient(reservation.getClient());
                saveReservation(reservation, booking.getBookingID());
                saveTicket(reservation.getTicket(), reservation.getClient());
            }
            
            connection.commit(); // Commit transaction
            System.out.println("Booking saved successfully to database!");
            return true;
            
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback on error
                System.err.println("Error saving booking, rolled back: " + e.getMessage());
            } catch (SQLException ex) {
                System.err.println("Error rolling back: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Reset to default
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
        }
    }
    
    /**
     * Saves a trip to the database (only if it doesn't already exist)
     */
    private void saveTripIfNotExists(Trip trip) throws SQLException {
        // Check if trip already exists
        String checkSql = "SELECT trip_id FROM trips WHERE trip_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setString(1, trip.getTripID());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return; // Trip already exists, skip
            }
        }
        
        // Save connections first (they should already be in DB from CSV load)
        // If not, you could add saveConnection() method here
        
        // Insert trip
        String sql = """
            INSERT INTO trips (trip_id, total_time, total_fc_rate, total_sc_rate, 
                              connection1_id, connection2_id, connection3_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, trip.getTripID());
            pstmt.setString(2, trip.getTotalTime().toString());
            pstmt.setDouble(3, trip.getTotalFCRate());
            pstmt.setDouble(4, trip.getTotalSCRate());
            
            List<com.soen342.domain.Connection> connections = trip.getConnections();
            pstmt.setString(5, connections.size() > 0 ? connections.get(0).getRouteID() : null);
            pstmt.setString(6, connections.size() > 1 ? connections.get(1).getRouteID() : null);
            pstmt.setString(7, connections.size() > 2 ? connections.get(2).getRouteID() : null);
            
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Saves a booking record
     */
    private void saveBookingRecord(Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (booking_id, trip_id) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, booking.getBookingID());
            pstmt.setString(2, booking.getTrip().getTripID());
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Saves or updates a client
     * If license_id already exists, retrieves the existing client_pk
     */
    private Integer saveClient(Client client) throws SQLException {
        // Check if client already exists
        String checkSql = "SELECT client_pk FROM clients WHERE license_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setString(1, client.getId());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("client_pk"); // Return existing PK
            }
        }
        
        // Insert new client
        String sql = "INSERT INTO clients (license_id, first_name, last_name, age) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, client.getId());
            pstmt.setString(2, client.getFirstName());
            pstmt.setString(3, client.getLastName());
            pstmt.setInt(4, client.getAge());
            pstmt.executeUpdate();
            
            // Get the auto-generated client_pk
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return null;
    }
    
    /**
     * Saves a reservation
     */
    private void saveReservation(Reservation reservation, String bookingID) throws SQLException {
        // Get the client's PK
        Integer clientPk = getClientPk(reservation.getClient().getId());
        
        String sql = """
            INSERT INTO reservations (reservation_id, client_pk, trip_id, booking_id)
            VALUES (?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reservation.getReservationID());
            pstmt.setInt(2, clientPk);
            pstmt.setString(3, reservation.getTrip().getTripID());
            pstmt.setString(4, bookingID);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Saves a ticket
     */
    private void saveTicket(Ticket ticket, Client client) throws SQLException {
        // Get the client's PK
        Integer clientPk = getClientPk(client.getId());
        
        String sql = """
            INSERT INTO tickets (ticket_id, reservation_id, client_pk)
            VALUES (?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ticket.getTicketID());
            pstmt.setString(2, ticket.getReservation().getReservationID());
            pstmt.setInt(3, clientPk);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Helper method to get client_pk from license_id
     */
    private Integer getClientPk(String licenseId) throws SQLException {
        String sql = "SELECT client_pk FROM clients WHERE license_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, licenseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("client_pk");
            }
        }
        throw new SQLException("Client not found with license_id: " + licenseId);
    }
}
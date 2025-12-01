package com.soen342.service;

import com.soen342.domain.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

/**
 * Data Access Object for retrieving booking information from database
 */
public class RetrievalDAO {
    
    private Connection connection;
    
    public RetrievalDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Retrieves all reservations for a client by name and license ID
     * Returns them as Reservation objects in working memory
     */
    public List<Reservation> getClientReservations(String lastName, String licenseId) {
        List<Reservation> reservations = new ArrayList<>();
        
        String sql = """
            SELECT r.reservation_id, r.trip_id, c.client_pk, c.license_id, 
                c.first_name, c.last_name, c.age, t.ticket_id
            FROM reservations r
            JOIN clients c ON r.client_pk = c.client_pk
            JOIN tickets t ON r.reservation_id = t.reservation_id
            WHERE c.license_id = ? AND c.last_name LIKE ?
            ORDER BY r.reservation_id
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, licenseId);
            pstmt.setString(2, "%" + lastName + "%");
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String reservationID = rs.getString("reservation_id");
                String tripID = rs.getString("trip_id");
                String firstName = rs.getString("first_name");
                String LName = rs.getString("last_name");
                int clientAge = rs.getInt("age");
                String clientLicenseId = rs.getString("license_id");
                String ticketID = rs.getString("ticket_id");
                
                // Reconstruct objects in memory
                Client client = new Client(firstName, LName, clientAge, clientLicenseId);
                Trip trip = loadTripByID(tripID);
                
                if (trip != null) {
                    // Create reservation object (without calling constructor that increments counter)
                    Reservation reservation = reconstructReservation(reservationID, client, trip, ticketID);
                    reservations.add(reservation);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error retrieving reservations: " + e.getMessage());
        }
        
        return reservations;
    }
    
    /**
     * Separates reservations into current/future and past trips
     * Returns a ClientTripsResult object containing both lists
     */
    public ClientTripsResult getClientTripsGrouped(String lastName, String licenseId) {
        List<Reservation> allReservations = getClientReservations(lastName, licenseId);
        List<Reservation> currentTrips = new ArrayList<>();
        List<Reservation> pastTrips = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        
        for (Reservation reservation : allReservations) {
            if (isTripInFuture(reservation.getTrip(), today)) {
                currentTrips.add(reservation);
            } else {
                pastTrips.add(reservation);
            }
        }
        
        return new ClientTripsResult(currentTrips, pastTrips);
    }
    
    /**
     * Determines if a trip is current/future based on its first connection's departure
     * For simplicity, we check if today matches the days of operation
     * In a real system, you'd store actual booking dates
     */
    private boolean isTripInFuture(Trip trip, LocalDate today) {
        // Simple heuristic: check if the trip's day of operation includes today or future days
        // In a production system, you'd store the actual travel date with the booking
        
        List<com.soen342.domain.Connection> connections = trip.getConnections();
        if (connections == null || connections.isEmpty()) return false;
        
        com.soen342.domain.Connection firstConn = connections.get(0);
        if (firstConn == null || firstConn.getParameters() == null) return false;
        
        String daysOfOperation = firstConn.getParameters().getDaysOfOperation();
        if (daysOfOperation == null) return false;
        
        String todayName = today.getDayOfWeek().toString().substring(0, 3);
        
        // If days contains today or if it's "Daily", consider it current
        return daysOfOperation.contains(todayName) || daysOfOperation.contains("Daily");
    }
    
    /**
     * Loads a Trip from database by ID and reconstructs it in memory
     */
    private Trip loadTripByID(String tripID) {
        String sql = """
            SELECT trip_id, total_time, total_fc_rate, total_sc_rate,
                   connection1_id, connection2_id, connection3_id
            FROM trips
            WHERE trip_id = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tripID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Time totalTime = Time.valueOf(rs.getString("total_time"));
                double totalFCRate = rs.getDouble("total_fc_rate");
                double totalSCRate = rs.getDouble("total_sc_rate");
                
                // Load connections
                List<com.soen342.domain.Connection> connections = new ArrayList<>();
                
                String conn1ID = rs.getString("connection1_id");
                String conn2ID = rs.getString("connection2_id");
                String conn3ID = rs.getString("connection3_id");
                
                if (conn1ID != null) connections.add(loadConnectionByRouteID(conn1ID));
                if (conn2ID != null) connections.add(loadConnectionByRouteID(conn2ID));
                if (conn3ID != null) connections.add(loadConnectionByRouteID(conn3ID));
                
                // Reconstruct Trip without incrementing counter
                return reconstructTrip(tripID, totalTime, totalFCRate, totalSCRate, connections);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading trip: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Loads a Connection from database by route ID
     */
    private com.soen342.domain.Connection loadConnectionByRouteID(String routeID) {
        if (routeID == null) return null;
        
        String sql = """
            SELECT route_id, departure_city, arrival_city, departure_time, arrival_time,
                   train_type, days_of_operation, first_class_rate, second_class_rate
            FROM connections
            WHERE route_id = ?
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, routeID);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Parameters params = new Parameters(
                    rs.getString("departure_city"),
                    rs.getString("arrival_city"),
                    Time.valueOf(rs.getString("departure_time")),
                    Time.valueOf(rs.getString("arrival_time")),
                    rs.getString("train_type"),
                    rs.getString("days_of_operation"),
                    rs.getDouble("first_class_rate"),
                    rs.getDouble("second_class_rate")
                );
                
                return new com.soen342.domain.Connection(routeID, params);
            } else {
                System.err.println("Warning: Connection not found in database: " + routeID);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading connection: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Reconstructs a Trip object without incrementing the static counter
     * Uses reflection to set the tripID directly
     */
    private Trip reconstructTrip(String tripID, Time totalTime, double totalFCRate, 
                                 double totalSCRate, List<com.soen342.domain.Connection> connections) {
        // Create trip normally (this will increment counter and assign new ID)
        Trip trip = new Trip(totalTime, totalFCRate, totalSCRate, connections);
        
        // Override the ID using reflection to restore the database ID
        try {
            java.lang.reflect.Field tripIDField = Trip.class.getDeclaredField("tripID");
            tripIDField.setAccessible(true);
            tripIDField.set(trip, tripID);
        } catch (Exception e) {
            System.err.println("Warning: Could not restore trip ID: " + e.getMessage());
        }
        
        return trip;
    }
    
    /**
     * Reconstructs a Reservation object without incrementing the static counter
     */
    private Reservation reconstructReservation(String reservationID, Client client, 
                                              Trip trip, String ticketID) {
        // Create reservation normally
        Reservation reservation = new Reservation(client, trip);
        
        // Override the IDs using reflection
        try {
            java.lang.reflect.Field resIDField = Reservation.class.getDeclaredField("reservationID");
            resIDField.setAccessible(true);
            resIDField.set(reservation, reservationID);
            
            // Also fix the ticket ID
            Ticket ticket = reservation.getTicket();
            java.lang.reflect.Field ticketIDField = Ticket.class.getDeclaredField("ticketID");
            ticketIDField.setAccessible(true);
            ticketIDField.set(ticket, ticketID);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not restore reservation/ticket IDs: " + e.getMessage());
        }
        
        return reservation;
    }
    
    /**
     * Inner class to hold the result of grouped trips
     */
    public static class ClientTripsResult {
        private List<Reservation> currentTrips;
        private List<Reservation> pastTrips;
        
        public ClientTripsResult(List<Reservation> currentTrips, List<Reservation> pastTrips) {
            this.currentTrips = currentTrips;
            this.pastTrips = pastTrips;
        }
        
        public List<Reservation> getCurrentTrips() {
            return currentTrips;
        }
        
        public List<Reservation> getPastTrips() {
            return pastTrips;
        }
        
        public boolean hasCurrentTrips() {
            return !currentTrips.isEmpty();
        }
        
        public boolean hasPastTrips() {
            return !pastTrips.isEmpty();
        }
    }
}
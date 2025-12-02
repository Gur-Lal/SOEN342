package com.soen342.service;

import com.soen342.domain.Connection;
import com.soen342.domain.Parameters;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for managing connections in the database
 */
public class ConnectionDAO {
    
    private java.sql.Connection connection;
    
    public ConnectionDAO(java.sql.Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Saves a connection to the database (only if it doesn't already exist)
     */
    public void saveConnection(Connection conn) {
        // Check if connection already exists
        if (connectionExists(conn.getRouteID())) {
            return; // Already exists, skip
        }
        
        String sql = """
            INSERT INTO connections (route_id, departure_city, arrival_city, 
                                    departure_time, arrival_time, train_type, 
                                    days_of_operation, first_class_rate, second_class_rate)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            Parameters params = conn.getParameters();
            
            pstmt.setString(1, conn.getRouteID());
            pstmt.setString(2, params.getDepartureCity());
            pstmt.setString(3, params.getArrivalCity());
            pstmt.setString(4, params.getDepartureTime().toString());
            pstmt.setString(5, params.getArrivalTime().toString());
            pstmt.setString(6, params.getTrainType());
            pstmt.setString(7, params.getDaysOfOperation());
            pstmt.setDouble(8, params.getFirstClassRate());
            pstmt.setDouble(9, params.getSecondClassRate());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error saving connection " + conn.getRouteID() + ": " + e.getMessage());
        }
    }
    
    /**
     * Checks if a connection already exists in the database
     */
    private boolean connectionExists(String routeID) {
        String sql = "SELECT route_id FROM connections WHERE route_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, routeID);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("Error checking connection existence: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets count of connections in database
     */
    public int getConnectionCount() {
        String sql = "SELECT COUNT(*) FROM connections";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting connection count: " + e.getMessage());
        }
        
        return 0;
    }
}
package com.soen342.service;
import com.soen342.domain.Booking;
import com.soen342.domain.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingCatalog {
    private List<Booking> bookings;

    public BookingCatalog() {
        this.bookings = new ArrayList<>();
    }

    public void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public void removeBooking(Booking booking) {
        bookings.remove(booking);
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public Booking getBookingByID(String bookingID) {
        for (Booking booking : bookings) {
            if (booking.getBookingID().equals(bookingID)) {
                return booking;
            }
        }
        return null;
    }

     public void saveBookingToDatabase(Booking booking) {
        try (Connection conn = DatabaseManager.getConnection()) {

            // --- Step 1: Insert or find client ---
            int clientId;
            String findClientSql = "SELECT id FROM Client WHERE identifier = ?";
            try (PreparedStatement psFind = conn.prepareStatement(findClientSql)) {
                psFind.setString(1, booking.getClient().getIdentifier());
                ResultSet rs = psFind.executeQuery();
                if (rs.next()) {
                    clientId = rs.getInt("id");
                } else {
                    String insertClient = "INSERT INTO Client(name, age, identifier) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertClient, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, booking.getClient().getName());
                        ps.setInt(2, booking.getClient().getAge());
                        ps.setString(3, booking.getClient().getIdentifier());
                        ps.executeUpdate();
                        ResultSet keys = ps.getGeneratedKeys();
                        keys.next();
                        clientId = keys.getInt(1);
                    }
                }
            }

            // --- Step 2: Insert trip ---
            int tripId;
            String insertTrip = "INSERT INTO Trip(clientId, totalPrice) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertTrip, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, clientId);
                ps.setDouble(2, booking.getTrip().getTotalSCRate()); // or totalFCRate
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                tripId = keys.getInt(1);
            }

            // --- Step 3: Insert reservations ---
            for (Reservation r : booking.getReservations()) {
                for (com.soen342.domain.Connection connection : booking.getTrip().getConnections()) {
                    String insertReservation = "INSERT INTO Reservation(tripId, connectionId) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertReservation)) {
                        ps.setInt(1, tripId);
                        ps.setInt(2, 0); // placeholder — your Connection table not yet populated
                        ps.executeUpdate();
                    }
                }
            }

            System.out.println("Booking saved to database for client: " + booking.getClient().getName());

        } catch (SQLException e) {
            System.err.println(" Database insert error: " + e.getMessage());
        }
    }

    
}

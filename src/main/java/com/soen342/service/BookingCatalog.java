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
        String findClientSQL = "SELECT id FROM Client WHERE firstName = ? AND lastName = ? AND age = ?";
        String insertClientSQL = "INSERT INTO Client(firstName, lastName, age) VALUES (?, ?, ?)";
         String insertTripSQL = "INSERT INTO BookedTrips (tripID, bookingDate, departureDate, arrivalDate, isFirstClass) VALUES (?, ?, ?, ?, ?)";
        String insertReservationSQL = "INSERT INTO Reservations(tripId, clientId, connectionRouteId, isFirstClass) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            int clientId;
            // --- Step 1: Find or insert client ---
            try (PreparedStatement psFind = conn.prepareStatement(findClientSQL)) {
                psFind.setString(1, booking.getClient().getFirstName());
                psFind.setString(2, booking.getClient().getLastName());
                psFind.setInt(3, booking.getClient().getAge());
                ResultSet rs = psFind.executeQuery();

                if (rs.next()) {
                    clientId = rs.getInt("id");
                } else {
                    try (PreparedStatement psInsert = conn.prepareStatement(insertClientSQL, Statement.RETURN_GENERATED_KEYS)) {
                        psInsert.setString(1, booking.getClient().getFirstName());
                        psInsert.setString(2, booking.getClient().getLastName());
                        psInsert.setInt(3, booking.getClient().getAge());
                        psInsert.executeUpdate();
                        ResultSet keys = psInsert.getGeneratedKeys();
                        keys.next();
                        clientId = keys.getInt(1);
                    }
                }
            }

            // --- Step 2: Insert trip ---
            int tripId;
            com.soen342.domain.Connection firstConn = booking.getTrip().getConnections().get(0);
            com.soen342.domain.Connection lastConn =
                    booking.getTrip().getConnections().get(booking.getTrip().getConnections().size() - 1);

             String findTripSQL = "SELECT bookedTripId FROM BookedTrips WHERE tripID = ?";
            try (PreparedStatement psFindTrip = conn.prepareStatement(findTripSQL)) {
                psFindTrip.setString(1, booking.getTrip().getTripID());
                ResultSet rsTrip = psFindTrip.executeQuery();
                if (rsTrip.next()) {
                    // Reuse existing BookedTrip
                    tripId = rsTrip.getInt("bookedTripId");
                } else {
                    // Create new BookedTrip only once
                    try (PreparedStatement psTrip = conn.prepareStatement(insertTripSQL, Statement.RETURN_GENERATED_KEYS)) {
                        psTrip.setString(1, booking.getTrip().getTripID());
                        psTrip.setString(2, java.time.LocalDate.now().toString()); // bookingDate
                        psTrip.setString(3, firstConn.getParameters().getDepartureTime().toString());
                        psTrip.setString(4, lastConn.getParameters().getArrivalTime().toString());
                        psTrip.setInt(5, 0); // second class
                        psTrip.executeUpdate();
                        ResultSet keys = psTrip.getGeneratedKeys();
                        keys.next();
                        tripId = keys.getInt(1);
                    }
                }
            }
            // --- Step 3: Insert reservations + tickets ---
            for (Reservation reservation : booking.getReservations()) {
                for (com.soen342.domain.Connection connItem : booking.getTrip().getConnections()) {
                    // Retrieve actual connectionId from DB using routeID
                    int connectionId = 0;
                    String findConnectionSQL = "SELECT id FROM Connections WHERE routeID = ?";
                    try (PreparedStatement psFindConn = conn.prepareStatement(findConnectionSQL)) {
                        psFindConn.setString(1, connItem.getRouteID());
                        ResultSet rsConn = psFindConn.executeQuery();
                        if (rsConn.next()) {
                            connectionId = rsConn.getInt("id");
                        }
                    }

                    // Insert reservation
                    try (PreparedStatement psRes = conn.prepareStatement(insertReservationSQL, Statement.RETURN_GENERATED_KEYS)) {
                        psRes.setInt(1, tripId);
                        psRes.setInt(2, clientId);
                        psRes.setInt(3, connectionId);
                        psRes.setInt(4, 0); // second class
                        psRes.executeUpdate();
                    }
                }
            }

            conn.commit(); //  Transaction successful
            System.out.println("Booking saved for client: "
                    + booking.getClient().getFirstName() + " " + booking.getClient().getLastName());

        } catch (SQLException e) {
            System.err.println("Database insert error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void viewReservationsByClientName(String firstName, String lastName) {
    String sql = """
        SELECT 
            r.id AS reservationId,
            bt.tripID AS tripCode,
            c1.name AS departureCity,
            c2.name AS arrivalCity,
            tr.type AS trainType,
            conn.departureTime,
            conn.arrivalTime,
            conn.firstClassPrice,
            conn.secondClassPrice
        FROM Reservations r
        JOIN Client cl ON r.clientId = cl.id
        JOIN BookedTrips bt ON r.tripId = bt.bookedTripId
        JOIN Connections conn ON r.connectionRouteId = conn.id
        JOIN Cities c1 ON conn.departureCityId = c1.id
        JOIN Cities c2 ON conn.arrivalCityId = c2.id
        JOIN Trains tr ON conn.trainId = tr.id
        WHERE cl.firstName = ? AND cl.lastName = ?
        ORDER BY conn.departureTime;
    """;

    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, firstName);
        ps.setString(2, lastName);
        ResultSet rs = ps.executeQuery();

        boolean found = false;
        System.out.println("\n=== Reservations for " + firstName + " " + lastName + " ===");
        while (rs.next()) {
            found = true;
            System.out.println("Reservation ID: " + rs.getInt("reservationId"));
            System.out.println("Trip Code: " + rs.getString("tripCode"));
            System.out.println("From: " + rs.getString("departureCity") + " ->" + rs.getString("arrivalCity"));
            System.out.println("Train: " + rs.getString("trainType"));
            System.out.println("Departure: " + rs.getString("departureTime"));
            System.out.println("Arrival: " + rs.getString("arrivalTime"));
            System.out.println("1st Class: EUR" + rs.getDouble("firstClassPrice"));
            System.out.println("2nd Class: EUR" + rs.getDouble("secondClassPrice"));
            System.out.println("----------------------------------------");
        }

        if (!found) {
            System.out.println("No reservations found for this client.");
        }

    } catch (SQLException e) {
        System.err.println("Error retrieving reservations: " + e.getMessage());
        e.printStackTrace();
    }
}



    
}

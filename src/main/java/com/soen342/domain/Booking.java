package com.soen342.domain;
import java.util.List;
import java.util.ArrayList;

public class Booking {
    private static int counter = 0;
    private String bookingID;
    private Trip trip;
    private List<Reservation> reservations;
    
    public Booking(Trip trip) {
        counter++;
        this.bookingID = "BOOK" + String.format("%04d", counter);
        this.trip = trip;
        this.reservations = new ArrayList<>();
    }
    
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public String getBookingID() {
        return bookingID;
    }
    
    public Trip getTrip() {
        return trip;
    }
    
    public List<Reservation> getReservations() {
        return reservations;
    }
    
    /**
     * Initialize the static counter from database
     * Call this at application startup
     */
    public static void initializeCounter(int maxCounter) {
        counter = maxCounter;
    }
    
    /**
     * Get current counter value (for testing/debugging)
     */
    public static int getCounter() {
        return counter;
    }
}
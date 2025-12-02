package com.soen342.domain;

public class Reservation {
    private static int counter = 0;
    private String reservationID;
    private Ticket ticket;
    private Client client;
    private Trip trip;

    public Reservation(Client client, Trip trip) {
        counter++;
        this.reservationID = "RES" + String.format("%04d", counter);
        this.client = client;
        this.trip = trip;
        this.ticket = new Ticket(this, client);
    }

    public String getReservationID() {
        return reservationID;
    }

    public Client getClient() {
        return client;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Trip getTrip() {
        return trip;
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
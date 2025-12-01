package com.soen342.domain;

public class Ticket {
    private static int counter = 0;
    private String ticketID;
    private Reservation reservation;
    private Client client;

    public Ticket(Reservation reservation, Client client) {
        counter++;
        this.ticketID = "TICK" + String.format("%04d", counter);
        this.reservation = reservation;
        this.client = client;
    }

    public String getTicketID() {
        return ticketID;
    }
    
    public Reservation getReservation() {
        return reservation;
    }
    
    public Client getClient() {
        return client;
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
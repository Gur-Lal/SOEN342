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
}

package com.soen342.domain;

import java.util.List;

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
}

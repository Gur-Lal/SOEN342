package com.soen342.domain;

import java.util.List;

public class Reservation {
    private static int counter = 0;
    private String reservationID;
    private Ticket ticket;
    private Client client;
    private Trip trip;

     private Connection connection;
    private int dbId;

    public Reservation(Client client, Trip trip) {
        counter++;
        this.reservationID = "RES" + String.format("%04d", counter);
        this.client = client;
        this.trip = trip;
        this.ticket = new Ticket(this, client);
    }

    public Reservation(Client client, Trip trip, Connection connection) {
        this(client, trip);
        this.connection = connection;
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

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "dbId=" + dbId +
                ", reservationID='" + reservationID + '\'' +
                ", client=" + (client != null ? client.getName() : "N/A") +
                ", tripID=" + (trip != null ? trip.getTripID() : "N/A") +
                ", connection=" + (connection != null ? connection.getParameters().getDepartureCity() + " → " + connection.getParameters().getArrivalCity() : "N/A") +
                '}';
    }
}

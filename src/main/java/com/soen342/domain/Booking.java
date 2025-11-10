package com.soen342.domain;
import java.util.List;
import java.util.ArrayList;

public class Booking {
    private static int counter = 0;
    private String bookingID;
    private Trip trip;
    private List<Reservation> reservations;

      private Client client;

       private int dbId;


    public Booking(Trip trip) {
        counter++;
        this.bookingID = "BOOK" + String.format("%04d", counter);
        this.trip = trip;
        this.reservations = new ArrayList<>();
    }

    public Booking(Client client, Trip trip) {
        counter++;
        this.bookingID = "BOOK" + String.format("%04d", counter);
        this.client = client;
        this.trip = trip;
        this.reservations = new ArrayList<>();
    }
    
    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }

    public String getBookingID() {
        return bookingID;
    }

     public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public int getDbId() {
        return dbId;
    }

    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    
@Override
public String toString() {
    return "Booking{" +
           "dbId=" + dbId +
           ", bookingID='" + bookingID + '\'' +
           ", client=" + (client != null ?  client.getFirstName() + " " + client.getLastName()  : "N/A") +
           ", tripID=" + (trip != null ? trip.getTripID() : "N/A") +
           ", reservations=" + reservations.size() +
           '}';
}

}

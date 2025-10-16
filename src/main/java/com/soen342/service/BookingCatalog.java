package com.soen342.service;
import com.soen342.domain.Booking;
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

    
}

package com.soen342.service;

import com.soen342.domain.Ticket;
import java.util.ArrayList;
import java.util.List;

public class TicketCatalog {
    private List<Ticket> tickets;

    public TicketCatalog() {
        this.tickets = new ArrayList<>();
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }

    public List<Ticket> getTickets() {
        return tickets;
    }
}

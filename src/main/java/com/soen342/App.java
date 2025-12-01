package com.soen342;

import com.soen342.service.DatabaseManager;
import com.soen342.service.BookingDAO;
import com.soen342.service.RetrievalDAO;
import com.soen342.service.ConnectionDAO;
import com.soen342.domain.Search;
import com.soen342.service.ConnectionCatalog;
import com.soen342.service.SearchService;
import com.soen342.domain.Parameters;
import com.soen342.domain.Reservation;
import com.soen342.domain.Booking;
import com.soen342.service.SearchResult;
import com.soen342.domain.Trip;
import com.soen342.domain.Client;
import java.util.List;
import java.sql.Time;
import java.io.File;
import java.util.List;
import java.util.Scanner;


public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== EU Rail Trip Planner ===");

        // Initialize Database
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.initializeTables();
        
        // Initialize counters from database
        Booking.initializeCounter(dbManager.getNextBookingCounter());
        Trip.initializeCounter(dbManager.getNextTripCounter());
        Reservation.initializeCounter(dbManager.getNextReservationCounter());
        com.soen342.domain.Ticket.initializeCounter(dbManager.getNextTicketCounter());
        
        // Create DAOs for database operations
        BookingDAO bookingDAO = new BookingDAO(dbManager.getConnection());
        RetrievalDAO retrievalDAO = new RetrievalDAO(dbManager.getConnection());
        ConnectionDAO connectionDAO = new ConnectionDAO(dbManager.getConnection());
        
        // Load connections from CSV into database (only once, they persist)
        String csvPath = "src/main/java/com/soen342/resources/eu_rail_network.csv";
        File csvFile = new File(csvPath);
        
        if (!csvFile.exists()) {
            System.out.println("Error: CSV file not found at " + csvFile.getAbsolutePath());
            scanner.close();
            dbManager.closeConnection();
            System.exit(1);
        }
        
        // Load connections into catalog AND database
        ConnectionCatalog catalog = new ConnectionCatalog();
        catalog.loadFromFile(csvPath);
        
        // Save connections to database if not already there
        int existingConnections = connectionDAO.getConnectionCount();
        if (existingConnections == 0) {
            System.out.println("Loading connections into database for first time...");
            for (com.soen342.domain.Connection conn : catalog.getAllConnections()) {
                connectionDAO.saveConnection(conn);
            }
            System.out.println("Loaded " + catalog.getAllConnections().size() + " connections into database.");
        }

        // Main menu
        System.out.println("\nWhat would you like to do?");
        System.out.println("1. Make a new booking");
        System.out.println("2. View my reservations");
        System.out.print("Enter your choice (1 or 2): ");
        
        int mainChoice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        
        if (mainChoice == 2) {
            // Retrieve reservations flow
            handleRetrieveReservations(scanner, retrievalDAO);
            scanner.close();
            dbManager.closeConnection();
            return;
        } else if (mainChoice != 1) {
            System.out.println("Invalid choice. Exiting.");
            scanner.close();
            dbManager.closeConnection();
            return;
        }

        // Continue with booking flow (mainChoice == 1)
        
        // Parameters initialization
        Time departureTime = null;
        Time arrivalTime = null;
        String trainType = null;
        String daysOfOperation = null;
        double firstClassRate = 0.0;
        double secondClassRate = 0.0;

        // Take user inputs for Parameters
        System.out.print("\nEnter departure city (press Enter to skip): ");
        String departureCity = scanner.nextLine().trim();
        if (departureCity.isEmpty()) departureCity = null;

        System.out.print("Enter arrival city (press Enter to skip): ");
        String arrivalCity = scanner.nextLine().trim();
        if (arrivalCity.isEmpty()) arrivalCity = null;

        System.out.print("Enter departure time (HH:MM:SS or press Enter to skip): ");
        String depTimeInput = scanner.nextLine().trim();
        if (!depTimeInput.isEmpty()) departureTime = Time.valueOf(depTimeInput);

        System.out.print("Enter arrival time (HH:MM:SS or press Enter to skip): ");
        String arrTimeInput = scanner.nextLine().trim();
        if (!arrTimeInput.isEmpty()) arrivalTime = Time.valueOf(arrTimeInput);

        System.out.print("Enter train type (press Enter to skip): ");
        trainType = scanner.nextLine().trim();
        if (trainType.isEmpty()) trainType = null;

        System.out.print("Enter days of operation (Mon, Tue, Wed, Thu, Fri, Sat, Sun) (press Enter to skip): ");
        daysOfOperation = scanner.nextLine().trim();
        if (daysOfOperation.isEmpty()) daysOfOperation = null;

        System.out.print("Enter first-class rate (press Enter to skip): ");
        String fcInput = scanner.nextLine().trim();
        if (!fcInput.isEmpty()) firstClassRate = Double.parseDouble(fcInput);

        System.out.print("Enter second-class rate (press Enter to skip): ");
        String scInput = scanner.nextLine().trim();
        if (!scInput.isEmpty()) secondClassRate = Double.parseDouble(scInput);

        // Create Parameters object
        Parameters parameters = new Parameters(
                departureCity,
                arrivalCity,
                departureTime,
                arrivalTime,
                trainType,
                daysOfOperation,
                firstClassRate,
                secondClassRate
        );

        // Perform search
        Search search = new Search(parameters);
        SearchService searchService = new SearchService(catalog);
        SearchResult result = searchService.searchTrips(search);

        // If no results
        if (result.getSearchResultDirect().isEmpty() && result.getSearchResultIndirect().isEmpty()) {
            System.out.println("\nNo matching trips found.");
            scanner.close();
            dbManager.closeConnection();
            return;
        }

        // Ask user for sorting options
        System.out.println("\nHow would you like to sort the trips?");
        System.out.println("1. By Duration");
        System.out.println("2. By Price");
        System.out.print("Enter your choice (1 or 2): ");
        int sortFieldChoice = scanner.nextInt();

        System.out.println("\nChoose order:");
        System.out.println("1. Ascending");
        System.out.println("2. Descending");
        System.out.print("Enter your choice (1 or 2): ");
        int sortOrderChoice = scanner.nextInt();

        // Apply chosen sorting method
        if (sortFieldChoice == 1 && sortOrderChoice == 1) {
            result.sortDurationAsc();
            System.out.println("\nTrips sorted by duration (ascending).");
        } else if (sortFieldChoice == 1 && sortOrderChoice == 2) {
            result.sortDurationDesc();
            System.out.println("\nTrips sorted by duration (descending).");
        } else if (sortFieldChoice == 2 && sortOrderChoice == 1) {
            result.sortPriceAsc();
            System.out.println("\nTrips sorted by price (ascending).");
        } else if (sortFieldChoice == 2 && sortOrderChoice == 2) {
            result.sortPriceDesc();
            System.out.println("\nTrips sorted by price (descending).");
        } else {
            System.out.println("\nInvalid choice. Showing unsorted results.");
        }

        // Display results
        System.out.println("\n=== Search Results ===");
        System.out.println(result);


        System.out.println("Enter the trip ID to make a booking: ");
        String tripID = scanner.next();
        Trip selectedTrip = result.getTripByID(tripID);

        if (selectedTrip == null) {
            System.out.println("Invalid trip ID.");
            scanner.close();
            dbManager.closeConnection();
            return;
        }

        // Create booking object
        Booking booking = new Booking(selectedTrip);

        System.out.println("How many passengers? ");
        int numPassengers = scanner.nextInt();

        System.out.println("Enter name, age and ID for each passenger: ");
        for (int i = 0; i < numPassengers; i++) {
            System.out.println("Passenger " + (i + 1) + ":");
            System.out.print("First Name: ");
            String firstName = scanner.next();
            System.out.print("Last Name: ");
            String lastName = scanner.next();
            System.out.print("Age: ");
            int age = scanner.nextInt();
            System.out.print("ID: ");
            String id = scanner.next();

            Client client = new Client(firstName, lastName, age, id);
            Reservation reservation = new Reservation(client, selectedTrip);
            
            // Add reservation to booking
            booking.addReservation(reservation);
            
            System.out.println("Reservation successful! Reservation ID: " + reservation.getReservationID() 
                             + ", Ticket ID: " + reservation.getTicket().getTicketID());
        }

        // Save booking to database
        System.out.println("\nSaving booking to database...");
        boolean saved = bookingDAO.saveBooking(booking);
        
        if (saved) {
            System.out.println("Booking " + booking.getBookingID() + " saved successfully!");
            System.out.println("All reservations and tickets have been stored.");
        } else {
            System.out.println("Failed to save booking to database.");
        }
        scanner.close();
        dbManager.closeConnection();
    }
    
    /**
     * Handles the retrieve reservations flow
     */
    private static void handleRetrieveReservations(Scanner scanner, RetrievalDAO retrievalDAO) {
        System.out.println("\n=== Retrieve My Reservations ===");
        
        System.out.print("Enter your last name: ");
        String lastName = scanner.nextLine().trim();
        
        System.out.print("Enter your ID (license number): ");
        String licenseId = scanner.nextLine().trim();
        
        System.out.println("\nSearching for reservations...");
        
        RetrievalDAO.ClientTripsResult result = retrievalDAO.getClientTripsGrouped(lastName, licenseId);
        
        if (!result.hasCurrentTrips() && !result.hasPastTrips()) {
            System.out.println("No reservations found for " + lastName + " with ID " + licenseId);
            return;
        }
        
        // Display current/future trips
        System.out.println("\n" + "=".repeat(70));
        System.out.println("CURRENT & UPCOMING TRIPS");
        System.out.println("=".repeat(70));
        
        if (result.hasCurrentTrips()) {
            for (Reservation reservation : result.getCurrentTrips()) {
                displayReservation(reservation);
            }
        } else {
            System.out.println("No current or upcoming trips.");
        }
        
        // Display past trips (history)
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TRIP HISTORY (Past Trips)");
        System.out.println("=".repeat(70));
        
        if (result.hasPastTrips()) {
            for (Reservation reservation : result.getPastTrips()) {
                displayReservation(reservation);
            }
        } else {
            System.out.println("No past trips.");
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Total current/upcoming trips: " + result.getCurrentTrips().size());
        System.out.println("Total past trips: " + result.getPastTrips().size());
    }
    
    /**
     * Displays a single reservation with its details
     */
    private static void displayReservation(Reservation reservation) {
        System.out.println("\n--- Reservation " + reservation.getReservationID() + " ---");
        System.out.println("Passenger: " + reservation.getClient().getFullName() + 
                          " (Age: " + reservation.getClient().getAge() + 
                          ", ID: " + reservation.getClient().getId() + ")");
        System.out.println("Ticket ID: " + reservation.getTicket().getTicketID());
        System.out.println("\nTrip Details:");
        System.out.println(reservation.getTrip().toString());
    }
}
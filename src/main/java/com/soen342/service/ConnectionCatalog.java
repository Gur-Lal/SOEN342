package com.soen342.service;
import java.sql.*;    

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.soen342.domain.Trip;
import com.soen342.domain.Search;
import com.soen342.domain.Parameters;
import com.soen342.domain.Connection;

public class ConnectionCatalog {

    private List<Connection> connections;

    public ConnectionCatalog() {
        this.connections = new ArrayList<>();
    }

    public List<Connection> getAllConnections() {
        return connections;
    }

    private String expandDays(String daysStr) {

        List<String> days = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");


        daysStr = daysStr.trim();
        if (!daysStr.contains("-")) return daysStr;


        String[] range = daysStr.split("-");
        if (range.length != 2) return daysStr;

        String start = range[0].trim();
        String end = range[1].trim();

        int startIndex = days.indexOf(start);
        int endIndex = days.indexOf(end);
        if (startIndex == -1 || endIndex == -1) return daysStr;

        StringBuilder expanded = new StringBuilder();
        int i = startIndex;
        while (true) {
            expanded.append(days.get(i));
            if (i == endIndex) break;
            expanded.append(", ");
            i = (i + 1) % days.size();
        }

        return expanded.toString();
    }

     // now also saves to SQLite after loading from file
    public void loadFromFile(String filePath) {
        connections.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             java.sql.Connection dbConn = DatabaseManager.getConnection()) {

            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length < 9) continue;

                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].replace("\"", "").trim();
                }

                String routeID = parts[0];
                String departureCity = parts[1];
                String arrivalCity = parts[2];

                String depTimeStr = parts[3].trim() + ":00";
                Time departureTime = Time.valueOf(depTimeStr);

                String arrTimeStr = parts[4].trim().split(" ")[0] + ":00";
                Time arrivalTime = Time.valueOf(arrTimeStr);

                String trainType = parts[5];
                String daysOfOperation = expandDays(parts[6]);
                double firstClassRate = Double.parseDouble(parts[7]);
                double secondClassRate = Double.parseDouble(parts[8]);

                Parameters parameters = new Parameters(
                        departureCity, arrivalCity, departureTime, arrivalTime,
                        trainType, daysOfOperation, firstClassRate, secondClassRate
                );

                Connection connection = new Connection(routeID, parameters);

                // insert or find existing in DB, store dbId
                int dbId = saveToDatabase(dbConn, connection);
                connection.setId(dbId);

                connections.add(connection);
            }
                //POUR DEBUGGING
           // System.out.println("Loaded " + connections.size() + " connections (DB + memory).");

        } catch (IOException | SQLException e) {
            System.err.println("Error loading connections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // insert into SQLite only if not already there; return DB id
   private int saveToDatabase(java.sql.Connection dbConn, Connection connObj) throws SQLException {
    Parameters p = connObj.getParameters();

    //  Find or insert departure & arrival cities ----
    int depCityId = findOrInsertCity(dbConn, p.getDepartureCity());
    int arrCityId = findOrInsertCity(dbConn, p.getArrivalCity());

    // Find or insert train type ----
    int trainId = findOrInsertTrain(dbConn, p.getTrainType());

    //  Check if connection already exists ----
    String findSql = """
        SELECT id FROM Connections 
        WHERE routeId = ? AND departureCityId = ? AND arrivalCityId = ? 
          AND departureTime = ? AND arrivalTime = ? AND trainId = ?
    """;
    try (PreparedStatement psFind = dbConn.prepareStatement(findSql)) {
        psFind.setString(1, connObj.getRouteID());
        psFind.setInt(2, depCityId);
        psFind.setInt(3, arrCityId);
        psFind.setString(4, p.getDepartureTime().toString());
        psFind.setString(5, p.getArrivalTime().toString());
        psFind.setInt(6, trainId);
        ResultSet rs = psFind.executeQuery();
        if (rs.next()) {
            return rs.getInt("id"); // already exists
        }
    }

    //  Insert new connection ----
    String insertSql = """
        INSERT INTO Connections
        (routeId, departureCityId, arrivalCityId, departureTime, arrivalTime,
         trainId, daysOfOperation, firstClassPrice, secondClassPrice)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
    """;

    try (PreparedStatement ps = dbConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, connObj.getRouteID());
        ps.setInt(2, depCityId);
        ps.setInt(3, arrCityId);
        ps.setString(4, p.getDepartureTime().toString());
        ps.setString(5, p.getArrivalTime().toString());
        ps.setInt(6, trainId);
        ps.setString(7, p.getDaysOfOperation());
        ps.setDouble(8, p.getFirstClassRate());
        ps.setDouble(9, p.getSecondClassRate());
        ps.executeUpdate();

        ResultSet keys = ps.getGeneratedKeys();
        keys.next();
        return keys.getInt(1);
    }
}

    private int findOrInsertCity(java.sql.Connection dbConn, String cityName) throws SQLException {
        String findSql = "SELECT id FROM Cities WHERE name = ?";
        try (PreparedStatement psFind = dbConn.prepareStatement(findSql)) {
            psFind.setString(1, cityName);
            ResultSet rs = psFind.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO Cities(name) VALUES (?)";
        try (PreparedStatement psInsert = dbConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, cityName);
            psInsert.executeUpdate();
            ResultSet keys = psInsert.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        }
    }
    private int findOrInsertTrain(java.sql.Connection dbConn, String trainType) throws SQLException {
        String findSql = "SELECT id FROM Trains WHERE type = ?";
        try (PreparedStatement psFind = dbConn.prepareStatement(findSql)) {
            psFind.setString(1, trainType);
            ResultSet rs = psFind.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        String insertSql = "INSERT INTO Trains(type) VALUES (?)";
        try (PreparedStatement psInsert = dbConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            psInsert.setString(1, trainType);
            psInsert.executeUpdate();
            ResultSet keys = psInsert.getGeneratedKeys();
            keys.next();
            return keys.getInt(1);
        }
    }


    // optional: reload all from DB if needed
    public List<Connection> loadFromDatabase() {
        List<Connection> dbConnections = new ArrayList<>();
        String sql = "SELECT * FROM Connection";

        try (java.sql.Connection dbConn = DatabaseManager.getConnection();
             Statement st = dbConn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String depCity = rs.getString("departure");
                String arrCity = rs.getString("arrival");
                Time depTime = null;
                Time arrTime = null;
                double price = rs.getDouble("price");

                Parameters params = new Parameters(depCity, arrCity, depTime, arrTime, null, null, 0, price);
                Connection conn = new Connection(String.valueOf(id), params);
                conn.setId(id);
                dbConnections.add(conn);
            }

            System.out.println("✅ Loaded " + dbConnections.size() + " connections from DB.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dbConnections;
    }


    // Checks if a direct connection exists. Is used searchTrips 
    public boolean directConnectionExists(Parameters searchParams) {
        for (Connection conn : connections) {
            Parameters params = conn.getParameters();
            if (params.getDepartureCity().equalsIgnoreCase(searchParams.getDepartureCity()) &&
                params.getArrivalCity().equalsIgnoreCase(searchParams.getArrivalCity())) {
                return true;
            }
        }
        return false;
    }

    // Calculates total time for a list of connections
    public Time calculateTotalTime(List<Connection> connections) {

        if (connections == null || connections.isEmpty()) {
            return Time.valueOf("00:00:00");
        }

        long totalMillis = 0;

        for (Connection conn : connections) {
            long duration = conn.calculateTime();
            totalMillis += duration;
        }

        totalMillis = totalMillis % (24 * 60 * 60 * 1000);

        int hours = (int) (totalMillis / (1000 * 60 * 60));
        int minutes = (int) ((totalMillis / (1000 * 60)) % 60);
        int seconds = (int) ((totalMillis / 1000) % 60);

        String formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return Time.valueOf(formatted);
    }

    // Calculates total first class rate for a list of connections
    public double calculateTotalFCRate(List<Connection> connections) {
        double total = 0.0;

        for (Connection connection : connections) {
            total += connection.getParameters().getFirstClassRate();
        }

        return total;
    }

    // Calculates total second class rate for a list of connections
    public double calculateTotalSCRate(List<Connection> connections) {
        double total = 0.0;

        for (Connection connection : connections) {
            total += connection.getParameters().getSecondClassRate();
        }

        return total;
    }

    // Searches for direct connections based on search parameters
    public List<Trip> searchDirect(Parameters searchParams) {
        List<Trip> result = new ArrayList<>();

        for (Connection conn : connections) {
            Parameters params = conn.getParameters();

            if (!params.getDepartureCity().equalsIgnoreCase(searchParams.getDepartureCity())) continue;
            if (!params.getArrivalCity().equalsIgnoreCase(searchParams.getArrivalCity())) continue;

            if (searchParams.getDepartureTime() != null && params.getDepartureTime().before(searchParams.getDepartureTime())) continue;
            if (searchParams.getArrivalTime() != null && params.getArrivalTime().after(searchParams.getArrivalTime())) continue;
            if (searchParams.getTrainType() != null && !params.getTrainType().equalsIgnoreCase(searchParams.getTrainType())) continue;
            if (searchParams.getDaysOfOperation() != null && !params.getDaysOfOperation().contains(searchParams.getDaysOfOperation())) continue;
            if (searchParams.getFirstClassRate() > 0 && params.getFirstClassRate() > searchParams.getFirstClassRate()) continue;
            if (searchParams.getSecondClassRate() > 0 && params.getSecondClassRate() > searchParams.getSecondClassRate()) continue;

            List<Connection> connList = new ArrayList<>();
            connList.add(conn);
            Time totalTime = calculateTotalTime(connList);
            double totalFCRate = calculateTotalFCRate(connList);
            double totalSCRate = calculateTotalSCRate(connList);
            Trip trip = new Trip(totalTime, totalFCRate, totalSCRate, connList);
            result.add(trip);
        }

        return result;
        
    }

    public List<Trip> searchIndirect(Parameters searchParams) {
    List<Trip> result = new ArrayList<>();

    // --- Two-hop connections ---
    for (Connection conn1 : connections) {
        String dep1 = conn1.getParameters().getDepartureCity();
        String arr1 = conn1.getParameters().getArrivalCity();

        if (dep1.equalsIgnoreCase(searchParams.getDepartureCity())) {
            for (Connection conn2 : connections) {
                String dep2 = conn2.getParameters().getDepartureCity();
                String arr2 = conn2.getParameters().getArrivalCity();

                // Avoid loops or redundant paths
                if (dep2.equalsIgnoreCase(arr1) &&
                    arr2.equalsIgnoreCase(searchParams.getArrivalCity()) &&
                    !arr2.equalsIgnoreCase(dep1) &&   // don’t go back to start
                    !dep2.equalsIgnoreCase(dep1)) {   // avoid same city twice

                    // Build valid trip
                    List<Connection> connList = new ArrayList<>();
                    connList.add(conn1);
                    connList.add(conn2);

                    Time totalTime = calculateTotalTime(connList);
                    double totalFCRate = calculateTotalFCRate(connList);
                    double totalSCRate = calculateTotalSCRate(connList);
                    Trip trip = new Trip(totalTime, totalFCRate, totalSCRate, connList);

                    // Avoid adding if already present (basic duplicate check)
                    if (!result.contains(trip)) {
                        result.add(trip);
                    }
                }
            }
        }
    }

    // --- Three-hop connections ---
    for (Connection conn1 : connections) {
        String dep1 = conn1.getParameters().getDepartureCity();
        String arr1 = conn1.getParameters().getArrivalCity();

        if (dep1.equalsIgnoreCase(searchParams.getDepartureCity())) {
            for (Connection conn2 : connections) {
                String dep2 = conn2.getParameters().getDepartureCity();
                String arr2 = conn2.getParameters().getArrivalCity();

                if (dep2.equalsIgnoreCase(arr1) &&
                    !arr2.equalsIgnoreCase(dep1) &&   // no return to start
                    !dep2.equalsIgnoreCase(dep1)) {   // avoid same city twice
                    for (Connection conn3 : connections) {
                        String dep3 = conn3.getParameters().getDepartureCity();
                        String arr3 = conn3.getParameters().getArrivalCity();

                        if (dep3.equalsIgnoreCase(arr2) &&
                            arr3.equalsIgnoreCase(searchParams.getArrivalCity()) &&
                            !arr3.equalsIgnoreCase(dep1) &&  // avoid loop back to start
                            !arr3.equalsIgnoreCase(arr1) &&  // avoid revisiting arr1
                            !dep3.equalsIgnoreCase(dep1)) {  // avoid same city twice

                            List<Connection> connList = new ArrayList<>();
                            connList.add(conn1);
                            connList.add(conn2);
                            connList.add(conn3);

                            Time totalTime = calculateTotalTime(connList);
                            double totalFCRate = calculateTotalFCRate(connList);
                            double totalSCRate = calculateTotalSCRate(connList);
                            Trip trip = new Trip(totalTime, totalFCRate, totalSCRate, connList);

                            if (!result.contains(trip)) {
                                result.add(trip);
                            }
                        }
                    }
                }
            }
        }
    }

    return result;
}


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Connection Catalog ===\n");
        for (Connection c : connections) {
            sb.append(c.toString()).append("\n");
        }
        return sb.toString();
    }

}

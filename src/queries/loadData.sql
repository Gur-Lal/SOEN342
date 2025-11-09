INSERT OR IGNORE INTO Cities(name)
SELECT DISTINCT departureCity FROM RawConnections
UNION
SELECT DISTINCT arrivalCity FROM RawConnections;

INSERT OR IGNORE INTO Trains(type)
SELECT DISTINCT trainType FROM RawConnections;

INSERT INTO Connections (
    routeId, departureCityId, arrivalCityId, departureTime, arrivalTime,
    trainId, daysOfOperation, firstClassPrice, secondClassPrice
)
SELECT
    r.routeId,
    (SELECT id FROM Cities WHERE name = r.departureCity),
    (SELECT id FROM Cities WHERE name = r.arrivalCity),
    r.departureTime,
    r.arrivalTime,
    (SELECT id FROM Trains WHERE type = r.trainType),
    r.daysOfOperation,
    r.firstClassPrice,
    r.secondClassPrice
FROM RawConnections r;
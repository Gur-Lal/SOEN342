CREATE TABLE Connections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    routeId TEXT UNIQUE NOT NULL,
    departureCityId INTEGER NOT NULL,
    arrivalCityId INTEGER NOT NULL,
    departureTime TEXT NOT NULL,
    arrivalTime TEXT NOT NULL,
    trainId INTEGER NOT NULL,
    daysOfOperation TEXT NOT NULL,
    firstClassPrice REAL NOT NULL,
    secondClassPrice REAL NOT NULL,
    isNextDay INTEGER DEFAULT 0,

    FOREIGN KEY (departureCityId) REFERENCES Cities(id),
    FOREIGN KEY (arrivalCityId) REFERENCES Cities(id),
    FOREIGN KEY (trainId) REFERENCES Trains(id)
);
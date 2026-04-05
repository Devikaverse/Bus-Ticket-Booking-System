-- ================================================================
--   SMART BUS RESERVATION SYSTEM
--   GitHub Repository Setup Script
--   Version : 1.0
--   Database : SmartBusReservation
--   Tool     : MySQL Workbench / MySQL 8.0+
--   IDE      : NetBeans
--   Authors  : K Akhila          (24B11CS184)
--              K Ch Devika Reddy (24B11CS193)
--              K Jasmitha Sri Gowri (24B11CS196)
--              S Hemanth Srinivas (24B11CS396)
--   College  : Aditya University, Surampalem
-- ================================================================
--   HOW TO USE:
--   1. Open MySQL Workbench
--   2. Go to File > Open SQL Script > select this file
--   3. Press Ctrl+Shift+Enter to run the entire script
--   4. Open the project in NetBeans and run it
-- ================================================================


-- ================================================================
-- STEP 1 : CREATE & SELECT DATABASE
-- ================================================================

CREATE DATABASE IF NOT EXISTS SmartBusReservation;
USE SmartBusReservation;


-- ================================================================
-- STEP 2 : DROP EXISTING TABLES (clean slate for fresh setup)
--          Order matters — child tables dropped before parents
-- ================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS schedules;
DROP TABLE IF EXISTS buses;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;


-- ================================================================
-- STEP 3 : CREATE TABLES
-- ================================================================

-- ----------------------------------------------------
-- users
-- user_id = 1 is always treated as Admin in the app
-- All other users are Passengers
-- ----------------------------------------------------
CREATE TABLE users (
    user_id  INT          PRIMARY KEY AUTO_INCREMENT,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(100) NOT NULL,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(50)  NOT NULL,
    role     VARCHAR(20)  DEFAULT 'customer'
);

-- ----------------------------------------------------
-- buses
-- Stores bus route master data
-- Managed by Admin via ManageBus.java
-- ----------------------------------------------------
CREATE TABLE buses (
    bus_id      INT          PRIMARY KEY AUTO_INCREMENT,
    bus_number  VARCHAR(50)  NOT NULL,
    source      VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    fare        DOUBLE       NOT NULL,
    bus_type    VARCHAR(50)  DEFAULT 'Non-AC',
    total_seats INT          DEFAULT 40,
    travel_date DATE
);

-- ----------------------------------------------------
-- schedules
-- Links buses to specific dates with seat availability
-- Used in BookTicket.java JOIN query
-- ----------------------------------------------------
CREATE TABLE schedules (
    schedule_id     INT    PRIMARY KEY AUTO_INCREMENT,
    bus_id          INT    NOT NULL,
    travel_date     DATE   NOT NULL,
    available_seats INT    NOT NULL DEFAULT 40,
    fare            DOUBLE NOT NULL,
    FOREIGN KEY (bus_id) REFERENCES buses(bus_id)
);

-- ----------------------------------------------------
-- bookings
-- Central transaction table
-- Populated by SeatSelection.java on booking confirmation
-- ----------------------------------------------------
CREATE TABLE bookings (
    booking_id     INT          PRIMARY KEY AUTO_INCREMENT,
    user_id        INT,
    schedule_id    INT,
    bus_id         INT,
    passenger_name VARCHAR(100) NOT NULL,
    seats_booked   INT          NOT NULL,
    total_amount   DOUBLE,
    travel_date    VARCHAR(20),
    route          VARCHAR(200),
    booking_date   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)     REFERENCES users(user_id),
    FOREIGN KEY (bus_id)      REFERENCES buses(bus_id),
    FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id)
);


-- ================================================================
-- STEP 4 : INSERT DEFAULT DATA
-- ================================================================

-- ----------------------------------------------------
-- Admin user  (user_id = 1 → triggers Admin Dashboard)
-- Default login: username = admin / password = admin123
-- ----------------------------------------------------
INSERT INTO users (name, email, username, password, role)
VALUES ('AdminHead', 'admindev@gmail.com', 'admin', 'admin123', 'admin');

-- ----------------------------------------------------
-- Sample passenger accounts (for testing)
-- ----------------------------------------------------
INSERT INTO users (name, email, username, password, role)
VALUES
    ('Amit Kumar',   'amit@gmail.com',  'amit_99',      'pass123', 'customer'),
    ('Neha Sharma',  'neha@gmail.com',  'ruby',         'pass123', 'customer'),
    ('Rahul Verma',  'rahul@gmail.com', 'rashi_v',      'pass123', 'customer'),
    ('Priya Sharma', 'priya@gmail.com', 'priya_sharma', 'pass123', 'customer');

-- ----------------------------------------------------
-- Bus master data
-- ----------------------------------------------------
INSERT INTO buses (bus_number, source, destination, fare, bus_type, total_seats, travel_date)
VALUES
    ('AP-31-TV-1234', 'Vizag',    'Hyderabad',  1200, 'AC Sleeper', 40, '2026-04-10'),
    ('AP-31-TV-7777', 'Vizag',    'Vijayawada',  850, 'AC',         40, '2026-04-15'),
    ('AP-31-TV-9999', 'Vizag',    'Chennai',    1500, 'AC Sleeper', 40, '2026-04-20'),
    ('AP-RJ-1313-TP', 'Vizag',    'Tirupathi',  1800, 'AC',         40, '2026-04-15'),
    ('AP-31-TV-0220', 'Kakinada', 'Rajamundry',  500, 'Non-AC',     40, '2026-04-08'),
    ('AP-31-TV-0101', 'Hyderabad','Vijayawada',  600, 'Non-AC',     40, '2026-04-10'),
    ('AP-31-TV-0119', 'Tirupathi','Vijayawada', 2000, 'AC',         40, '2026-04-14');

-- ----------------------------------------------------
-- Schedule data  (matches the screenshot in the report)
-- ----------------------------------------------------
INSERT INTO schedules (bus_id, travel_date, available_seats, fare)
VALUES
    (3, '2026-04-06', 40, 1100.00),
    (4, '2026-04-07', 40,  850.00),
    (5, '2026-04-08', 42, 1300.00),
    (1, '2026-04-10', 47, 1350.00),
    (2, '2026-04-24', 40, 1200.00),
    (1, '2026-04-07', 40, 1220.00),
    (7, '2026-04-14', 35, 1200.00),
    (6, '2026-04-10', 40, 1200.00),
    (5, '2026-04-24', 40, 1000.00);


-- ================================================================
-- STEP 5 : VERIFY SETUP
-- ================================================================

SHOW TABLES;
SELECT * FROM users;
SELECT * FROM buses;
SELECT * FROM schedules;
SELECT * FROM bookings;


-- ================================================================
-- STEP 6 : KEY QUERIES USED IN THE APPLICATION
--          (For reference — these run inside the Java code)
-- ================================================================

-- BookTicket.java — Search buses by route and date (3-day window)
SELECT
    s.schedule_id,
    s.bus_id,
    b.bus_number,
    b.source,
    b.destination,
    s.fare,
    s.travel_date
FROM schedules s
JOIN buses b ON s.bus_id = b.bus_id
WHERE LOWER(b.source)      = LOWER('Vizag')
  AND LOWER(b.destination) = LOWER('Hyderabad')
  AND s.travel_date BETWEEN DATE_SUB('2026-04-10', INTERVAL 1 DAY)
                        AND DATE_ADD('2026-04-10', INTERVAL 2 DAY)
ORDER BY s.travel_date ASC;

-- SeatSelection.java — Check if seat is already booked
SELECT COUNT(*)
FROM bookings
WHERE bus_id = 1 AND seats_booked = 5 AND travel_date = '2026-04-10';

-- SeatSelection.java — Load all booked seats for a bus on a date
SELECT seats_booked
FROM bookings
WHERE bus_id = 1 AND travel_date = '2026-04-10';

-- SeatSelection.java — Insert a confirmed booking
INSERT INTO bookings (
    user_id, schedule_id, bus_id,
    passenger_name, seats_booked,
    total_amount, travel_date, route
)
VALUES (
    2, 1, 1,
    'Amit Kumar', 7,
    1200.00, '2026-04-10', 'Vizag to Hyderabad'
);

-- ViewBookings.java (Admin) — All bookings with passenger details
SELECT
    bk.booking_id,
    u.name        AS passenger,
    b.bus_number,
    bk.route,
    bk.travel_date,
    bk.total_amount
FROM bookings bk
JOIN users u    ON bk.user_id = u.user_id
JOIN buses b    ON bk.bus_id  = b.bus_id
ORDER BY bk.booking_id DESC;

-- ViewBookings.java (Admin) — Total revenue
SELECT SUM(total_amount) AS total_revenue FROM bookings;

-- ViewMyHistory.java (User) — User's personal booking history
SELECT
    booking_id,
    route,
    passenger_name,
    seats_booked,
    total_amount,
    travel_date
FROM bookings
WHERE user_id = 2;

-- ManageBus.java — View all buses (Admin)
SELECT bus_id, bus_number, source, destination, fare, travel_date
FROM buses;


-- ================================================================
-- END OF SETUP SCRIPT
-- ================================================================

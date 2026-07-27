-- PostgreSQL schema and data for the load-balancing-java project.
-- Run this file against your PostgreSQL server:
--   psql -U postgres -f students.sql

-- Create database (run separately if it doesn't exist yet):
-- CREATE DATABASE students;

\c students;

DROP TABLE IF EXISTS studentinfo;

CREATE TABLE studentinfo (
                             sid   INTEGER      PRIMARY KEY,
                             name  VARCHAR(60),
                             dob   VARCHAR(15),
                             major VARCHAR(50),
                             level VARCHAR(15),
                             year  VARCHAR(20)
);

INSERT INTO studentinfo (sid, name, dob, major, level, year) VALUES
(1, 'Rahul Mehta',      '03/14/2000', 'Computer Engineering',       'Undergraduate', 'Junior'),
(2, 'Priya Nair',       '11/09/1999', 'Data Science',               'Graduate',      'First'),
(3, 'Arjun Verma',      '06/25/2001', 'Artificial Intelligence',    'Undergraduate', 'Sophomore'),
(4, 'Sneha Kapoor',     '09/18/1998', 'Cyber Security',             'Graduate',      'Second'),
(5, 'Rohan Gupta',      '01/30/2002', 'Software Engineering',       'Undergraduate', 'Freshman'),
(6, 'Ananya Iyer',      '08/12/1999', 'Information Technology',     'Graduate',      'First'),
(7, 'Karan Malhotra',   '04/07/2000', 'Computer Science',           'Undergraduate', 'Senior');
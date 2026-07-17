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
    (1, 'Ayush Soni',       '5/19/97',  'Computer Science',                  'Graduate',     'First'),
    (2, 'Daniel Stafford',  '2/10/97',  'Information Security',               'Graduate',     'Second'),
    (3, 'Mark Straten',     '5/12/96',  'Interactive Games and Media',        'Undergraduate','Senior'),
    (4, 'Varun Sharma',     '8/22/99',  'Information Science and Technology', 'Undergraduate','Freshman'),
    (5, 'Alexander Luoi',   '7/18/98',  'Cyber Security',                    'Undergraduate','Sophomore'),
    (6, 'Yu Kong',          '12/13/97', 'Computer Science',                  'Graduate',     'First'),
    (7, 'Peter Drinklage',  '8/27/98',  'Software Engineering',              'Undergraduate','Senior');

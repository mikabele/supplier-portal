DROP TABLE IF EXISTS last_notification CASCADE;
CREATE TABLE last_notification
(
    last_date TIMESTAMP
);

INSERT INTO last_notification
VALUES (CURRENT_TIMESTAMP);
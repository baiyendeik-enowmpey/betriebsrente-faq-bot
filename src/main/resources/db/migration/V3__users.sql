CREATE TABLE users (
    username VARCHAR(50) NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL REFERENCES users(username) ON DELETE CASCADE,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT uq_authorities_username_authority UNIQUE (username, authority)
);

CREATE INDEX idx_authorities_username ON authorities(username);

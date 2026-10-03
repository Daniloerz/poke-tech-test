--liquibase formatted sql

--changeset poke-tech-test:20261003-02-seed-app-user
--comment: Demo users documented in the README (ash / pikachu123, misty / starmie123). BCrypt hashes, strength 10.
INSERT INTO app_user (username, password_hash) VALUES
    ('ash',   '$2a$10$7sltUls.nWf047ebQYCe4.x7lrLmYqCI3.a0Zjt9VQAKi0az9tKY6'),
    ('misty', '$2a$10$ueXh0cX.1tm8In7wVSAKaOrmrLuyDShJ0T9u/QKJk4YPAFpwdErmu');
--rollback DELETE FROM app_user WHERE username IN ('ash', 'misty');

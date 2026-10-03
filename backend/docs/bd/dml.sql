-- Consolidated demo data. Source of truth: Liquibase changesets in src/main/resources/db/changelog/changes.

-- Demo users (documented in the README). Passwords: ash / pikachu123, misty / starmie123.
-- Hashes are BCrypt, strength 10.
INSERT INTO app_user (username, password_hash) VALUES
    ('ash',   '$2a$10$7sltUls.nWf047ebQYCe4.x7lrLmYqCI3.a0Zjt9VQAKi0az9tKY6'),
    ('misty', '$2a$10$ueXh0cX.1tm8In7wVSAKaOrmrLuyDShJ0T9u/QKJk4YPAFpwdErmu');

-- Demo local Pokemon (US03 AC-12): PokeAPI snapshot plus proprietary fields.
INSERT INTO local_pokemon (poke_api_id, name, sprite_url, types, height_decimetres, weight_hectograms, localized_name, region, tags) VALUES
    (1, 'bulbasaur',  'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png', ARRAY['grass', 'poison'], 7, 69, 'Fushigidane', 'Kanto', ARRAY['starter', 'gen-1']),
    (4, 'charmander', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png', ARRAY['fire'],             6, 85, 'Hitokage',    'Kanto', ARRAY['starter', 'gen-1']),
    (7, 'squirtle',   'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png', ARRAY['water'],            5, 90, 'Zenigame',    'Kanto', ARRAY['starter', 'gen-1']);

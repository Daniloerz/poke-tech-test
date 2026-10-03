--liquibase formatted sql

--changeset poke-tech-test:20261003-04-seed-local-pokemon
--comment: Demo local Pokemon (US03 AC-12): PokeAPI snapshot plus proprietary fields.
INSERT INTO local_pokemon (poke_api_id, name, sprite_url, types, height_decimetres, weight_hectograms, localized_name, region, tags) VALUES
    (1, 'bulbasaur',  'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png', ARRAY['grass', 'poison'], 7, 69, 'Fushigidane', 'Kanto', ARRAY['starter', 'gen-1']),
    (4, 'charmander', 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/4.png', ARRAY['fire'],             6, 85, 'Hitokage',    'Kanto', ARRAY['starter', 'gen-1']),
    (7, 'squirtle',   'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/7.png', ARRAY['water'],            5, 90, 'Zenigame',    'Kanto', ARRAY['starter', 'gen-1']);
--rollback DELETE FROM local_pokemon WHERE poke_api_id IN (1, 4, 7);

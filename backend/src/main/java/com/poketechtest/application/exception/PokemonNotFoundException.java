package com.poketechtest.application.exception;

public class PokemonNotFoundException extends RuntimeException {

    public PokemonNotFoundException(String identifier) {
        super("Pokemon not found: " + identifier);
    }
}

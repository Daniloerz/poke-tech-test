package com.poketechtest.application.exception;

public class LocalPokemonNotFoundException extends RuntimeException {

    public LocalPokemonNotFoundException(long id) {
        super("Local Pokemon not found: " + id);
    }
}

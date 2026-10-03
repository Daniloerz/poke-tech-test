package com.poketechtest.application.exception;

import lombok.Getter;

@Getter
public class PokemonNotFoundException extends RuntimeException {

    private final String identifier;

    public PokemonNotFoundException(String identifier) {
        super("Pokemon not found: " + identifier);
        this.identifier = identifier;
    }
}

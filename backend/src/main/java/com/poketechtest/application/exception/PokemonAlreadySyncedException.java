package com.poketechtest.application.exception;

public class PokemonAlreadySyncedException extends RuntimeException {

    public PokemonAlreadySyncedException(String name, Long localId) {
        super(localId == null
                ? "Pokemon already synchronized: " + name
                : "Pokemon already synchronized: " + name + " (local id " + localId + ")");
    }
}

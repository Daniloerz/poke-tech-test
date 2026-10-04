package com.poketechtest.application.exception;

import lombok.Getter;

@Getter
public class PokemonAlreadySyncedException extends RuntimeException {

    /** Id of the existing local record; null when it is not known (two requests at the same time). */
    private final Long localId;

    public PokemonAlreadySyncedException(String name, Long localId) {
        super(localId == null
                ? "Pokemon already synchronized: " + name
                : "Pokemon already synchronized: " + name + " (local id " + localId + ")");
        this.localId = localId;
    }
}

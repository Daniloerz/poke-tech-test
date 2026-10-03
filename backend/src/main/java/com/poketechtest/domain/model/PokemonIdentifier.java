package com.poketechtest.domain.model;

import java.util.Locale;
import java.util.Objects;

/** A PokeAPI id or name. Names are case-insensitive, so identifiers are always trimmed and lower-cased. */
public final class PokemonIdentifier {

    private PokemonIdentifier() {
    }

    public static String normalize(String idOrName) {
        return Objects.requireNonNull(idOrName, "idOrName must not be null").trim().toLowerCase(Locale.ROOT);
    }
}

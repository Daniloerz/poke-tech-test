package com.poketechtest.domain.model;

import java.math.BigDecimal;

/** PokeAPI gives height in decimetres and weight in hectograms: both become metres and kilograms with one decimal. */
final class Measurements {

    private static final int ONE_DECIMAL_SCALE = 1;

    private Measurements() {
    }

    static BigDecimal fromTenths(int tenths) {
        return BigDecimal.valueOf(tenths, ONE_DECIMAL_SCALE);
    }
}

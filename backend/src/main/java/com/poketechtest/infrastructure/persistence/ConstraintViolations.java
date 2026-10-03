package com.poketechtest.infrastructure.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/** Tells which database constraint failed, so only the expected one is turned into a business error. */
final class ConstraintViolations {

    private ConstraintViolations() {
    }

    static boolean isViolationOf(DataIntegrityViolationException exception, String constraintName) {
        return exception.getCause() instanceof ConstraintViolationException violation
                && constraintName.equalsIgnoreCase(violation.getConstraintName());
    }
}

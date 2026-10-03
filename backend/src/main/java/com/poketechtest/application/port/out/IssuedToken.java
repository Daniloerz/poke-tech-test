package com.poketechtest.application.port.out;

public record IssuedToken(String value, long expiresInSeconds) {

    // Keeps the token out of logs.
    @Override
    public String toString() {
        return "IssuedToken[expiresInSeconds=" + expiresInSeconds + "]";
    }
}

package com.poketechtest.interfaces.rest.dto;

import com.poketechtest.application.port.out.IssuedToken;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(IssuedToken issuedToken) {
        return new TokenResponse(issuedToken.value(), BEARER, issuedToken.expiresInSeconds());
    }
}

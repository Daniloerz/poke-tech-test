package com.poketechtest.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.poketechtest.application.port.out.IssuedToken;
import com.poketechtest.domain.model.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokenIssuerTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters!";
    private static final Duration EXPIRATION = Duration.ofHours(1);
    private static final User ASH = new User(1L, "ash", "hash", null);

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final JwtProperties jwtProperties = new JwtProperties(SECRET, EXPIRATION, "poke-tech-test");
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(jwtProperties);

    @Test
    void issuesASignedTokenWithTheUserClaims() {
        Instant now = Instant.now();
        JwtTokenIssuer jwtTokenIssuer = issuerWithClock(Clock.fixed(now, ZoneOffset.UTC), jwtProperties);

        IssuedToken token = jwtTokenIssuer.issue(ASH);
        Jwt jwt = jwtDecoder.decode(token.value());

        assertThat(token.expiresInSeconds()).isEqualTo(3600);
        assertThat(jwt.getSubject()).isEqualTo("ash");
        assertThat(jwt.<Number>getClaim(JwtTokenIssuer.USER_ID_CLAIM).longValue()).isEqualTo(1L);
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("poke-tech-test");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(EXPIRATION);
    }

    @Test
    void anExpiredTokenIsRejected() {
        Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
        JwtTokenIssuer jwtTokenIssuer = issuerWithClock(Clock.fixed(twoHoursAgo, ZoneOffset.UTC), jwtProperties);

        String expiredToken = jwtTokenIssuer.issue(ASH).value();

        assertThatThrownBy(() -> jwtDecoder.decode(expiredToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        JwtProperties otherProperties = new JwtProperties("another-secret-with-at-least-32-characters", EXPIRATION, "poke-tech-test");
        JwtTokenIssuer otherIssuer = issuerWithClock(Clock.systemUTC(), otherProperties);

        String foreignToken = otherIssuer.issue(ASH).value();

        assertThatThrownBy(() -> jwtDecoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenFromAnotherIssuerIsRejected() {
        JwtProperties otherIssuerProperties = new JwtProperties(SECRET, EXPIRATION, "someone-else");
        JwtTokenIssuer otherIssuer = issuerWithClock(Clock.systemUTC(), otherIssuerProperties);

        String foreignToken = otherIssuer.issue(ASH).value();

        assertThatThrownBy(() -> jwtDecoder.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void propertiesToStringDoesNotExposeTheSecret() {
        assertThat(jwtProperties.toString()).doesNotContain(SECRET);
    }

    private JwtTokenIssuer issuerWithClock(Clock clock, JwtProperties properties) {
        return new JwtTokenIssuer(securityConfig.jwtEncoder(properties), properties, clock);
    }
}

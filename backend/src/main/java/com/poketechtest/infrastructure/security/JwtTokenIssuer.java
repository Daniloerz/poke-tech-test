package com.poketechtest.infrastructure.security;

import com.poketechtest.application.port.out.IssuedToken;
import com.poketechtest.application.port.out.TokenIssuer;
import com.poketechtest.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

    static final String USER_ID_CLAIM = "uid";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Override
    public IssuedToken issue(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(user.username())
                .claim(USER_ID_CLAIM, user.id())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.expiration()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(tokenValue, jwtProperties.expiration().toSeconds());
    }
}

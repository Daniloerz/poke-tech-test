package com.poketechtest.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private static final String[] PUBLIC_GET_ROUTES = {
            "/api/v1/pokemon", "/api/v1/pokemon/**",
            "/actuator/health",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
    };
    private static final String[] PUBLIC_POST_ROUTES = {"/api/v1/auth/register", "/api/v1/auth/login"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProblemHandler securityProblemHandler) throws Exception {
        return http
                // Stateless JWT API: no session and no cookies, so CSRF protection is not needed.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ROUTES).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey(jwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtProperties.issuer()));
        return jwtDecoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties jwtProperties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(jwtProperties)).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private SecretKey secretKey(JwtProperties jwtProperties) {
        return new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
    }
}

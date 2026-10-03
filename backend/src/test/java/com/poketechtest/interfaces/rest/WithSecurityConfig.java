package com.poketechtest.interfaces.rest;

import com.poketechtest.infrastructure.security.SecurityConfig;
import com.poketechtest.infrastructure.security.SecurityProblemHandler;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** Runs a @WebMvcTest with the real security rules of the application and a test JWT secret. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, SecurityProblemHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=" + WithSecurityConfig.TEST_JWT_SECRET,
        "security.jwt.expiration=1h",
        "security.jwt.issuer=poke-tech-test"
})
public @interface WithSecurityConfig {

    String TEST_JWT_SECRET = "test-secret-with-at-least-32-characters!";
}

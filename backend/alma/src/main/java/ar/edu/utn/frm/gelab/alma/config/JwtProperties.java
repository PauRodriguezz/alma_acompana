package ar.edu.utn.frm.gelab.alma.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del JWT propio (alma.security.jwt.*).
 */
@ConfigurationProperties("alma.security.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        long rememberExpirationMinutes,
        String issuer) {
}

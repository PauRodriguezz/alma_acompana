package ar.edu.utn.frm.gelab.alma.auth.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.config.JwtProperties;

/**
 * Emite el JWT con los claims del contrato (sección 5): iss, sub, rol, iat y exp.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROL = "rol";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propiedades;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties propiedades) {
        this.jwtEncoder = jwtEncoder;
        this.propiedades = propiedades;
    }

    public TokenEmitido emitir(Cuenta cuenta, boolean mantenerSesion) {
        Duration duracion = Duration.ofMinutes(mantenerSesion
                ? propiedades.rememberExpirationMinutes()
                : propiedades.expirationMinutes());
        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .subject(String.valueOf(cuenta.getId()))
                .claim(CLAIM_ROL, cuenta.getRol().name())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
        return new TokenEmitido(token, duracion.toSeconds());
    }

    public record TokenEmitido(String token, long expiraEnSegundos) {
    }
}

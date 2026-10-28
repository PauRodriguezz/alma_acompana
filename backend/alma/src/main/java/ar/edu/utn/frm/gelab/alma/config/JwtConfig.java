package ar.edu.utn.frm.gelab.alma.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Firma y validación del JWT con HS256 (ver docs/contrato-login.md, sección 5).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    private static final int MINIMO_BYTES_SECRETO = 32;

    private final JwtProperties propiedades;

    public JwtConfig(JwtProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveSecreta()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(claveSecreta())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propiedades.issuer()));
        return decoder;
    }

    /** Lee el claim "rol" y le agrega ROLE_, para poder usar hasRole("ADMINISTRADOR"). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private SecretKey claveSecreta() {
        String secreto = propiedades.secret();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < MINIMO_BYTES_SECRETO) {
            throw new IllegalStateException(
                    "La clave JWT_SECRET falta o es muy corta: tiene que tener al menos "
                            + MINIMO_BYTES_SECRETO + " bytes. Generá una con: openssl rand -base64 48");
        }
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}

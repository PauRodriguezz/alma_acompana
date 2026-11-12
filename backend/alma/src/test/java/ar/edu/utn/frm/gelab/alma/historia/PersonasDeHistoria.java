package ar.edu.utn.frm.gelab.alma.historia;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Cuentas y personas mayores de prueba del módulo de historia (prefijo {@code test_historia_}), con su
 * limpieza: primero el subgrafo de la persona con {@code eliminarHistoria}, después las filas de PostgreSQL.
 * La persona mayor tiene el mismo id en PostgreSQL y en el grafo.
 */
final class PersonasDeHistoria {

    /** La cuenta y la persona mayor de un usuario de prueba. */
    record Persona(Long cuentaId, Long personaId) {
    }

    private final CuentaRepository cuentaRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistoriaDeVidaService historia;
    private final JwtEncoder jwtEncoder;

    PersonasDeHistoria(CuentaRepository cuentaRepository, PersonaMayorRepository personaMayorRepository,
            PasswordEncoder passwordEncoder, HistoriaDeVidaService historia, JwtEncoder jwtEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.passwordEncoder = passwordEncoder;
        this.historia = historia;
        this.jwtEncoder = jwtEncoder;
    }

    /** Crea la cuenta y la persona mayor del usuario (o las devuelve si ya existen). */
    Persona crear(String nombreUsuario) {
        Cuenta cuenta = cuentaRepository.findByNombreUsuario(nombreUsuario).orElseGet(() -> nuevaCuenta(nombreUsuario));
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuenta.getId()).orElseGet(() -> {
            PreferenciaAccesibilidad preferencia = new PreferenciaAccesibilidad();
            preferencia.setTamanioLetra(TamanioLetra.NORMAL);
            preferencia.setAltoContraste(false);
            PersonaMayor nueva = new PersonaMayor();
            nueva.setNombre("Test");
            nueva.setApellido("Historia");
            nueva.setFechaNacimiento(LocalDate.of(1950, 5, 12));
            nueva.setCuenta(cuenta);
            nueva.setPreferenciaAccesibilidad(preferencia);
            return personaMayorRepository.save(nueva);
        });
        return new Persona(cuenta.getId(), persona.getId());
    }

    /** Crea solo la cuenta, sin persona mayor (para el 404). */
    Long crearCuentaSinPersona(String nombreUsuario) {
        return cuentaRepository.findByNombreUsuario(nombreUsuario).orElseGet(() -> nuevaCuenta(nombreUsuario)).getId();
    }

    /** Borra el subgrafo, la persona y la cuenta. No falla si no existen. */
    void borrar(String nombreUsuario) {
        cuentaRepository.findByNombreUsuario(nombreUsuario).ifPresent(cuenta -> {
            personaMayorRepository.findByCuentaId(cuenta.getId()).ifPresent(persona -> {
                historia.eliminarHistoria(persona.getId());
                personaMayorRepository.delete(persona);
            });
            cuentaRepository.delete(cuenta);
        });
    }

    String token(Long cuentaId, String rol) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("alma")
                .subject(String.valueOf(cuentaId))
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private Cuenta nuevaCuenta(String nombreUsuario) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNombreUsuario(nombreUsuario);
        cuenta.setContrasenia(passwordEncoder.encode("Alma2026!"));
        cuenta.setRol(Rol.PERSONA_MAYOR);
        cuenta.setFechaAlta(LocalDate.of(2026, 11, 4));
        return cuentaRepository.save(cuenta);
    }
}

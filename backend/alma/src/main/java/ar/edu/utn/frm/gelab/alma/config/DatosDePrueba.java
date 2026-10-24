package ar.edu.utn.frm.gelab.alma.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.administrador.domain.Administrador;
import ar.edu.utn.frm.gelab.alma.administrador.repository.AdministradorRepository;
import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.domain.Rol;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.domain.TamanioLetra;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Crea los usuarios de prueba (contrato, sección 10). Solo con alma.seed.enabled=true.
 * Es idempotente: si el nombre de usuario ya existe, no lo vuelve a crear.
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDePrueba implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDePrueba.class);

    private static final String CONTRASENIA_PRUEBA = "Alma2026!";

    private final CuentaRepository cuentaRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    public DatosDePrueba(CuentaRepository cuentaRepository,
                         PersonaMayorRepository personaMayorRepository,
                         AdministradorRepository administradorRepository,
                         PasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearPersonaMayor("ana", "Ana", "Pérez", LocalDate.of(1950, 5, 12));
        crearPersonaMayor("carlos", "Carlos", "Gómez", LocalDate.of(1948, 11, 3));
        crearAdministrador("admin", "Admin", "Alma");
    }

    private void crearPersonaMayor(String nombreUsuario, String nombre, String apellido,
                                   LocalDate fechaNacimiento) {
        if (cuentaRepository.existsByNombreUsuario(nombreUsuario)) {
            return;
        }
        PreferenciaAccesibilidad preferencia = new PreferenciaAccesibilidad();
        preferencia.setTamanioLetra(TamanioLetra.NORMAL);
        preferencia.setAltoContraste(false);

        PersonaMayor persona = new PersonaMayor();
        persona.setNombre(nombre);
        persona.setApellido(apellido);
        persona.setFechaNacimiento(fechaNacimiento);
        persona.setCuenta(crearCuenta(nombreUsuario, Rol.PERSONA_MAYOR));
        persona.setPreferenciaAccesibilidad(preferencia);
        personaMayorRepository.save(persona);
        log.info("Usuario de prueba creado: {} ({})", nombreUsuario, Rol.PERSONA_MAYOR);
    }

    private void crearAdministrador(String nombreUsuario, String nombre, String apellido) {
        if (cuentaRepository.existsByNombreUsuario(nombreUsuario)) {
            return;
        }
        Administrador administrador = new Administrador();
        administrador.setNombre(nombre);
        administrador.setApellido(apellido);
        administrador.setCuenta(crearCuenta(nombreUsuario, Rol.ADMINISTRADOR));
        administradorRepository.save(administrador);
        log.info("Usuario de prueba creado: {} ({})", nombreUsuario, Rol.ADMINISTRADOR);
    }

    private Cuenta crearCuenta(String nombreUsuario, Rol rol) {
        Cuenta cuenta = new Cuenta();
        cuenta.setNombreUsuario(nombreUsuario);
        cuenta.setContrasenia(passwordEncoder.encode(CONTRASENIA_PRUEBA));
        cuenta.setRol(rol);
        cuenta.setFechaAlta(LocalDate.now());
        return cuentaRepository.save(cuenta);
    }
}

package ar.edu.utn.frm.gelab.alma.auth.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.administrador.domain.Administrador;
import ar.edu.utn.frm.gelab.alma.administrador.repository.AdministradorRepository;
import ar.edu.utn.frm.gelab.alma.auth.domain.Cuenta;
import ar.edu.utn.frm.gelab.alma.auth.dto.LoginRequest;
import ar.edu.utn.frm.gelab.alma.auth.dto.LoginResponse;
import ar.edu.utn.frm.gelab.alma.auth.dto.UsuarioResponse;
import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.common.exception.CredencialesInvalidasException;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.dto.PreferenciaAccesibilidadDto;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

@Service
public class AuthService {

    private final CuentaRepository cuentaRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final AdministradorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Hash contra el que se compara cuando el usuario no existe, para que el tiempo de respuesta no lo delate. */
    private final String hashDummy;

    public AuthService(CuentaRepository cuentaRepository,
                       PersonaMayorRepository personaMayorRepository,
                       AdministradorRepository administradorRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.cuentaRepository = cuentaRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.administradorRepository = administradorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashDummy = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String nombreUsuario = request.nombreUsuario().trim().toLowerCase(Locale.ROOT);
        Optional<Cuenta> cuenta = cuentaRepository.findByNombreUsuario(nombreUsuario);

        // Se compara siempre, exista o no la cuenta. Ambos casos lanzan la misma excepción.
        String hash = cuenta.map(Cuenta::getContrasenia).orElse(hashDummy);
        boolean contraseniaCorrecta = passwordEncoder.matches(request.contrasenia(), hash);
        if (cuenta.isEmpty() || !contraseniaCorrecta) {
            throw new CredencialesInvalidasException();
        }

        JwtService.TokenEmitido token = jwtService.emitir(cuenta.get(), request.mantenerSesion());
        return new LoginResponse(token.token(), LoginResponse.TIPO_BEARER, token.expiraEnSegundos(),
                usuarioDe(cuenta.get()));
    }

    /** Usuario de la cuenta del token. Si la cuenta ya no existe, la sesión deja de valer (401). */
    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(Long cuentaId) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new InvalidBearerTokenException("La cuenta del token no existe"));
        return usuarioDe(cuenta);
    }

    private UsuarioResponse usuarioDe(Cuenta cuenta) {
        return switch (cuenta.getRol()) {
            case PERSONA_MAYOR -> {
                PersonaMayor persona = personaMayorRepository.findByCuentaId(cuenta.getId())
                        .orElseThrow(() -> sinDatos(cuenta));
                yield new UsuarioResponse(cuenta.getId(), cuenta.getNombreUsuario(),
                        persona.getNombre(), persona.getApellido(), cuenta.getRol(),
                        PreferenciaAccesibilidadDto.de(persona.getPreferenciaAccesibilidad()));
            }
            case ADMINISTRADOR -> {
                Administrador administrador = administradorRepository.findByCuentaId(cuenta.getId())
                        .orElseThrow(() -> sinDatos(cuenta));
                yield new UsuarioResponse(cuenta.getId(), cuenta.getNombreUsuario(),
                        administrador.getNombre(), administrador.getApellido(), cuenta.getRol(), null);
            }
        };
    }

    /** Una cuenta sin su PersonaMayor o Administrador es un dato roto: termina en un 500 genérico. */
    private static IllegalStateException sinDatos(Cuenta cuenta) {
        return new IllegalStateException("La cuenta " + cuenta.getId() + " (" + cuenta.getRol()
                + ") no tiene sus datos personales cargados");
    }
}

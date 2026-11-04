package ar.edu.utn.frm.gelab.alma.bienestar.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.bienestar.domain.RegistroAnimo;
import ar.edu.utn.frm.gelab.alma.bienestar.dto.RegistrarAnimoRequest;
import ar.edu.utn.frm.gelab.alma.bienestar.dto.RegistroAnimoResponse;
import ar.edu.utn.frm.gelab.alma.bienestar.repository.RegistroAnimoRepository;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Registro y corrección del ánimo de la persona del token (CU-01). Todo dato se busca con la cuenta
 * del token: lo que no es de la persona responde 404. "Hoy" se calcula solo con el Clock inyectado.
 * Es el punto de entrada para otros módulos (por ejemplo, Chat): se llama a este servicio, no al HTTP.
 */
@Service
public class RegistroAnimoService {

    private final RegistroAnimoRepository registroRepository;
    private final PersonaMayorRepository personaMayorRepository;
    private final Clock clock;

    public RegistroAnimoService(RegistroAnimoRepository registroRepository,
                                PersonaMayorRepository personaMayorRepository,
                                Clock clock) {
        this.registroRepository = registroRepository;
        this.personaMayorRepository = personaMayorRepository;
        this.clock = clock;
    }

    /** Crea un registro nuevo con la hora de ahora. No hay límite por día. */
    @Transactional
    public RegistroAnimoResponse registrar(Long cuentaId, RegistrarAnimoRequest datos) {
        PersonaMayor persona = personaMayorRepository.findByCuentaId(cuentaId)
                .orElseThrow(RecursoNoEncontradoException::new);

        RegistroAnimo registro = new RegistroAnimo();
        registro.setPersonaMayor(persona);
        registro.setNivelAnimo(datos.nivelAnimo());
        registro.setFechaHora(LocalDateTime.now(clock));
        return RegistroAnimoResponse.de(registroRepository.save(registro));
    }

    /**
     * Corrige el ánimo de hoy: cambia el nivel y la fecha y hora, y conserva el id. Si el registro no
     * existe, no es de la persona o no es su ánimo de hoy, responde 404. Con el mismo nivel no cambia nada.
     */
    @Transactional
    public RegistroAnimoResponse modificar(Long cuentaId, Long id, RegistrarAnimoRequest datos) {
        RegistroAnimo registro = ultimoDeHoy(cuentaId)
                .filter(deHoy -> deHoy.getId().equals(id))
                .orElseThrow(RecursoNoEncontradoException::new);

        if (registro.getNivelAnimo() != datos.nivelAnimo()) {
            registro.setNivelAnimo(datos.nivelAnimo());
            registro.setFechaHora(LocalDateTime.now(clock));
        }
        return RegistroAnimoResponse.de(registro);
    }

    /** El último ánimo de hoy, o vacío si todavía no registró. Lo usa el controlador y también el Chat. */
    @Transactional(readOnly = true)
    public Optional<RegistroAnimoResponse> animoDeHoy(Long cuentaId) {
        return ultimoDeHoy(cuentaId).map(RegistroAnimoResponse::de);
    }

    private Optional<RegistroAnimo> ultimoDeHoy(Long cuentaId) {
        LocalDateTime desde = LocalDateTime.now(clock).toLocalDate().atStartOfDay();
        return registroRepository
                .findFirstByPersonaMayorCuentaIdAndFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraDescIdDesc(
                        cuentaId, desde, desde.plusDays(1));
    }
}

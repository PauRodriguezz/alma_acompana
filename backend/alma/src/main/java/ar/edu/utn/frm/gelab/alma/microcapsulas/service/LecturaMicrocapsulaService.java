package ar.edu.utn.frm.gelab.alma.microcapsulas.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.MicrocapsulaGuardada;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaLecturaDetalleResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaLecturaResumenResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaGuardadaRepository;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Lectura de microcápsulas de la persona del token (CU-06): listado, guardadas, detalle, guardar y quitar.
 * Contrato de microcápsulas, sección 5. "Vigente" = sin fechaBaja. Lo del panel (CU-11) sigue en
 * MicrocapsulaService.
 */
@Service
public class LecturaMicrocapsulaService {

    private final PersonaMayorRepository personaMayorRepository;
    private final MicrocapsulaRepository microcapsulaRepository;
    private final MicrocapsulaGuardadaRepository guardadaRepository;
    private final Clock clock;

    public LecturaMicrocapsulaService(PersonaMayorRepository personaMayorRepository,
                                      MicrocapsulaRepository microcapsulaRepository,
                                      MicrocapsulaGuardadaRepository guardadaRepository,
                                      Clock clock) {
        this.personaMayorRepository = personaMayorRepository;
        this.microcapsulaRepository = microcapsulaRepository;
        this.guardadaRepository = guardadaRepository;
        this.clock = clock;
    }

    /** 5.1: las vigentes, de la más nueva a la más vieja (id descendente si empatan), con "guardada". */
    @Transactional(readOnly = true)
    public List<MicrocapsulaLecturaResumenResponse> listar(Long cuentaId) {
        PersonaMayor persona = personaDe(cuentaId);
        Set<Long> guardadas = guardadaRepository.idsGuardadosPor(persona.getId());
        return microcapsulaRepository.findByFechaBajaIsNullOrderByFechaPublicacionDescIdDesc().stream()
                .map(m -> MicrocapsulaLecturaResumenResponse.de(m, guardadas.contains(m.getId())))
                .toList();
    }

    /** 5.2: las guardadas, vigentes o no, de la última guardada a la primera. */
    @Transactional(readOnly = true)
    public List<MicrocapsulaLecturaResumenResponse> guardadas(Long cuentaId) {
        PersonaMayor persona = personaDe(cuentaId);
        return guardadaRepository.guardadasDe(persona.getId()).stream()
                .map(g -> MicrocapsulaLecturaResumenResponse.de(g.getMicrocapsula(), true))
                .toList();
    }

    /** 5.3: vigente, o dada de baja pero guardada por la persona. Si no, 404. */
    @Transactional(readOnly = true)
    public MicrocapsulaLecturaDetalleResponse detalle(Long cuentaId, Long id) {
        PersonaMayor persona = personaDe(cuentaId);
        Accesible accesible = accesible(persona, id);
        return MicrocapsulaLecturaDetalleResponse.de(accesible.microcapsula(), accesible.guardada());
    }

    /** 5.4: idempotente. 404 si no es accesible para la persona. */
    @Transactional
    public void guardar(Long cuentaId, Long id) {
        PersonaMayor persona = personaDe(cuentaId);
        Accesible accesible = accesible(persona, id);
        if (accesible.guardada()) {
            return;
        }
        MicrocapsulaGuardada guardada = new MicrocapsulaGuardada();
        guardada.setPersonaMayor(persona);
        guardada.setMicrocapsula(accesible.microcapsula());
        guardada.setFechaGuardado(LocalDateTime.now(clock));
        guardadaRepository.save(guardada);
    }

    /** 5.5: idempotente. 404 solo si la microcápsula no existe; sirve también para una dada de baja. */
    @Transactional
    public void quitar(Long cuentaId, Long id) {
        PersonaMayor persona = personaDe(cuentaId);
        if (!microcapsulaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException();
        }
        guardadaRepository.borrar(persona.getId(), id);
    }

    /** La microcápsula si la persona puede verla (5.3), con si la tiene guardada. */
    private Accesible accesible(PersonaMayor persona, Long id) {
        Microcapsula microcapsula = microcapsulaRepository.findById(id)
                .orElseThrow(RecursoNoEncontradoException::new);
        boolean guardada = guardadaRepository.existsByPersonaMayorIdAndMicrocapsulaId(persona.getId(), id);
        if (microcapsula.getFechaBaja() != null && !guardada) {
            throw new RecursoNoEncontradoException();
        }
        return new Accesible(microcapsula, guardada);
    }

    private PersonaMayor personaDe(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new);
    }

    private record Accesible(Microcapsula microcapsula, boolean guardada) {
    }
}

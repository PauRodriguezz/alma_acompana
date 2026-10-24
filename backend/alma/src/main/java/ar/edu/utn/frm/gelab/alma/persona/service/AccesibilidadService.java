package ar.edu.utn.frm.gelab.alma.persona.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import ar.edu.utn.frm.gelab.alma.persona.domain.PreferenciaAccesibilidad;
import ar.edu.utn.frm.gelab.alma.persona.dto.PreferenciaAccesibilidadDto;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Preferencias de accesibilidad de la persona mayor del token (CU-13).
 */
@Service
public class AccesibilidadService {

    private final PersonaMayorRepository personaMayorRepository;

    public AccesibilidadService(PersonaMayorRepository personaMayorRepository) {
        this.personaMayorRepository = personaMayorRepository;
    }

    @Transactional(readOnly = true)
    public PreferenciaAccesibilidadDto obtener(Long cuentaId) {
        return PreferenciaAccesibilidadDto.de(preferenciaDe(cuentaId));
    }

    @Transactional
    public PreferenciaAccesibilidadDto actualizar(Long cuentaId, PreferenciaAccesibilidadDto datos) {
        PreferenciaAccesibilidad preferencia = preferenciaDe(cuentaId);
        preferencia.setTamanioLetra(datos.tamanioLetra());
        preferencia.setAltoContraste(datos.altoContraste());
        return PreferenciaAccesibilidadDto.de(preferencia);
    }

    private PreferenciaAccesibilidad preferenciaDe(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId)
                .map(PersonaMayor::getPreferenciaAccesibilidad)
                .orElseThrow(RecursoNoEncontradoException::new);
    }
}

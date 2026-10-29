package ar.edu.utn.frm.gelab.alma.persona.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.persona.dto.PerfilDto;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Perfil de la persona mayor del token (CU-13 y CU-14).
 */
@Service
public class PerfilService {

    private final PersonaMayorRepository personaMayorRepository;

    public PerfilService(PersonaMayorRepository personaMayorRepository) {
        this.personaMayorRepository = personaMayorRepository;
    }

    @Transactional(readOnly = true)
    public PerfilDto obtener(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId)
                .map(PerfilDto::de)
                .orElseThrow(RecursoNoEncontradoException::new);
    }
}

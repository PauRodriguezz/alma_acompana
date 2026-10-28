package ar.edu.utn.frm.gelab.alma.administrador.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.administrador.dto.ResumenPanelResponse;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;

@Service
public class PanelAdminService {

    private final EjercicioRepository ejercicioRepository;
    private final MicrocapsulaRepository microcapsulaRepository;

    public PanelAdminService(EjercicioRepository ejercicioRepository,
                             MicrocapsulaRepository microcapsulaRepository) {
        this.ejercicioRepository = ejercicioRepository;
        this.microcapsulaRepository = microcapsulaRepository;
    }

    /** Cuenta lo que no está dado de baja (sin fechaBaja). */
    @Transactional(readOnly = true)
    public ResumenPanelResponse resumen() {
        return new ResumenPanelResponse(ejercicioRepository.countByFechaBajaIsNull(),
                microcapsulaRepository.countByFechaBajaIsNull());
    }
}

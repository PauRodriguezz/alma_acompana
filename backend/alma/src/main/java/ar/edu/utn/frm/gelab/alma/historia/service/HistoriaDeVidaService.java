package ar.edu.utn.frm.gelab.alma.historia.service;

import static ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionNeo4j.MANEJADOR_NEO4J;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;

/**
 * Historia de vida de la persona mayor (contrato del grafo). Todo va con el manejador de transacciones de
 * Neo4j. Por ahora: la baja de la persona; el guion y el progreso llegan con los endpoints.
 */
@Service
public class HistoriaDeVidaService {

    private final EscrituraDelGrafo escritura;

    public HistoriaDeVidaService(EscrituraDelGrafo escritura) {
        this.escritura = escritura;
    }

    /**
     * Baja de la persona (contrato, 5.10): borra su nodo y todos sus recuerdos, personas y lugares, sin tocar
     * las preguntas del guion ni los datos de otras personas. Se puede repetir sin error. Todavía no la llama
     * ningún endpoint: hoy el panel de operador no da de baja personas (contrato, sección 2).
     */
    @Transactional(MANEJADOR_NEO4J)
    public void eliminarHistoria(Long personaId) {
        escritura.eliminarSubgrafo(personaId);
    }
}

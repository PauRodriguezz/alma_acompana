package ar.edu.utn.frm.gelab.alma.historia.service;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.EtapaDeVida;
import ar.edu.utn.frm.gelab.alma.historia.domain.PrecisionAnio;
import ar.edu.utn.frm.gelab.alma.historia.domain.TextoCortado;
import ar.edu.utn.frm.gelab.alma.historia.dto.ListaDeRecuerdosResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.LugarDeRecuerdoResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.PersonaDeRecuerdoResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RecuerdoDetalleResponse;
import ar.edu.utn.frm.gelab.alma.historia.dto.RecuerdoResumenResponse;
import ar.edu.utn.frm.gelab.alma.historia.repository.DetalleDeRecuerdo;
import ar.edu.utn.frm.gelab.alma.historia.repository.LecturaDelGrafo;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoLeido;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * Lectura de los recuerdos de la persona para la línea de tiempo y el detalle (contrato de historia de vida,
 * 5.1, 5.2 y 7.3 y 7.4). Solo lee: no necesita transacción de escritura. La persona sale siempre del token (el id
 * de la cuenta) y toda consulta parte de su nodo. El log no lleva texto de la persona.
 */
@Service
public class ConsultaDeRecuerdos {

    private final PersonaMayorRepository personaMayorRepository;
    private final LecturaDelGrafo lectura;
    private final ConfiguracionHistoria configuracion;

    public ConsultaDeRecuerdos(PersonaMayorRepository personaMayorRepository, LecturaDelGrafo lectura,
            ConfiguracionHistoria configuracion) {
        this.personaMayorRepository = personaMayorRepository;
        this.lectura = lectura;
        this.configuracion = configuracion;
    }

    /**
     * Los recuerdos en el orden de 5.1 (el grafo ya los devuelve así). Sin recuerdos, lista vacía.
     *
     * @throws RecursoNoEncontradoException 404: la cuenta no tiene persona mayor
     */
    public ListaDeRecuerdosResponse listar(Long cuentaId) {
        List<RecuerdoResumenResponse> recuerdos = lectura.recuerdosEnOrden(personaId(cuentaId)).stream()
                .map(this::resumen).toList();
        return new ListaDeRecuerdosResponse(recuerdos);
    }

    /**
     * El detalle de un recuerdo con sus personas y lugares.
     *
     * @throws RecursoNoEncontradoException 404: sin persona mayor, o el recuerdo no existe o es de otra persona (5.2)
     */
    public RecuerdoDetalleResponse detalle(Long cuentaId, String recuerdoId) {
        DetalleDeRecuerdo detalle = lectura.detalleDeRecuerdo(personaId(cuentaId), recuerdoId)
                .orElseThrow(RecursoNoEncontradoException::new);
        RecuerdoLeido r = detalle.recuerdo();
        return new RecuerdoDetalleResponse(r.id(), r.titulo(), r.descripcion(), r.anio(), precision(r),
                etapa(r),
                detalle.personas().stream()
                        .map(p -> new PersonaDeRecuerdoResponse(p.id(), p.nombre(), p.vinculo())).toList(),
                detalle.lugares().stream().map(l -> new LugarDeRecuerdoResponse(l.id(), l.nombre())).toList(),
                faltantes(r), detalle.preguntaTexto());
    }

    private RecuerdoResumenResponse resumen(RecuerdoLeido r) {
        return new RecuerdoResumenResponse(r.id(), r.titulo(), r.anio(), precision(r), etapa(r),
                TextoCortado.cortar(r.descripcion(), configuracion.getResumenMax()), faltantes(r));
    }

    /** Los datos que se pueden seguir completando (contrato del mapa de vida, 3.1): la misma regla de las repreguntas. */
    private List<DatoFaltante> faltantes(RecuerdoLeido r) {
        return SelectorDeRepregunta.faltantes(r.paraRepreguntar(), configuracion.getIntentosMax());
    }

    /** La precisión del recuerdo, o {@code null} si no tiene o el valor guardado en el grafo no es válido. */
    private static PrecisionAnio precision(RecuerdoLeido r) {
        return aEnum(PrecisionAnio.class, r.precision());
    }

    /** La etapa del recuerdo, o {@code null} si no tiene o el valor guardado en el grafo no es válido. */
    private static EtapaDeVida etapa(RecuerdoLeido r) {
        return aEnum(EtapaDeVida.class, r.etapaDeVida());
    }

    /**
     * Un valor del grafo inválido (por ejemplo, cargado a mano en Neo4j) no tumba el listado: se muestra el recuerdo
     * sin ese dato. Se deja sin loguear el valor.
     */
    private static <E extends Enum<E>> E aEnum(Class<E> tipo, String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long personaId(Long cuentaId) {
        return personaMayorRepository.findByCuentaId(cuentaId).orElseThrow(RecursoNoEncontradoException::new).getId();
    }
}

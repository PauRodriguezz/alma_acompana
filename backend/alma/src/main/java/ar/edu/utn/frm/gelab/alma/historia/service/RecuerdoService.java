package ar.edu.utn.frm.gelab.alma.historia.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.domain.DatosParaCompletar;
import ar.edu.utn.frm.gelab.alma.historia.domain.NombreNormalizado;
import ar.edu.utn.frm.gelab.alma.historia.domain.NuevoRecuerdo;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.CalculoDeAnio;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.CalculoDeAnio.AnioDelRecuerdo;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.PersonaExtraida;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.RespuestaExtraida;
import ar.edu.utn.frm.gelab.alma.historia.extraccion.TrabajoDeExtraccion;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;

/**
 * Escribe el recuerdo que salió de una respuesta (contrato del grafo, 5.6): decide el año, la precisión y la
 * etapa de vida, quita repetidos y deja la escritura, en una sola transacción de Neo4j, a
 * {@link EscrituraDelGrafo#registrarRecuerdo}. El log no lleva texto de la persona (5.9).
 */
@Service
public class RecuerdoService {

    private final EscrituraDelGrafo escritura;
    private final Clock clock;

    public RecuerdoService(EscrituraDelGrafo escritura, Clock clock) {
        this.escritura = escritura;
        this.clock = clock;
    }

    /** @return el id del recuerdo creado */
    public String guardar(TrabajoDeExtraccion trabajo, RespuestaExtraida respuesta) {
        AnioDelRecuerdo anio = CalculoDeAnio.calcular(respuesta.anio(), respuesta.anioAproximado(),
                respuesta.edadMencionada(), trabajo.edadDesde(), trabajo.edadHasta(),
                trabajo.fechaNacimiento().getYear(), LocalDate.now(clock).getYear());
        NuevoRecuerdo nuevo = new NuevoRecuerdo(trabajo.preguntaId(), respuesta.titulo(), respuesta.descripcion(),
                anio.anio(), anio.precision(), anio.etapa(), personasSinRepetir(respuesta.personas()),
                lugaresSinRepetir(respuesta.lugares()));
        return escritura.registrarRecuerdo(trabajo.personaId(), nuevo);
    }

    /**
     * Aplica la respuesta de una repregunta a un recuerdo ya creado (contrato, 5.7): usa solo lo que corresponde al
     * dato preguntado, y nunca el título ni la descripción. Para {@code ANIO} acepta un año o una edad (misma regla
     * de 5.6, sin franja de edad: una repregunta nunca estima); si la respuesta no aporta ninguno, no cambia nada.
     */
    public void completar(TrabajoDeExtraccion trabajo, RespuestaExtraida respuesta) {
        TrabajoDeExtraccion.Repregunta repregunta = trabajo.repregunta();
        AnioDelRecuerdo anio = repregunta.dato() == DatoFaltante.ANIO
                ? CalculoDeAnio.calcular(respuesta.anio(), respuesta.anioAproximado(), respuesta.edadMencionada(),
                        null, null, trabajo.fechaNacimiento().getYear(), LocalDate.now(clock).getYear())
                : null;
        escritura.completarRecuerdo(trabajo.personaId(), repregunta.recuerdoId(),
                new DatosParaCompletar(repregunta.dato(), anio != null ? anio.anio() : null,
                        anio != null ? anio.precision() : null, anio != null ? anio.etapa() : null,
                        personasSinRepetir(respuesta.personas()), lugaresSinRepetir(respuesta.lugares())));
    }

    /** Las personas con nombre repetido (mismo {@code nombreNormalizado}) cuentan una sola vez. */
    private static List<NuevoRecuerdo.PersonaNueva> personasSinRepetir(List<PersonaExtraida> personas) {
        Map<String, NuevoRecuerdo.PersonaNueva> conNombre = new LinkedHashMap<>();
        List<NuevoRecuerdo.PersonaNueva> sinNombre = new ArrayList<>();
        for (PersonaExtraida p : personas) {
            String normalizado = NombreNormalizado.de(p.nombre());
            if (normalizado == null || normalizado.isEmpty()) {
                sinNombre.add(new NuevoRecuerdo.PersonaNueva(null, p.vinculo()));
            } else {
                conNombre.putIfAbsent(normalizado, new NuevoRecuerdo.PersonaNueva(p.nombre(), p.vinculo()));
            }
        }
        List<NuevoRecuerdo.PersonaNueva> resultado = new ArrayList<>(conNombre.values());
        resultado.addAll(sinNombre);
        return resultado;
    }

    private static List<String> lugaresSinRepetir(List<String> lugares) {
        Map<String, String> unicos = new LinkedHashMap<>();
        for (String lugar : lugares) {
            String normalizado = NombreNormalizado.de(lugar);
            if (normalizado != null && !normalizado.isEmpty()) {
                unicos.putIfAbsent(normalizado, lugar);
            }
        }
        return List.copyOf(unicos.values());
    }
}

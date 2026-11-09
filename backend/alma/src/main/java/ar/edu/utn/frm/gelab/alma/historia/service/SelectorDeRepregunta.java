package ar.edu.utn.frm.gelab.alma.historia.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.edu.utn.frm.gelab.alma.historia.domain.DatoFaltante;
import ar.edu.utn.frm.gelab.alma.historia.repository.RecuerdoParaRepreguntar;

/**
 * Elige la repregunta candidata (contrato del grafo, 5.7 y 7.5). Función pura: no toca ninguna base.
 *
 * <p>Un dato es candidato de un recuerdo si está en la lista {@code repreguntas} de su pregunta de origen, si
 * todavía falta y si se preguntó menos de {@code maximo} veces. Dentro de un recuerdo, el orden es el de esa
 * lista; entre recuerdos, gana el más antiguo ({@code fechaRegistro}) que tenga alguna candidata.
 */
public final class SelectorDeRepregunta {

    /** El recuerdo elegido y el dato por el que se pregunta. */
    public record Candidata(String recuerdoId, DatoFaltante dato) {
    }

    private SelectorDeRepregunta() {
    }

    public static Optional<Candidata> siguiente(List<RecuerdoParaRepreguntar> recuerdos, Set<DatoFaltante> excluir,
            int maximo) {
        return recuerdos.stream()
                .sorted(Comparator.comparing(RecuerdoParaRepreguntar::fechaRegistro,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RecuerdoParaRepreguntar::id))
                .map(recuerdo -> recuerdo.repreguntas().stream()
                        .filter(dato -> !excluir.contains(dato))
                        .filter(recuerdo::falta)
                        .filter(dato -> recuerdo.intentos(dato) < maximo)
                        .findFirst()
                        .map(dato -> new Candidata(recuerdo.id(), dato)))
                .flatMap(Optional::stream)
                .findFirst();
    }
}

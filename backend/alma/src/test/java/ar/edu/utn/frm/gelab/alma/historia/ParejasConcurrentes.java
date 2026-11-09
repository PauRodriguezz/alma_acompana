package ar.edu.utn.frm.gelab.alma.historia;

import static ar.edu.utn.frm.gelab.alma.historia.GrafoDePrueba.contar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.data.neo4j.core.Neo4jClient;

import ar.edu.utn.frm.gelab.alma.common.exception.ConflictoException;
import ar.edu.utn.frm.gelab.alma.historia.PersonasDeHistoria.Persona;
import ar.edu.utn.frm.gelab.alma.historia.service.HistoriaDeVidaService;

/**
 * Dos pedidos de la misma persona sobre la misma pregunta, a la vez (contrato del grafo, sección 2,
 * "Concurrencia en responder y saltear"). Una barrera suelta los dos hilos juntos. Por cada pregunta se
 * cuenta cómo terminaron y cuántas relaciones {@code RESPONDIO} o {@code SALTEO} quedaron.
 */
final class ParejasConcurrentes {

    enum Accion {
        RESPONDER, SALTEAR
    }

    /**
     * @param correctos      un pedido registrado y el otro con 409, y una sola relación
     * @param dosExitos      los dos pedidos se registraron (la carrera: regla de integridad 3 rota)
     * @param duplicadas     preguntas con más de una relación
     * @param errores        cualquier otro resultado (por ejemplo, un error de Neo4j)
     */
    record Resumen(String nombre, int pares, int correctos, int dosExitos, int duplicadas, int errores,
            List<String> detalleDeErrores) {

        int carreras() {
            return Math.max(dosExitos, duplicadas);
        }

        @Override
        public String toString() {
            return nombre + ": pares=" + pares + ", correctos=" + correctos + ", dos pedidos registrados=" + dosExitos
                    + ", preguntas con relación duplicada=" + duplicadas + ", errores=" + errores;
        }
    }

    private ParejasConcurrentes() {
    }

    static Resumen correr(String nombre, HistoriaDeVidaService historia, Neo4jClient neo4j, Persona persona,
            List<Integer> preguntas, Accion primera, Accion segunda) throws Exception {
        int correctos = 0;
        int dosExitos = 0;
        int duplicadas = 0;
        int errores = 0;
        List<String> detalle = new ArrayList<>();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int pregunta : preguntas) {
                CyclicBarrier barrera = new CyclicBarrier(2);
                Future<String> a = hilos.submit(() -> ejecutar(barrera, historia, persona, pregunta, primera));
                Future<String> b = hilos.submit(() -> ejecutar(barrera, historia, persona, pregunta, segunda));
                String resultadoA = a.get(60, TimeUnit.SECONDS);
                String resultadoB = b.get(60, TimeUnit.SECONDS);
                long relaciones = contar(neo4j, """
                        MATCH (:PersonaMayor {idPersonaMayor: $p})-[r:RESPONDIO|SALTEO]->(:PreguntaGuion {idPreguntaGuion: $q})
                        RETURN count(r) AS c
                        """, "p", persona.personaId(), "q", pregunta);
                boolean unExito = ("OK".equals(resultadoA) && "409".equals(resultadoB))
                        || ("409".equals(resultadoA) && "OK".equals(resultadoB));
                if ("OK".equals(resultadoA) && "OK".equals(resultadoB)) {
                    dosExitos++;
                } else if (unExito && relaciones == 1) {
                    correctos++;
                } else if (!unExito) {
                    errores++;
                    detalle.add("pregunta " + pregunta + ": " + resultadoA + " / " + resultadoB);
                }
                if (relaciones > 1) {
                    duplicadas++;
                }
            }
        } finally {
            hilos.shutdownNow();
        }
        return new Resumen(nombre, preguntas.size(), correctos, dosExitos, duplicadas, errores, detalle);
    }

    private static String ejecutar(CyclicBarrier barrera, HistoriaDeVidaService historia, Persona persona,
            int pregunta, Accion accion) throws Exception {
        barrera.await(30, TimeUnit.SECONDS);
        try {
            if (accion == Accion.RESPONDER) {
                historia.responder(persona.cuentaId(), pregunta, "Una respuesta.");
            } else {
                historia.saltear(persona.cuentaId(), pregunta);
            }
            return "OK";
        } catch (ConflictoException e) {
            return "409";
        } catch (RuntimeException e) {
            return "ERROR " + e.getClass().getSimpleName();
        }
    }
}

package ar.edu.utn.frm.gelab.alma.historia.repository;

/**
 * Nombre de una persona o de un lugar del grafo (consultas P16 y P17), con el {@code nombreNormalizado} guardado:
 * P13 y P14 buscan por ese valor.
 */
public record NombreConocido(String nombre, String nombreNormalizado) {
}

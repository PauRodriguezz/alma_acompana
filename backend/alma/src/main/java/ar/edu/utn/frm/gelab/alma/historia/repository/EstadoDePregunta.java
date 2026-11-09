package ar.edu.utn.frm.gelab.alma.historia.repository;

import java.time.ZonedDateTime;

/**
 * Una fila de la consulta P4 del modelo de grafo: una pregunta del guion y, si la persona la respondió o
 * la salteó, el tipo de relación ({@code RESPONDIO} o {@code SALTEO}) y su fecha. {@code tipo} y
 * {@code fecha} son nulos en las pendientes.
 */
public record EstadoDePregunta(Integer id, Integer orden, String texto, String tipo, ZonedDateTime fecha) {
}

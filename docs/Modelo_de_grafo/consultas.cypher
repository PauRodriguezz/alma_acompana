// Consultas P1 a P17 del modelo de grafo, para la persona mayor con idPersonaMayor: 1.
// Para probar el aislamiento entre personas, reemplazar el 1 por el 2 (consultas P6, P13 y P14).
// Ejecutar de a una en Neo4j Browser. Toda consulta parte del nodo PersonaMayor.

// ---- CU-03: guion y progreso ----

// P1 – Próxima pregunta pendiente
MATCH (p:PersonaMayor {idPersonaMayor: 1})
MATCH (q:PreguntaGuion)
WHERE NOT EXISTS { (p)-[:RESPONDIO|SALTEO]->(q) }
RETURN q.idPreguntaGuion, q.texto ORDER BY q.orden LIMIT 1;

// P2 – ¿Hay preguntas pendientes?
MATCH (p:PersonaMayor {idPersonaMayor: 1})
RETURN EXISTS {
  MATCH (q:PreguntaGuion) WHERE NOT EXISTS { (p)-[:RESPONDIO|SALTEO]->(q) }
} AS hayPendientes;

// P3 – Última pregunta respondida o salteada
MATCH (:PersonaMayor {idPersonaMayor: 1})-[r:RESPONDIO|SALTEO]->(q:PreguntaGuion)
RETURN q.idPreguntaGuion, q.texto, r.fecha, type(r) AS tipo
ORDER BY r.fecha DESC LIMIT 1;

// P4 – Estado de todas las preguntas
MATCH (p:PersonaMayor {idPersonaMayor: 1})
MATCH (q:PreguntaGuion)
OPTIONAL MATCH (p)-[r:RESPONDIO|SALTEO]->(q)
RETURN q.orden, q.texto,
  CASE type(r) WHEN 'RESPONDIO' THEN 'respondida' WHEN 'SALTEO' THEN 'salteada' ELSE 'pendiente' END AS estado
ORDER BY q.orden;

// P5 – Recuerdos que surgieron de una pregunta (pregunta 2)
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(:PreguntaGuion {idPreguntaGuion: 2})
RETURN r.idRecuerdo, r.titulo;

// ---- CU-03: construcción del grafo (evitar duplicados) ----

// P6 – ¿Existe una persona con este nombre?
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:CONOCE]->(pe:Persona {nombreNormalizado: 'marta'})
RETURN pe;

// P7 – ¿Existe una persona con este vínculo?
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:CONOCE {vinculo: 'hermana'}]->(pe:Persona)
RETURN pe;

// P8 – ¿Existe este lugar?
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(l:Lugar {nombreNormalizado: 'cipolletti'})
RETURN DISTINCT l;

// P9 – Vínculo con una persona
MATCH (:PersonaMayor {idPersonaMayor: 1})-[c:CONOCE]->(:Persona {nombreNormalizado: 'marta'})
RETURN c.vinculo;

// ---- CU-04: línea de tiempo ----

// P10 – Recuerdos en orden cronológico (los sin año quedan al final)
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(r:Recuerdo)
RETURN r.idRecuerdo, r.titulo, r.anio, r.precision
ORDER BY r.anio;

// P11 – ¿Tiene recuerdos?
MATCH (p:PersonaMayor {idPersonaMayor: 1})
RETURN EXISTS { (p)-[:TIENE_RECUERDO]->(:Recuerdo) } AS tieneRecuerdos;

// P12 – Detalle de un recuerdo (recuerdo 1)
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(r:Recuerdo {idRecuerdo: 1})
OPTIONAL MATCH (pe:Persona)-[:PARTICIPA_EN]->(r)
OPTIONAL MATCH (r)-[:OCURRIO_EN]->(l:Lugar)
RETURN r.titulo, r.descripcion, r.anio, r.precision,
  collect(DISTINCT pe.nombre) AS personas, collect(DISTINCT l.nombre) AS lugares;

// ---- CU-02: personalización del chat ----

// P13 – Recuerdos de una persona
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:CONOCE]->(:Persona {nombreNormalizado: 'marta'})-[:PARTICIPA_EN]->(r:Recuerdo)
RETURN r.titulo, r.descripcion, r.anio;

// P14 – Recuerdos de un lugar
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:OCURRIO_EN]->(:Lugar {nombreNormalizado: 'cipolletti'})
RETURN r.titulo, r.descripcion, r.anio;

// P15 – Recuerdos de una etapa de vida
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(r:Recuerdo {etapaDeVida: 'INFANCIA'})
RETURN r.titulo, r.descripcion, r.anio;

// P16 – Personas de su vida y vínculo
MATCH (:PersonaMayor {idPersonaMayor: 1})-[c:CONOCE]->(pe:Persona)
RETURN pe.nombre, c.vinculo;

// P17 – Lugares de su historia
MATCH (:PersonaMayor {idPersonaMayor: 1})-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(l:Lugar)
RETURN DISTINCT l.nombre;

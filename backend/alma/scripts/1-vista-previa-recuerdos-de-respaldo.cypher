// =====================================================================================
// SOLO PARA DESARROLLO. NO CORRER EN UN ENTORNO REAL.
//
// Paso 1 de 2: VISTA PREVIA (solo lectura, no modifica nada).
//
// Lista los recuerdos "de respaldo" del extractor (contrato del grafo, 5.4, punto 7): los que se
// crearon con el texto de la pregunta como título porque el modelo no respondió. Un recuerdo
// entra si su título es igual al texto de la pregunta de la que surgió (SURGIO_DE).
//
// No filtra por persona: muestra a todos los usuarios afectados. Corrida de nuevo después del
// paso 2, tiene que devolver cero filas.
//
// Cómo correrlo: en el navegador de Neo4j (http://localhost:7474) o con cypher-shell.
// =====================================================================================

MATCH (p:PersonaMayor)-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
WHERE r.titulo = q.texto
RETURN p.idPersonaMayor AS idPersonaMayor,
       q.idPreguntaGuion AS pregunta,
       r.idRecuerdo AS idRecuerdo,
       r.fechaRegistro AS fechaRegistro,
       EXISTS { (p)-[:RESPONDIO]->(q) } AS tieneRespondio
ORDER BY idPersonaMayor, pregunta;

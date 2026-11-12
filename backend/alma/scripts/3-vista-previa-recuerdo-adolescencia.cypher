// =====================================================================================
// SOLO PARA DESARROLLO. NO CORRER EN UN ENTORNO REAL.
//
// Paso 1 de 2: VISTA PREVIA (solo lectura, no modifica nada).
//
// Lista los recuerdos de prueba titulados "Los tiempos de la adolescencia" (creados cuando una respuesta "No me
// acuerdo" todavía generaba un recuerdo vacío; ahora se trata como un salteo, contrato del grafo, 5.1). No filtra por
// persona: muestra todos los que hay. Revisá el idPersonaMayor y el idRecuerdo de cada fila antes de pasar al paso 2.
//
// Cómo correrlo: en el navegador de Neo4j (http://localhost:7474) o con cypher-shell.
// =====================================================================================

MATCH (p:PersonaMayor)-[:TIENE_RECUERDO]->(r:Recuerdo {titulo: 'Los tiempos de la adolescencia'})
OPTIONAL MATCH (r)-[:SURGIO_DE]->(q:PreguntaGuion)
RETURN p.idPersonaMayor AS idPersonaMayor,
       q.idPreguntaGuion AS pregunta,
       r.idRecuerdo AS idRecuerdo,
       r.fechaRegistro AS fechaRegistro,
       r.descripcion AS descripcion,
       EXISTS { (p)-[:RESPONDIO]->(q) } AS tieneRespondio,
       EXISTS { (p)-[:SALTEO]->(q) } AS tieneSalteo,
       EXISTS { (r)<-[:PARTICIPA_EN]-(:Persona) } AS tienePersonas,
       EXISTS { (r)-[:OCURRIO_EN]->(:Lugar) } AS tieneLugares
ORDER BY idPersonaMayor, pregunta;

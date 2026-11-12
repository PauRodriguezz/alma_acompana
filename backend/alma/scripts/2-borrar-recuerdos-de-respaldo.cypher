// =====================================================================================
// SOLO PARA DESARROLLO. NO CORRER EN UN ENTORNO REAL. BORRA DATOS.
//
// Paso 2 de 2: BORRADO. Correr siempre después de revisar la vista previa
// (1-vista-previa-recuerdos-de-respaldo.cypher).
//
// Para UNA persona (parámetro $persona = su idPersonaMayor), borra:
//   - los recuerdos de respaldo: título igual al texto de la pregunta de la que surgieron;
//   - la relación RESPONDIO de esa persona a esas mismas preguntas (así Alma vuelve a hacerlas);
//   - los nodos Persona y Lugar de esos recuerdos, solo si quedan sin ninguna relación con
//     otro recuerdo (no toca los que siguen en uso, ni nodos de otras personas).
// No toca las preguntas del guion, los SALTEO ni los recuerdos que no cumplen la condición.
//
// Cómo correrlo (el parámetro es obligatorio; sin él, la consulta falla y no borra nada):
//   Navegador de Neo4j (http://localhost:7474):   :param persona => 1
//   cypher-shell:                                 :param persona => 1;
// y después ejecutar la consulta.
//
// Devuelve cuántos borró. Para el caso de la persona 1 se esperan 7 recuerdos y 7 relaciones.
// Es seguro repetirlo: la segunda vez devuelve todo en 0.
// =====================================================================================

MATCH (p:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
WHERE r.titulo = q.texto
OPTIONAL MATCH (p)-[resp:RESPONDIO]->(q)
OPTIONAL MATCH (pe:Persona)-[:PARTICIPA_EN]->(r)
OPTIONAL MATCH (r)-[:OCURRIO_EN]->(l:Lugar)
WITH collect(DISTINCT r) AS recuerdos,
     collect(DISTINCT resp) AS respondidas,
     collect(DISTINCT pe) AS personas,
     collect(DISTINCT l) AS lugares
WITH recuerdos, respondidas, personas, lugares,
     size(recuerdos) AS recuerdosBorrados,
     size(respondidas) AS relacionesBorradas
FOREACH (x IN respondidas | DELETE x)
FOREACH (x IN recuerdos | DETACH DELETE x)
WITH personas, lugares, recuerdosBorrados, relacionesBorradas
CALL (personas) {
  UNWIND personas AS pe
  WITH pe
  WHERE NOT EXISTS { (pe)-[:PARTICIPA_EN]->() }
  DETACH DELETE pe
  RETURN count(*) AS personasHuerfanasBorradas
}
CALL (lugares) {
  UNWIND lugares AS l
  WITH l
  WHERE NOT EXISTS { (l)<-[:OCURRIO_EN]-() }
  DETACH DELETE l
  RETURN count(*) AS lugaresHuerfanosBorrados
}
RETURN recuerdosBorrados, relacionesBorradas, personasHuerfanasBorradas, lugaresHuerfanosBorrados;

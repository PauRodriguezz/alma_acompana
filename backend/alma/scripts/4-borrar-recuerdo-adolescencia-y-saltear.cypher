// =====================================================================================
// SOLO PARA DESARROLLO. NO CORRER EN UN ENTORNO REAL. BORRA DATOS.
//
// Paso 2 de 2: BORRADO Y SALTEO. Correr siempre después de revisar la vista previa
// (3-vista-previa-recuerdo-adolescencia.cypher).
//
// Para UNA persona y UN recuerdo (parámetros $persona = idPersonaMayor y $recuerdo = idRecuerdo, los de la vista
// previa), solo si el recuerdo se titula "Los tiempos de la adolescencia":
//   - borra el recuerdo;
//   - cambia la relación RESPONDIO de esa persona a la pregunta de origen por una SALTEO, con la misma fecha (así la
//     pregunta no se vuelve a ofrecer, igual que una respondida con "no me acuerdo" desde que se trata como salteo);
//   - borra los nodos Persona y Lugar de ese recuerdo, solo si quedan sin ninguna relación con otro recuerdo.
// No toca las preguntas del guion, otros recuerdos ni otras personas.
//
// Cómo correrlo (los dos parámetros son obligatorios; sin ellos la consulta falla y no borra nada):
//   Navegador de Neo4j (http://localhost:7474):   :param persona => 1
//                                                 :param recuerdo => 'el-id-de-la-vista-previa'
//   cypher-shell:                                 :param persona => 1;
//                                                 :param recuerdo => 'el-id-de-la-vista-previa';
// y después ejecutar la consulta.
//
// Devuelve cuántos borró y cuántas relaciones cambió. Sin filas: no había nada que borrar (el recuerdo no existe, no es
// de esa persona o no se titula así). Es seguro repetirlo: la segunda vez no devuelve filas.
// =====================================================================================

MATCH (p:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo {idRecuerdo: $recuerdo, titulo: 'Los tiempos de la adolescencia'})-[:SURGIO_DE]->(q:PreguntaGuion)
OPTIONAL MATCH (p)-[resp:RESPONDIO]->(q)
OPTIONAL MATCH (pe:Persona)-[:PARTICIPA_EN]->(r)
OPTIONAL MATCH (r)-[:OCURRIO_EN]->(l:Lugar)
WITH p, q, r,
     collect(DISTINCT resp) AS respondidas,
     collect(DISTINCT pe) AS personas,
     collect(DISTINCT l) AS lugares
WITH p, q, r, respondidas, personas, lugares,
     size(respondidas) AS relacionesCambiadas,
     coalesce(head([x IN respondidas | x.fecha]), datetime()) AS fecha
FOREACH (x IN respondidas | DELETE x)
DETACH DELETE r
WITH p, q, personas, lugares, relacionesCambiadas, fecha
MERGE (p)-[s:SALTEO]->(q)
  ON CREATE SET s.fecha = fecha
WITH personas, lugares, relacionesCambiadas
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
RETURN 1 AS recuerdosBorrados, relacionesCambiadas, personasHuerfanasBorradas, lugaresHuerfanosBorrados;

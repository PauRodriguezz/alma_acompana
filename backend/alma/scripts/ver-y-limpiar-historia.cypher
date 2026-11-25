// =====================================================================================
// VER Y LIMPIAR LA HISTORIA DE VIDA (Neo4j)
// SOLO PARA DESARROLLO Y DEMO. NO CORRER EN UN ENTORNO REAL. La sección 3 BORRA DATOS.
//
// Un solo archivo, tres secciones. Se corre DE A UNA: en el Navegador de Neo4j
// (http://localhost:7474) pegá una consulta, ejecutala, mirá el resultado y recién ahí seguí.
//
//   1. VER           (solo lectura)  V1 a V4
//   2. VISTA PREVIA  (solo lectura)  qué borraría la sección 3 para una persona
//   3. BORRADO       (borra)         la historia de UNA persona
//
// PARÁMETROS (se definen en el Navegador, una línea por vez, ejecutándola sola):
//   :param persona => 1            id de la persona mayor (el mismo id que en PostgreSQL). Obligatorio
//                                  en V4, en la vista previa y en el borrado: sin él, la consulta falla
//                                  y no hace nada.
//   :param confirmar => true       solo para el borrado. Sin él, el borrado falla y no borra nada.
//                                  Después de borrar: :param confirmar => false
//
// Para saber el id de cada persona (PostgreSQL, base "alma"):
//   SELECT p.id, c.nombre_usuario FROM persona_mayor p JOIN cuenta c ON c.id = p.cuenta_id;
// También sirve la consulta V3, que lista las personas que existen en el grafo.
//
// QUÉ SE TOCA Y QUÉ NO (docs/Modelo_de_grafo/modelo-de-grafo.md, reglas 1 y 8):
//   Se borra, de UNA persona: sus recuerdos, sus relaciones RESPONDIO y SALTEO (así el guion
//   vuelve a empezar), sus relaciones CONOCE, y los nodos Persona y Lugar que queden sin ninguna
//   relación con otro recuerdo o con otra persona mayor.
//   No se toca: las PreguntaGuion (el guion es compartido), el nodo PersonaMayor, ni nada de
//   otras personas. Los nodos Persona y Lugar que sigan en uso por otro recuerdo no se borran.
//
// SEGURO DE REPETIR: correr el borrado dos veces no rompe nada; la segunda devuelve todo en 0.
// Si la persona no tiene nodo en el grafo, la vista previa y el borrado no devuelven filas.
//
// IMPORTANTE: si el back arranca con alma.seed.enabled=true y "ana" queda sin recuerdos ni
// progreso, la carga inicial le vuelve a cargar la historia de ejemplo. Apagá el seed
// (ALMA_SEED_ENABLED=false en el .env) antes de limpiar a "ana" y antes de volver a arrancar el back.
// =====================================================================================


// =====================================================================================
// 1. VER (solo lectura)
// =====================================================================================

// ---- V1. Grafo completo, con tope de 300 (incluye el guion y a todas las personas) ----
MATCH (n)
OPTIONAL MATCH (n)-[r]->(m)
RETURN n, r, m
LIMIT 300;


// ---- V2. Contadores por tipo de nodo y de relación ----
CALL {
  MATCH (n)
  UNWIND labels(n) AS tipo
  RETURN 'nodo' AS clase, tipo, count(*) AS cantidad
  UNION ALL
  MATCH ()-[r]->()
  RETURN 'relación' AS clase, type(r) AS tipo, count(*) AS cantidad
}
RETURN clase, tipo, cantidad
ORDER BY clase DESC, tipo;


// ---- V3. Personas del grafo, con la cantidad de recuerdos y de preguntas ----
MATCH (p:PersonaMayor)
RETURN p.idPersonaMayor AS persona,
       COUNT { (p)-[:TIENE_RECUERDO]->() } AS recuerdos,
       COUNT { (p)-[:RESPONDIO]->() } AS preguntasRespondidas,
       COUNT { (p)-[:SALTEO]->() } AS preguntasSalteadas,
       COUNT { (p)-[:CONOCE]->() } AS personasConocidas
ORDER BY persona;


// ---- V4. Grafo de UNA persona ($persona), partiendo de su PersonaMayor ----
// Recuerdos, personas mencionadas y lugares de esa persona. No trae el guion (las PreguntaGuion)
// ni nada de otras personas: todo parte del nodo PersonaMayor.
MATCH (p:PersonaMayor {idPersonaMayor: $persona})
CALL (p) {
  MATCH ruta = (p)-[:TIENE_RECUERDO]->(:Recuerdo) RETURN ruta
  UNION
  MATCH ruta = (p)-[:CONOCE]->(:Persona) RETURN ruta
  UNION
  MATCH ruta = (p)-[:TIENE_RECUERDO]->(:Recuerdo)-[:OCURRIO_EN]->(:Lugar) RETURN ruta
  UNION
  MATCH ruta = (p)-[:TIENE_RECUERDO]->(:Recuerdo)<-[:PARTICIPA_EN]-(:Persona) RETURN ruta
}
RETURN ruta;


// =====================================================================================
// 2. VISTA PREVIA DEL BORRADO (solo lectura)
// Mismas reglas que la sección 3. Mirá los números y los títulos antes de borrar.
// =====================================================================================

MATCH (p:PersonaMayor {idPersonaMayor: $persona})
OPTIONAL MATCH (p)-[:TIENE_RECUERDO]->(r:Recuerdo)
WITH p, collect(DISTINCT r) AS recuerdos
WITH p, recuerdos,
     COUNT { (p)-[:RESPONDIO]->(:PreguntaGuion) } AS respondidas,
     COUNT { (p)-[:SALTEO]->(:PreguntaGuion) } AS salteadas,
     COUNT { (p)-[:CONOCE]->(:Persona) } AS relacionesConoce,
     COLLECT {
       MATCH (pe:Persona)
       WHERE (p)-[:CONOCE]->(pe) OR any(x IN recuerdos WHERE (pe)-[:PARTICIPA_EN]->(x))
       RETURN DISTINCT pe
     } AS personas,
     COLLECT {
       UNWIND recuerdos AS x
       MATCH (x)-[:OCURRIO_EN]->(l:Lugar)
       RETURN DISTINCT l
     } AS lugares
RETURN p.idPersonaMayor AS persona,
       size(recuerdos) AS recuerdosAborrar,
       [x IN recuerdos | x.titulo] AS titulos,
       respondidas AS respondidasAborrar,
       salteadas AS salteadasAborrar,
       relacionesConoce AS conoceAborrar,
       size([pe IN personas WHERE
             NOT EXISTS { (pe)-[:PARTICIPA_EN]->(o:Recuerdo) WHERE NOT o IN recuerdos }
             AND NOT EXISTS { (otra:PersonaMayor)-[:CONOCE]->(pe) WHERE otra <> p }]) AS personasAborrar,
       size([l IN lugares WHERE
             NOT EXISTS { (l)<-[:OCURRIO_EN]-(o:Recuerdo) WHERE NOT o IN recuerdos }]) AS lugaresAborrar;


// =====================================================================================
// 3. BORRADO DE LA HISTORIA DE UNA PERSONA (BORRA DATOS)
// Correr solo después de revisar la vista previa. Necesita :param persona y :param confirmar => true.
// Devuelve cuántos nodos y relaciones borró.
// =====================================================================================

MATCH (p:PersonaMayor {idPersonaMayor: $persona})
WHERE $confirmar = true
OPTIONAL MATCH (p)-[:TIENE_RECUERDO]->(r:Recuerdo)
WITH p, collect(DISTINCT r) AS recuerdos
OPTIONAL MATCH (p)-[prog:RESPONDIO|SALTEO]->(:PreguntaGuion)
WITH p, recuerdos, collect(DISTINCT prog) AS progreso
OPTIONAL MATCH (p)-[c:CONOCE]->(:Persona)
WITH p, recuerdos, progreso, collect(DISTINCT c) AS conoce
WITH p, recuerdos, progreso, conoce,
     COLLECT {
       MATCH (pe:Persona)
       WHERE (p)-[:CONOCE]->(pe) OR any(x IN recuerdos WHERE (pe)-[:PARTICIPA_EN]->(x))
       RETURN DISTINCT pe
     } AS personas,
     COLLECT {
       UNWIND recuerdos AS x
       MATCH (x)-[:OCURRIO_EN]->(l:Lugar)
       RETURN DISTINCT l
     } AS lugares
WITH personas, lugares,
     size(recuerdos) AS recuerdosBorrados,
     size(progreso) AS relacionesDeProgresoBorradas,
     size(conoce) AS relacionesConoceBorradas,
     recuerdos, progreso, conoce
FOREACH (x IN progreso | DELETE x)
FOREACH (x IN conoce | DELETE x)
FOREACH (x IN recuerdos | DETACH DELETE x)
WITH personas, lugares, recuerdosBorrados, relacionesDeProgresoBorradas, relacionesConoceBorradas
CALL (personas) {
  UNWIND personas AS pe
  WITH pe
  WHERE NOT EXISTS { (pe)-[:PARTICIPA_EN]->() } AND NOT EXISTS { (:PersonaMayor)-[:CONOCE]->(pe) }
  DETACH DELETE pe
  RETURN count(*) AS personasBorradas
}
CALL (lugares) {
  UNWIND lugares AS l
  WITH l
  WHERE NOT EXISTS { (l)<-[:OCURRIO_EN]-() }
  DETACH DELETE l
  RETURN count(*) AS lugaresBorrados
}
RETURN recuerdosBorrados, relacionesDeProgresoBorradas, relacionesConoceBorradas,
       personasBorradas, lugaresBorrados;

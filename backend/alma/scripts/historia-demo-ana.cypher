// =====================================================================================
// HISTORIA DE VIDA DE DEMO PARA "ana" (Neo4j)
// SOLO PARA DESARROLLO Y DEMO. No correr en un entorno real.
//
// Carga 18 recuerdos (preguntas 1 a 18 respondidas) con sus personas y lugares, y deja las
// preguntas 19, 20 y 21 sin responder: Alma empieza a preguntar por la 19.
// Ana nació el 12/05/1950 (seed); año, precisión y etapa de vida están calculados con ese año.
//
// CÓMO SE CORRE (en el Navegador de Neo4j, http://localhost:7474):
//   1. Definí el id de Ana (el mismo que en PostgreSQL):  :param persona => 1
//      (SELECT p.id FROM persona_mayor p JOIN cuenta c ON c.id = p.cuenta_id
//       WHERE c.nombre_usuario = 'ana';  también sirve la consulta V3 de ver-y-limpiar-historia.cypher)
//   2. Ejecutá la sección 1 (CARGA), sola.
//   3. Ejecutá las consultas de la sección 2 (VERIFICAR) de a una.
//
// ATÓMICO E IDEMPOTENTE: la carga es una sola sentencia. Si Ana no existe en el grafo, si ya
// tiene recuerdos o progreso (TIENE_RECUERDO, RESPONDIO o SALTEO), o si falta alguna de las
// preguntas 1 a 18 del guion, no devuelve filas y NO crea nada. Para recargar, primero
// limpiá la historia con ver-y-limpiar-historia.cypher.
//
// Necesita el nodo PersonaMayor de Ana (lo crea la app la primera vez que usa el módulo) y el
// guion cargado (lo carga el back al arrancar). Si Ana nunca entró, creala con:
//   MERGE (:PersonaMayor {idPersonaMayor: <id>});
//
// NOTA: con ALMA_SEED_ENABLED=true el back solo carga su historia de ejemplo si Ana no tiene
// recuerdos ni progreso: con esta carga hecha, no la pisa.
//
// ESTA CARGA NO LLAMA AL MODELO DE IA. Los recuerdos ya vienen completos (año, personas y
// lugares), así que no queda ninguna repregunta pendiente.
// =====================================================================================


// =====================================================================================
// 1. CARGA (escribe datos). Necesita :param persona
// =====================================================================================

MATCH (p:PersonaMayor {idPersonaMayor: $persona})
WHERE NOT EXISTS { (p)-[:TIENE_RECUERDO|RESPONDIO|SALTEO]->() }
MATCH (q1:PreguntaGuion {idPreguntaGuion: 1})
MATCH (q2:PreguntaGuion {idPreguntaGuion: 2})
MATCH (q3:PreguntaGuion {idPreguntaGuion: 3})
MATCH (q4:PreguntaGuion {idPreguntaGuion: 4})
MATCH (q5:PreguntaGuion {idPreguntaGuion: 5})
MATCH (q6:PreguntaGuion {idPreguntaGuion: 6})
MATCH (q7:PreguntaGuion {idPreguntaGuion: 7})
MATCH (q8:PreguntaGuion {idPreguntaGuion: 8})
MATCH (q9:PreguntaGuion {idPreguntaGuion: 9})
MATCH (q10:PreguntaGuion {idPreguntaGuion: 10})
MATCH (q11:PreguntaGuion {idPreguntaGuion: 11})
MATCH (q12:PreguntaGuion {idPreguntaGuion: 12})
MATCH (q13:PreguntaGuion {idPreguntaGuion: 13})
MATCH (q14:PreguntaGuion {idPreguntaGuion: 14})
MATCH (q15:PreguntaGuion {idPreguntaGuion: 15})
MATCH (q16:PreguntaGuion {idPreguntaGuion: 16})
MATCH (q17:PreguntaGuion {idPreguntaGuion: 17})
MATCH (q18:PreguntaGuion {idPreguntaGuion: 18})
WITH p, datetime() AS ahora, q1, q2, q3, q4, q5, q6, q7, q8, q9, q10, q11, q12, q13, q14, q15, q16, q17, q18
CREATE
 (rosa:Persona {idPersona: randomUUID(), nombre: 'Rosa', nombreNormalizado: 'rosa'}),
 (p)-[:CONOCE {vinculo: 'madre'}]->(rosa),
 (hugo:Persona {idPersona: randomUUID(), nombre: 'Hugo', nombreNormalizado: 'hugo'}),
 (p)-[:CONOCE {vinculo: 'padre'}]->(hugo),
 (marta:Persona {idPersona: randomUUID(), nombre: 'Marta', nombreNormalizado: 'marta'}),
 (p)-[:CONOCE {vinculo: 'hermana'}]->(marta),
 (elena:Persona {idPersona: randomUUID(), nombre: 'Elena', nombreNormalizado: 'elena'}),
 (p)-[:CONOCE {vinculo: 'maestra'}]->(elena),
 (silvia:Persona {idPersona: randomUUID(), nombre: 'Silvia', nombreNormalizado: 'silvia'}),
 (p)-[:CONOCE {vinculo: 'amiga'}]->(silvia),
 (norma:Persona {idPersona: randomUUID(), nombre: 'Norma', nombreNormalizado: 'norma'}),
 (p)-[:CONOCE {vinculo: 'amiga'}]->(norma),
 (roberto:Persona {idPersona: randomUUID(), nombre: 'Roberto', nombreNormalizado: 'roberto'}),
 (p)-[:CONOCE {vinculo: 'marido'}]->(roberto),
 (martin:Persona {idPersona: randomUUID(), nombre: 'Martín', nombreNormalizado: 'martin'}),
 (p)-[:CONOCE {vinculo: 'hijo'}]->(martin),
 (teresa:Persona {idPersona: randomUUID(), nombre: 'Teresa', nombreNormalizado: 'teresa'}),
 (p)-[:CONOCE {vinculo: 'jefa'}]->(teresa),
 (hermanos:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'hermanos'}]->(hermanos),
 (vecinos:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'vecinos'}]->(vecinos),
 (amigosb:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'amigos del barrio'}]->(amigosb),
 (amigasb:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'amigas del barrio'}]->(amigasb),
 (compas:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'compañeras'}]->(compas),
 (maestros:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'maestros'}]->(maestros),
 (novio:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'novio'}]->(novio),
 (compast:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'compañeras de trabajo'}]->(compast),
 (hijos:Persona {idPersona: randomUUID()}),
 (p)-[:CONOCE {vinculo: 'hijos'}]->(hijos),
 (l_cipolletti:Lugar {idLugar: randomUUID(), nombre: 'Cipolletti', nombreNormalizado: 'cipolletti'}),
 (l_casasm:Lugar {idLugar: randomUUID(), nombre: 'Casa de la calle San Martín', nombreNormalizado: 'casa de la calle san martin'}),
 (l_escuela:Lugar {idLugar: randomUUID(), nombre: 'Escuela N° 12', nombreNormalizado: 'escuela n° 12'}),
 (l_mendoza:Lugar {idLugar: randomUUID(), nombre: 'Mendoza', nombreNormalizado: 'mendoza'}),
 (l_lasheras:Lugar {idLugar: randomUUID(), nombre: 'Las Heras', nombreNormalizado: 'las heras'}),
 (l_centro:Lugar {idLugar: randomUUID(), nombre: 'Centro de Mendoza', nombreNormalizado: 'centro de mendoza'}),
 (l_clubes:Lugar {idLugar: randomUUID(), nombre: 'Clubes y salones de baile de Mendoza', nombreNormalizado: 'clubes y salones de baile de mendoza'}),
 (l_local:Lugar {idLugar: randomUUID(), nombre: 'Local de ropa en el centro de Mendoza', nombreNormalizado: 'local de ropa en el centro de mendoza'}),
 (l_godoy:Lugar {idLugar: randomUUID(), nombre: 'Godoy Cruz', nombreNormalizado: 'godoy cruz'}),
 (l_casasilvia:Lugar {idLugar: randomUUID(), nombre: 'Casa de Silvia, Las Heras', nombreNormalizado: 'casa de silvia, las heras'}),
 (l_hospmza:Lugar {idLugar: randomUUID(), nombre: 'Hospital de Mendoza', nombreNormalizado: 'hospital de mendoza'}),
 (l_casapropia:Lugar {idLugar: randomUUID(), nombre: 'Casa propia en Godoy Cruz', nombreNormalizado: 'casa propia en godoy cruz'}),
 (l_mardel:Lugar {idLugar: randomUUID(), nombre: 'Mar del Plata', nombreNormalizado: 'mar del plata'}),
 (r1:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Mis primeros días en Cipolletti', descripcion: 'Nací en Cipolletti, en 1950, en la casa de mis papás. Ellos ya vivían ahí desde hacía varios años, y yo viví en Cipolletti hasta los 14.', anio: 1950, precision: 'EXACTA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 90}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r1),
 (r1)-[:SURGIO_DE]->(q1),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 90})}]->(q1),
 (hugo)-[:PARTICIPA_EN]->(r1),
 (rosa)-[:PARTICIPA_EN]->(r1),
 (r1)-[:OCURRIO_EN]->(l_cipolletti),
 (r2:Recuerdo {idRecuerdo: randomUUID(), titulo: 'La casa de mi infancia', descripcion: 'Era una casa chica, de esas que tenían un patio grande al fondo, en la calle San Martín. Teníamos una parra que en verano daba una sombra hermosa y ahí pasábamos bastante tiempo. No era una casa muy grande, pero para nosotros estaba bien.', anio: 1957, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 85}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r2),
 (r2)-[:SURGIO_DE]->(q2),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 85})}]->(q2),
 (hugo)-[:PARTICIPA_EN]->(r2),
 (rosa)-[:PARTICIPA_EN]->(r2),
 (marta)-[:PARTICIPA_EN]->(r2),
 (hermanos)-[:PARTICIPA_EN]->(r2),
 (r2)-[:OCURRIO_EN]->(l_casasm),
 (r2)-[:OCURRIO_EN]->(l_cipolletti),
 (r3:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Una familia numerosa', descripcion: 'Vivía con mis papás y mis tres hermanos. Mi papá trabajaba bastante y mi mamá se ocupaba de la casa y de nosotros. Como éramos varios, siempre había ruido, discusiones por cualquier cosa y también muchas risas.', anio: 1958, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 80}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r3),
 (r3)-[:SURGIO_DE]->(q3),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 80})}]->(q3),
 (hugo)-[:PARTICIPA_EN]->(r3),
 (rosa)-[:PARTICIPA_EN]->(r3),
 (marta)-[:PARTICIPA_EN]->(r3),
 (hermanos)-[:PARTICIPA_EN]->(r3),
 (r3)-[:OCURRIO_EN]->(l_casasm),
 (r3)-[:OCURRIO_EN]->(l_cipolletti),
 (r4:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Un barrio donde todos se conocían', descripcion: 'Era bastante tranquilo. Los vecinos se conocían todos y era común que las madres se quedaran conversando en las veredas mientras nosotros jugábamos. Si hacíamos alguna travesura, seguro al rato ya se había enterado mi mamá.', anio: 1960, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 75}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r4),
 (r4)-[:SURGIO_DE]->(q4),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 75})}]->(q4),
 (marta)-[:PARTICIPA_EN]->(r4),
 (hermanos)-[:PARTICIPA_EN]->(r4),
 (amigosb)-[:PARTICIPA_EN]->(r4),
 (vecinos)-[:PARTICIPA_EN]->(r4),
 (r4)-[:OCURRIO_EN]->(l_cipolletti),
 (r5:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Tardes de juegos en la vereda', descripcion: 'Jugábamos mucho a las escondidas, a la rayuela y a saltar la soga. También armábamos casitas con cualquier cosa que encontrábamos. Casi siempre jugábamos en la vereda o en el patio de casa, no necesitábamos demasiadas cosas para entretenernos.', anio: 1960, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 70}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r5),
 (r5)-[:SURGIO_DE]->(q5),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 70})}]->(q5),
 (marta)-[:PARTICIPA_EN]->(r5),
 (hermanos)-[:PARTICIPA_EN]->(r5),
 (amigasb)-[:PARTICIPA_EN]->(r5),
 (r5)-[:OCURRIO_EN]->(l_casasm),
 (r6:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Los sabores de mamá', descripcion: 'Las empanadas de mi mamá. Las preparaba generalmente los domingos y nosotros siempre andábamos dando vueltas por la cocina esperando que salieran. También hacía tortitas raspadas para tomar mate, esas me acuerdo que me encantaban.', anio: 1960, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 65}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r6),
 (r6)-[:SURGIO_DE]->(q6),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 65})}]->(q6),
 (rosa)-[:PARTICIPA_EN]->(r6),
 (hugo)-[:PARTICIPA_EN]->(r6),
 (marta)-[:PARTICIPA_EN]->(r6),
 (hermanos)-[:PARTICIPA_EN]->(r6),
 (r6)-[:OCURRIO_EN]->(l_casasm),
 (r7:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Mis años en la escuela', descripcion: 'Fui a una escuela pública que quedaba cerca de casa. Iba caminando con mis hermanos y algunas amigas. Me acuerdo de los recreos, de los actos escolares y de llevar una merienda preparada por mi mamá. En general tengo lindos recuerdos de esa época.', anio: 1959, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 60}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r7),
 (r7)-[:SURGIO_DE]->(q7),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 60})}]->(q7),
 (marta)-[:PARTICIPA_EN]->(r7),
 (hermanos)-[:PARTICIPA_EN]->(r7),
 (compas)-[:PARTICIPA_EN]->(r7),
 (maestros)-[:PARTICIPA_EN]->(r7),
 (r7)-[:OCURRIO_EN]->(l_escuela),
 (r7)-[:OCURRIO_EN]->(l_cipolletti),
 (r8:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Una maestra que recuerdo', descripcion: 'Sí, mi maestra de primer grado, se llamaba Elena. Era bastante paciente y siempre trataba de ayudarnos cuando no entendíamos algo. Me enseñó a leer, y me acuerdo que me felicitó una vez por un trabajo que había hecho y me puso muy contenta.', anio: 1957, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 55}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r8),
 (r8)-[:SURGIO_DE]->(q8),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 55})}]->(q8),
 (elena)-[:PARTICIPA_EN]->(r8),
 (r8)-[:OCURRIO_EN]->(l_escuela),
 (r8)-[:OCURRIO_EN]->(l_cipolletti),
 (r9:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Lo que aprendí de mi mamá', descripcion: 'Mi mamá fue muy importante. Siempre estaba pendiente de nosotros, aunque tenía muchísimo trabajo. Me enseñó a cocinar, a ordenar mis cosas y sobre todo a ser responsable. Muchas cosas que hago hoy las aprendí de ella.', anio: 1957, precision: 'APROXIMADA', etapaDeVida: 'INFANCIA', fechaRegistro: ahora - duration({minutes: 50}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r9),
 (r9)-[:SURGIO_DE]->(q9),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 50})}]->(q9),
 (rosa)-[:PARTICIPA_EN]->(r9),
 (r9)-[:OCURRIO_EN]->(l_casasm),
 (r9)-[:OCURRIO_EN]->(l_cipolletti),
 (r10:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Amigas y tardes de adolescencia', descripcion: 'Principalmente con mis amigas del colegio y algunas chicas del barrio. Nos juntábamos por las tardes, a veces en alguna casa y otras veces salíamos a caminar. También nos gustaba ir a los cumpleaños que organizaban los vecinos.', anio: 1966, precision: 'APROXIMADA', etapaDeVida: 'ADOLESCENCIA', fechaRegistro: ahora - duration({minutes: 45}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r10),
 (r10)-[:SURGIO_DE]->(q10),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 45})}]->(q10),
 (silvia)-[:PARTICIPA_EN]->(r10),
 (norma)-[:PARTICIPA_EN]->(r10),
 (r10)-[:OCURRIO_EN]->(l_lasheras),
 (r10)-[:OCURRIO_EN]->(l_centro),
 (r11:Recuerdo {idRecuerdo: randomUUID(), titulo: 'La música de aquellos años', descripcion: 'Escuchaba mucho a Sandro, Leo Dan y Los Iracundos. Me gustaban los bailes que se organizaban en los clubes, íbamos con mis amigas y a veces conocíamos gente de otros barrios. También escuchábamos bastante música en la radio de casa.', anio: 1969, precision: 'APROXIMADA', etapaDeVida: 'JUVENTUD', fechaRegistro: ahora - duration({minutes: 40}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r11),
 (r11)-[:SURGIO_DE]->(q11),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 40})}]->(q11),
 (silvia)-[:PARTICIPA_EN]->(r11),
 (norma)-[:PARTICIPA_EN]->(r11),
 (novio)-[:PARTICIPA_EN]->(r11),
 (r11)-[:OCURRIO_EN]->(l_clubes),
 (r12:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Mi primer trabajo', descripcion: 'Mi primer trabajo fue en un negocio de ropa del centro de Mendoza. Tenía unos 18 años y atendía a los clientes, acomodaba prendas y ayudaba con algunas tareas del local. Me gustaba porque empezaba a ganar mi propio dinero.', anio: 1968, precision: 'APROXIMADA', etapaDeVida: 'JUVENTUD', fechaRegistro: ahora - duration({minutes: 35}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r12),
 (r12)-[:SURGIO_DE]->(q12),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 35})}]->(q12),
 (teresa)-[:PARTICIPA_EN]->(r12),
 (compast)-[:PARTICIPA_EN]->(r12),
 (r12)-[:OCURRIO_EN]->(l_local),
 (r13:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Nuestro primer hogar', descripcion: 'La primera casa donde viví fue después de casarme. Era una casita pequeña, con dos habitaciones y un patio. Al principio teníamos pocos muebles, fuimos comprando las cosas de a poco. Me acuerdo de estar bastante ocupada acomodando todo para dejarla como queríamos.', anio: 1973, precision: 'APROXIMADA', etapaDeVida: 'JUVENTUD', fechaRegistro: ahora - duration({minutes: 30}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r13),
 (r13)-[:SURGIO_DE]->(q13),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 30})}]->(q13),
 (roberto)-[:PARTICIPA_EN]->(r13),
 (r13)-[:OCURRIO_EN]->(l_godoy),
 (r14:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Una casualidad que terminó en amor', descripcion: 'Lo conocí en el cumpleaños de una amiga. Él había ido porque era amigo del hermano de ella. Nos pusimos a conversar un rato y después empezamos a encontrarnos más seguido. En ese momento yo tenía unos 20 años.', anio: 1970, precision: 'APROXIMADA', etapaDeVida: 'JUVENTUD', fechaRegistro: ahora - duration({minutes: 25}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r14),
 (r14)-[:SURGIO_DE]->(q14),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 25})}]->(q14),
 (roberto)-[:PARTICIPA_EN]->(r14),
 (silvia)-[:PARTICIPA_EN]->(r14),
 (r14)-[:OCURRIO_EN]->(l_casasilvia),
 (r15:Recuerdo {idRecuerdo: randomUUID(), titulo: 'La llegada de mi primer hijo', descripcion: 'Mi primer hijo nació en 1974. Fue un día bastante movido porque estaba nerviosa y no sabía muy bien qué esperar. Roberto estuvo conmigo y después también vinieron mis padres a conocerlo. Fue una experiencia que me cambió bastante la vida.', anio: 1974, precision: 'EXACTA', etapaDeVida: 'JUVENTUD', fechaRegistro: ahora - duration({minutes: 20}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r15),
 (r15)-[:SURGIO_DE]->(q15),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 20})}]->(q15),
 (roberto)-[:PARTICIPA_EN]->(r15),
 (hugo)-[:PARTICIPA_EN]->(r15),
 (rosa)-[:PARTICIPA_EN]->(r15),
 (martin)-[:PARTICIPA_EN]->(r15),
 (r15)-[:OCURRIO_EN]->(l_hospmza),
 (r16:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Las llaves de nuestra casa', descripcion: 'Uno de los días que recuerdo con más alegría fue cuando nos entregaron las llaves de nuestra casa propia. Habíamos trabajado y ahorrado bastante para poder comprarla. Fuimos con Roberto y los chicos a verla y ya empezamos a imaginar cómo íbamos a acomodar todo.', anio: 1982, precision: 'APROXIMADA', etapaDeVida: 'ADULTEZ', fechaRegistro: ahora - duration({minutes: 15}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r16),
 (r16)-[:SURGIO_DE]->(q16),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 15})}]->(q16),
 (roberto)-[:PARTICIPA_EN]->(r16),
 (hijos)-[:PARTICIPA_EN]->(r16),
 (r16)-[:OCURRIO_EN]->(l_casapropia),
 (r17:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Vacaciones frente al mar', descripcion: 'Sí, un viaje que recuerdo mucho fue el primero que hicimos en familia a Mar del Plata. Los chicos eran pequeños y estaban muy entusiasmados con conocer el mar. Para nosotros también fue especial porque no viajábamos demasiado, así que organizamos todo con bastante anticipación.', anio: 1984, precision: 'APROXIMADA', etapaDeVida: 'ADULTEZ', fechaRegistro: ahora - duration({minutes: 10}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r17),
 (r17)-[:SURGIO_DE]->(q17),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 10})}]->(q17),
 (roberto)-[:PARTICIPA_EN]->(r17),
 (hijos)-[:PARTICIPA_EN]->(r17),
 (r17)-[:OCURRIO_EN]->(l_mardel),
 (r18:Recuerdo {idRecuerdo: randomUUID(), titulo: 'Empezar de nuevo en otro hogar', descripcion: 'Sí, cuando nos mudamos de Cipolletti a Mendoza. Yo tenía unos 14 años y dejé mi escuela y a mis amigas. Mi papá había conseguido trabajo en la ciudad y viajamos todos juntos, con los muebles en un camión. Al principio me costó acostumbrarme, pero después conocí a Silvia y a las chicas del barrio.', anio: 1964, precision: 'APROXIMADA', etapaDeVida: 'ADOLESCENCIA', fechaRegistro: ahora - duration({minutes: 5}) + duration({seconds: 30}), intentosAnio: 0, intentosLugares: 0, intentosPersonas: 0}),
 (p)-[:TIENE_RECUERDO]->(r18),
 (r18)-[:SURGIO_DE]->(q18),
 (p)-[:RESPONDIO {fecha: ahora - duration({minutes: 5})}]->(q18),
 (hugo)-[:PARTICIPA_EN]->(r18),
 (rosa)-[:PARTICIPA_EN]->(r18),
 (marta)-[:PARTICIPA_EN]->(r18),
 (silvia)-[:PARTICIPA_EN]->(r18),
 (r18)-[:OCURRIO_EN]->(l_cipolletti),
 (r18)-[:OCURRIO_EN]->(l_mendoza)
RETURN p.idPersonaMayor AS persona, 18 AS recuerdosCargados, 'preguntas 1 a 18 respondidas; 19, 20 y 21 pendientes' AS resultado;


// =====================================================================================
// 2. VERIFICAR (solo lectura). Necesita :param persona
// =====================================================================================

// ---- V1. Contadores de la persona (esperado: 18 recuerdos, 18 respondidas, 0 salteadas, 18 personas, 13 lugares) ----
MATCH (p:PersonaMayor {idPersonaMayor: $persona})
RETURN COUNT { (p)-[:TIENE_RECUERDO]->() } AS recuerdos,
       COUNT { (p)-[:RESPONDIO]->() } AS respondidas,
       COUNT { (p)-[:SALTEO]->() } AS salteadas,
       COUNT { (p)-[:CONOCE]->() } AS personasConocidas,
       COUNT { MATCH (p)-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(l:Lugar) RETURN DISTINCT l } AS lugares;


// ---- V2. Próxima pregunta pendiente (esperado: orden 19) ----
MATCH (p:PersonaMayor {idPersonaMayor: $persona})
MATCH (q:PreguntaGuion)
WHERE NOT EXISTS { (p)-[:RESPONDIO|SALTEO]->(q) }
RETURN q.orden AS orden, q.texto AS texto
ORDER BY q.orden
LIMIT 3;


// ---- V3. Recuerdos con su pregunta, año, precisión y etapa ----
MATCH (p:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
RETURN q.orden AS pregunta, r.titulo AS titulo, r.anio AS anio, r.precision AS precision, r.etapaDeVida AS etapa
ORDER BY q.orden;


// ---- V4. Personas y lugares de cada recuerdo ----
MATCH (p:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->(r:Recuerdo)-[:SURGIO_DE]->(q:PreguntaGuion)
RETURN q.orden AS pregunta,
       COLLECT { MATCH (pe:Persona)-[:PARTICIPA_EN]->(r) RETURN coalesce(pe.nombre, '(' + [(p)-[c:CONOCE]->(pe) | c.vinculo][0] + ')') } AS personas,
       COLLECT { MATCH (r)-[:OCURRIO_EN]->(l:Lugar) RETURN l.nombre } AS lugares
ORDER BY q.orden;


// ---- V5. Duplicados (esperado: 0 filas) ----
MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:CONOCE]->(pe:Persona)
WHERE pe.nombreNormalizado IS NOT NULL
WITH pe.nombreNormalizado AS nombre, count(*) AS veces WHERE veces > 1
RETURN 'persona' AS tipo, nombre, veces
UNION
MATCH (:PersonaMayor {idPersonaMayor: $persona})-[:TIENE_RECUERDO]->()-[:OCURRIO_EN]->(l:Lugar)
WITH DISTINCT l
WITH l.nombreNormalizado AS nombre, count(*) AS veces WHERE veces > 1
RETURN 'lugar' AS tipo, nombre, veces;

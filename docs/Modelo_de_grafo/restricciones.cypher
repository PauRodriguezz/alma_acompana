// Restricciones de unicidad del modelo de grafo (idempotentes: IF NOT EXISTS)
CREATE CONSTRAINT personaMayor_id IF NOT EXISTS FOR (n:PersonaMayor) REQUIRE n.idPersonaMayor IS UNIQUE;
CREATE CONSTRAINT preguntaGuion_id IF NOT EXISTS FOR (n:PreguntaGuion) REQUIRE n.idPreguntaGuion IS UNIQUE;
CREATE CONSTRAINT preguntaGuion_orden IF NOT EXISTS FOR (n:PreguntaGuion) REQUIRE n.orden IS UNIQUE;
CREATE CONSTRAINT recuerdo_id IF NOT EXISTS FOR (n:Recuerdo) REQUIRE n.idRecuerdo IS UNIQUE;
CREATE CONSTRAINT persona_id IF NOT EXISTS FOR (n:Persona) REQUIRE n.idPersona IS UNIQUE;
CREATE CONSTRAINT lugar_id IF NOT EXISTS FOR (n:Lugar) REQUIRE n.idLugar IS UNIQUE;

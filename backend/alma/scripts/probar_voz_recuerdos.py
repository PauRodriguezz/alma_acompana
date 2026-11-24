#!/usr/bin/env python3
"""
Prueba con Gemini real de la VOZ de las descripciones de los recuerdos (docs/contrato-grafo.md, v21):
las descripciones tienen que salir en primera persona ("vivíamos con mi mamá"), sin "su/sus" y sin
adjetivos ni participios con género referidos a quien responde ("contenta", "criada").

Manda 5 respuestas cortas, largas y mezcladas al guion de la cuenta test_sin_recuerdos, espera a que
termine la extracción asíncrona de cada una y muestra la descripción generada con su veredicto.

REQUISITOS
  - Python 3 (solo biblioteca estándar).
  - El back corriendo con tu .env y GEMINI_API_KEY completa: estas pruebas llaman a Gemini de verdad
    (de 1 a 2 pedidos por respuesta). El nivel gratuito no debe usarse con datos reales de personas.
  - Docker levantado y la cuenta "ana" existente: la cuenta test_sin_recuerdos se crea sola, una vez,
    copiando la contraseña de prueba de ana ("docker exec alma-postgres psql ...").

CÓMO CORRERLO (desde la raíz del repo)
  Terminal 1:  cd backend/alma && ./mvnw spring-boot:run
  Terminal 2:  python backend/alma/scripts/probar_voz_recuerdos.py                # las 5 respuestas
               python backend/alma/scripts/probar_voz_recuerdos.py --solo 8,2     # solo algunas (ids de pregunta)
               TOPE=180 python backend/alma/scripts/probar_voz_recuerdos.py       # segundos máximos de espera por respuesta (120)
  Otra dirección del back:  B=http://localhost:8080 python ...

QUÉ ESCRIBE
  Nada en ana ni en carlos. Todo va a la cuenta test_sin_recuerdos: las respuestas quedan en su guion y sus
  recuerdos quedan en Neo4j. Cada pregunta se responde una sola vez: si la cuenta ya la respondió, el script
  la salta (limpiá la historia de esa cuenta con ver-y-limpiar-historia.cypher para repetir la prueba).
  El script no borra nada.

QUÉ MIRAR
  FALLA: la descripción tiene "su/sus" o tercera persona, adjetivos con género, "no me acuerdo", o es el
  texto de respaldo (el modelo no respondió). REVISAR: dato esperado que no apareció, cantidad de oraciones
  fuera de lo esperado o emoción en la descripción. Leé siempre la descripción: el chequeo es una heurística.
"""
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

B = os.environ.get("B", "http://localhost:8080")
TOPE = float(os.environ.get("TOPE", "120"))
PAUSA = float(os.environ.get("PAUSA", "3"))
USUARIO = "test_sin_recuerdos"
CLAVE = "Alma2026!"
ESPERA_ENTRE_CONSULTAS = 3.0

# pregunta del guion -> (respuesta, qué se espera)
CASOS = [
    {"pregunta": 8, "tipo": "corta",
     "texto": "Elena.",
     "oraciones": (1, 1), "anio": None, "personas": {"elena": "maestra"}, "sin_emocion": True},
    {"pregunta": 2, "tipo": "larga",
     "texto": "Vivíamos en una chacra en Cipolletti, por los años 60. Era una casa de adobe con un galpón grande y un "
              "parral. Mi papá cultivaba peras y manzanas y mi mamá hacía dulces. A la tarde nos juntábamos todos a "
              "tomar mate bajo el parral.",
     "oraciones": (2, 4), "anio": None, "personas": {}, "sin_emocion": True},
    {"pregunta": 10, "tipo": "con \"no me acuerdo\" mezclado",
     "texto": "No me acuerdo del año, pero iba con Marta a bailar los sábados.",
     "oraciones": (1, 2), "anio": None, "personas": {"marta": None}, "sin_emocion": True},
    {"pregunta": 9, "tipo": "con hermana",
     "texto": "Mi hermana Rosa. Me cuidaba cuando mis papás salían a trabajar y me enseñó a andar en bicicleta.",
     "oraciones": (1, 3), "anio": None, "personas": {"rosa": "hermana"}, "sin_emocion": True},
    {"pregunta": 16, "tipo": "con nieto y una emoción (que no debe aparecer)",
     "texto": "El día que nació mi nieto Mateo, en 2015, estaba muy contenta. Fuimos todos al sanatorio y mi hija me lo "
              "puso en los brazos.",
     "oraciones": (1, 3), "anio": 2015, "personas": {"mateo": "nieto"}, "sin_emocion": True},
]

TERCERA = re.compile(r"\b(su|sus|suyo|suya|suyos|suyas|la persona|quien responde)\b", re.I)
CON_GENERO = re.compile(
    r"\b(content[oa]s?|cansad[oa]s?|emocionad[oa]s?|orgullos[oa]s?|nervios[oa]s?|asustad[oa]s?|criad[oa]s?|"
    r"nacid[oa]s?|casad[oa]s?|enamorad[oa]s?|agradecid[oa]s?|agotad[oa]s?|sorprendid[oa]s?|tranquil[oa]s?)\b", re.I)
NO_RECUERDO = re.compile(r"\b(no (me )?acuerd\w*|no recuerd\w*|no se acuerda\w*)\b", re.I)
EMOCION = re.compile(r"\b(feliz|felices|alegr\w+|content\w+|emoci\w+|tristeza|triste|orgullo\w*|miedo|nervio\w*)\b", re.I)
PRIMERA = re.compile(r"\b(mi|mis|me|nos|nuestr[oa]s?|yo)\b|\w+(amos|ábamos|íamos)\b", re.I)


def http(metodo, ruta, cuerpo=None, token=None):
    datos = json.dumps(cuerpo, ensure_ascii=False).encode("utf-8") if cuerpo is not None else None
    req = urllib.request.Request(B + ruta, data=datos, method=metodo)
    req.add_header("Content-Type", "application/json; charset=utf-8")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            texto = r.read().decode("utf-8")
            return r.status, (json.loads(texto) if texto else None)
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8")
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto
    except urllib.error.URLError as e:
        sys.exit(f"No pude conectar con el back en {B}: {e.reason}. ¿Está corriendo?")


def crear_cuenta():
    sql = f"""
DO $$ DECLARE p bigint; c bigint; BEGIN
  IF NOT EXISTS (SELECT 1 FROM cuenta WHERE nombre_usuario='{USUARIO}') THEN
    INSERT INTO preferencia_accesibilidad (tamanio_letra, alto_contraste) VALUES ('NORMAL', false) RETURNING id INTO p;
    INSERT INTO cuenta (contrasenia, fecha_alta, nombre_usuario, rol)
      SELECT contrasenia, CURRENT_DATE, '{USUARIO}', 'PERSONA_MAYOR' FROM cuenta WHERE nombre_usuario='ana'
      RETURNING id INTO c;
    INSERT INTO persona_mayor (apellido, fecha_nacimiento, nombre, cuenta_id, preferencia_accesibilidad_id)
      VALUES ('PRUEBA', DATE '1955-01-01', 'Test', c, p);
  END IF;
END $$;
"""
    r = subprocess.run(["docker", "exec", "-i", "alma-postgres", "psql", "-U", "alma", "-d", "alma",
                        "-v", "ON_ERROR_STOP=1"], input=sql.encode("utf-8"), capture_output=True)
    if r.returncode != 0:
        sys.exit("No pude crear la cuenta de prueba: " + r.stderr.decode("utf-8", "replace"))


def login():
    codigo, cuerpo = http("POST", "/api/auth/login", {"nombreUsuario": USUARIO, "contrasenia": CLAVE})
    if codigo != 200:
        sys.exit(f"Login de {USUARIO} falló ({codigo}). ¿Existe la cuenta ana (de la que se copia la clave)?")
    return cuerpo["token"]


def oraciones(texto):
    return len([p for p in re.split(r"(?<=[.!?])\s+", texto.strip()) if p])


def ids_de_recuerdos(token):
    codigo, cuerpo = http("GET", "/api/historia/recuerdos", None, token)
    if codigo != 200:
        sys.exit(f"No pude leer los recuerdos ({codigo}): {cuerpo}")
    return {r["id"] for r in cuerpo["recuerdos"]}


def pregunta_ya_respondida(token, numero):
    # /guion/estado hace el MERGE del nodo de la persona: acá es la cuenta de prueba, no importa.
    codigo, cuerpo = http("GET", "/api/historia/guion/estado", None, token)
    if codigo != 200:
        sys.exit(f"No pude leer el estado del guion ({codigo}): {cuerpo}")
    for p in cuerpo["preguntas"]:
        if p["id"] == numero:
            return p["estado"] != "PENDIENTE", p["texto"]
    sys.exit(f"La pregunta {numero} no existe en el guion.")


def esperar_nuevo(token, antes):
    limite = time.time() + TOPE
    while time.time() < limite:
        time.sleep(ESPERA_ENTRE_CONSULTAS)
        nuevos = ids_de_recuerdos(token) - antes
        if nuevos:
            return nuevos.pop()
    return None


def evaluar(caso, detalle, pregunta_texto):
    d = detalle["descripcion"]
    fallas, revisar = [], []
    if detalle["descripcion"].strip() == caso["texto"].strip() and detalle["titulo"].strip() == pregunta_texto.strip():
        fallas.append("es el texto de respaldo: el modelo no respondió")
    if TERCERA.search(d):
        fallas.append(f'tercera persona ({TERCERA.search(d).group(0)!r})')
    if CON_GENERO.search(d):
        fallas.append(f'adjetivo o participio con género ({CON_GENERO.search(d).group(0)!r})')
    if NO_RECUERDO.search(d):
        fallas.append(f'anota lo que no recuerda ({NO_RECUERDO.search(d).group(0)!r})')
    if caso["sin_emocion"] and EMOCION.search(d):
        revisar.append(f'menciona una emoción ({EMOCION.search(d).group(0)!r})')
    n = oraciones(d)
    minimo, maximo = caso["oraciones"]
    if not minimo <= n <= maximo:
        revisar.append(f"{n} oraciones (se esperaban de {minimo} a {maximo})")
    if detalle["anio"] != caso["anio"]:
        revisar.append(f"anio = {detalle['anio']} (se esperaba {caso['anio']})")
    nombres = {(p["nombre"] or "").lower(): (p["vinculo"] or "").lower() for p in detalle["personas"]}
    for nombre, vinculo in caso["personas"].items():
        if nombre not in nombres:
            revisar.append(f"falta la persona {nombre}")
        elif vinculo and vinculo not in nombres[nombre]:
            revisar.append(f"{nombre}: vínculo {nombres[nombre]!r} (se esperaba {vinculo!r})")
    marcas = "sí" if PRIMERA.search(d) else "no se ven marcas claras"
    veredicto = "FALLA: " + "; ".join(fallas) if fallas else ("REVISAR: " + "; ".join(revisar) if revisar else "OK")
    return veredicto, n, marcas


def main():
    args = sys.argv[1:]
    casos = CASOS
    if "--solo" in args:
        pedidas = {int(x) for x in args[args.index("--solo") + 1].split(",")}
        casos = [c for c in CASOS if c["pregunta"] in pedidas]

    crear_cuenta()
    token = login()
    resultados = []
    for i, caso in enumerate(casos):
        numero = caso["pregunta"]
        respondida, pregunta_texto = pregunta_ya_respondida(token, numero)
        print(f"\n=== Pregunta {numero} · {caso['tipo']}\n  Pregunta : {pregunta_texto}\n  Respuesta: {caso['texto']}")
        if respondida:
            print("  (esta cuenta ya respondió o salteó esta pregunta: se salta)")
            resultados.append((numero, "SALTADA", "-"))
            continue
        antes = ids_de_recuerdos(token)
        codigo, cuerpo = http("POST", f"/api/historia/guion/{numero}/respuesta", {"texto": caso["texto"]}, token)
        if codigo != 202:
            print(f"  La respuesta no fue aceptada: HTTP {codigo} {cuerpo}")
            resultados.append((numero, f"FALLA: HTTP {codigo}", "-"))
            continue
        print(f"  Aceptada (202). Esperando la extracción (hasta {TOPE:.0f} s)...")
        inicio = time.time()
        nuevo = esperar_nuevo(token, antes)
        if nuevo is None:
            print(f"  No apareció ningún recuerdo nuevo en {TOPE:.0f} s.")
            resultados.append((numero, "FALLA: sin recuerdo (tiempo agotado)", "-"))
            continue
        codigo, detalle = http("GET", f"/api/historia/recuerdos/{nuevo}", None, token)
        if codigo != 200:
            resultados.append((numero, f"FALLA: detalle HTTP {codigo}", "-"))
            continue
        veredicto, n, marcas = evaluar(caso, detalle, pregunta_texto)
        personas = ", ".join(f"{p['nombre'] or '(sin nombre)'} ({p['vinculo'] or 's/v'})" for p in detalle["personas"]) or "-"
        lugares = ", ".join(l["nombre"] for l in detalle["lugares"]) or "-"
        print(f"  Tardó {time.time() - inicio:.0f} s\n  Título     : {detalle['titulo']}\n"
              f"  Descripción: {detalle['descripcion']}\n  Año: {detalle['anio']} · Personas: {personas} · Lugares: {lugares}\n"
              f"  {n} oraciones · marcas de primera persona: {marcas}\n  VEREDICTO  : {veredicto}")
        resultados.append((numero, veredicto, f"{n} oraciones"))
        if i < len(casos) - 1:
            time.sleep(PAUSA)

    print("\n\n=========== RESUMEN ===========")
    for numero, veredicto, extra in resultados:
        print(f"Pregunta {numero:>2}: {veredicto}  [{extra}]")
    print("\nEl script no borra nada. Para limpiar la cuenta de prueba: ver-y-limpiar-historia.cypher (borrado por persona).")


if __name__ == "__main__":
    main()

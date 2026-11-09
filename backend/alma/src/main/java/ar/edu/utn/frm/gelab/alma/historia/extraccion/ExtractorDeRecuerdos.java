package ar.edu.utn.frm.gelab.alma.historia.extraccion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import ar.edu.utn.frm.gelab.alma.historia.config.ConfiguracionHistoria;
import ar.edu.utn.frm.gelab.alma.historia.domain.NombreNormalizado;
import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional;
import ar.edu.utn.frm.gelab.alma.ia.ModeloConversacional.OpcionesDeLlamada;
import ar.edu.utn.frm.gelab.alma.ia.ModeloNoDisponibleException;
import ar.edu.utn.frm.gelab.alma.ia.TurnoConversacion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convierte la respuesta de la persona en un recuerdo estructurado (contrato del grafo, 5.4): pedido al modelo,
 * limpieza, conversión, validación de estructura y de contenido, un reintento y el respaldo.
 *
 * <p>No toca el grafo ni usa transacciones: llamar al modelo puede tardar 25 s y no debe tener abierta una
 * transacción de Neo4j. <b>Nunca lanza errores hacia afuera</b>: lo que no se puede extraer termina en el
 * respaldo. Privacidad (5.9): el log registra pedidos, duración y tipo de falla, nunca el texto de la persona,
 * del prompt ni del JSON del modelo.
 */
@Component
public class ExtractorDeRecuerdos {

    private static final Logger log = LoggerFactory.getLogger(ExtractorDeRecuerdos.class);

    static final String RECURSO_PROMPT = "historia/prompt-extractor.txt";
    private static final int ANIO_MINIMO = 1900;
    private static final int EDAD_MAXIMA = 110;
    private static final int MAXIMO_DE_PEDIDOS = 2;

    // Los textos de estos errores se le mandan al modelo en el reintento: son fijos y no citan nada.
    static final String ERROR_NO_ES_JSON = "No era un objeto JSON válido.";
    static final String ERROR_CAMPO_INVALIDO = "Un campo tenía un tipo incorrecto.";
    static final String ERROR_TEXTO_VACIO = "Los campos titulo y descripcion no pueden estar vacíos.";

    private final ModeloConversacional modelo;
    private final ConfiguracionHistoria configuracion;
    private final Clock clock;
    private final JsonMapper jsonMapper;
    private final String prompt;

    public ExtractorDeRecuerdos(ModeloConversacional modelo, ConfiguracionHistoria configuracion, Clock clock,
            JsonMapper jsonMapper) {
        this.modelo = modelo;
        this.configuracion = configuracion;
        this.clock = clock;
        this.jsonMapper = jsonMapper;
        this.prompt = leerPrompt();
    }

    /** Hasta dos pedidos al modelo; si no sirven o el modelo no responde, el respaldo. No lanza errores. */
    public ResultadoDeExtraccion extraer(TrabajoDeExtraccion trabajo) {
        long inicio = System.nanoTime();
        String errorAnterior = null;
        int pedidos = 0;
        try {
            while (pedidos < MAXIMO_DE_PEDIDOS) {
                pedidos++;
                String salida;
                try {
                    salida = modelo.responder(prompt, List.of(turno(trabajo, errorAnterior)),
                            new OpcionesDeLlamada(configuracion.getExtraccionTemperatura(),
                                    configuracion.getExtraccionMaxTokens()));
                } catch (ModeloNoDisponibleException e) {
                    // Sin modelo, con demora excesiva o sin turno: no se reintenta, va el respaldo.
                    registrar("el modelo no está disponible", pedidos, inicio, true);
                    return respaldo(trabajo, pedidos);
                }
                try {
                    RespuestaExtraida respuesta = validarContenido(convertir(salida), trabajo);
                    registrar("recuerdo extraído", pedidos, inicio, false);
                    return new ResultadoDeExtraccion(respuesta, pedidos, false);
                } catch (RespuestaInvalida e) {
                    errorAnterior = e.getMessage();
                    log.info("La respuesta del modelo no sirvió (pedido {}): {}", pedidos, errorAnterior);
                }
            }
            registrar("la respuesta del modelo no sirvió", pedidos, inicio, true);
        } catch (RuntimeException e) {
            // Contrato, 5.4: el trabajo termina sin lanzar errores. El mensaje podría citar el texto.
            registrar("falla inesperada (" + e.getClass().getSimpleName() + ")", pedidos, inicio, true);
        }
        return respaldo(trabajo, pedidos);
    }

    // ---------- Pedido ----------

    private static TurnoConversacion turno(TrabajoDeExtraccion trabajo, String errorAnterior) {
        StringBuilder texto = new StringBuilder("Pregunta: ").append(trabajo.textoPregunta())
                .append("\nRespuesta: ").append(trabajo.respuesta());
        if (errorAnterior != null) {
            texto.append("\n\nTu respuesta anterior no se pudo usar. ").append(errorAnterior)
                    .append(" Devolvé solo el objeto JSON corregido.");
        }
        return new TurnoConversacion(TurnoConversacion.Rol.PERSONA, texto.toString());
    }

    // ---------- Limpieza y conversión (pasos 2 y 3) ----------

    /** Solo el bloque JSON: sin texto antes o después ni delimitadores de código. */
    static String limpiar(String salida) {
        String texto = salida == null ? "" : salida.strip();
        int abre = texto.indexOf('{');
        int cierra = texto.lastIndexOf('}');
        if (abre < 0 || cierra < abre) {
            throw new RespuestaInvalida(ERROR_NO_ES_JSON);
        }
        return texto.substring(abre, cierra + 1);
    }

    private RespuestaExtraida convertir(String salida) {
        JsonNode raiz;
        try {
            raiz = jsonMapper.readTree(limpiar(salida));
        } catch (RuntimeException e) {
            if (e instanceof RespuestaInvalida) {
                throw e;
            }
            throw new RespuestaInvalida(ERROR_NO_ES_JSON);
        }
        if (raiz == null || !raiz.isObject()) {
            throw new RespuestaInvalida(ERROR_NO_ES_JSON);
        }
        String titulo = texto(raiz.get("titulo"));
        String descripcion = texto(raiz.get("descripcion"));
        Integer anio = entero(raiz.get("anio"));
        Integer edad = entero(raiz.get("edadMencionada"));
        boolean aproximado = booleano(raiz.get("anioAproximado"));
        List<PersonaExtraida> personas = personas(raiz.get("personas"));
        List<String> lugares = lugares(raiz.get("lugares"));
        validarEstructura(titulo, descripcion, anio, edad);
        return new RespuestaExtraida(titulo, descripcion, anio, aproximado, edad, personas, lugares);
    }

    /** Texto sin espacios de más; {@code null}, el texto "null" y lo vacío pasan a nulo. */
    private static String texto(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }
        if (!nodo.isString() && !nodo.isNumber()) {
            throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
        }
        String valor = nodo.asString().strip();
        return valor.isEmpty() || valor.equalsIgnoreCase("null") ? null : valor;
    }

    /** Un número entero, o un texto que lo contiene ("1957"); lo demás, nulo si falta o error. */
    private static Integer entero(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }
        if (nodo.isNumber()) {
            double valor = nodo.doubleValue();
            if (valor != Math.rint(valor) || Math.abs(valor) > Integer.MAX_VALUE) {
                throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
            }
            return (int) valor;
        }
        if (nodo.isString()) {
            String valor = nodo.asString().strip();
            if (valor.isEmpty() || valor.equalsIgnoreCase("null")) {
                return null;
            }
            try {
                return Integer.valueOf(valor);
            } catch (NumberFormatException e) {
                throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
            }
        }
        throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
    }

    private static boolean booleano(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return false;
        }
        if (nodo.isBoolean()) {
            return nodo.booleanValue();
        }
        if (nodo.isString()) {
            return nodo.asString().strip().equalsIgnoreCase("true");
        }
        throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
    }

    private static List<PersonaExtraida> personas(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return List.of();
        }
        if (!nodo.isArray()) {
            throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
        }
        List<PersonaExtraida> personas = new ArrayList<>();
        for (JsonNode elemento : nodo) {
            if (!elemento.isObject()) {
                throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
            }
            String nombre = texto(elemento.get("nombre"));
            String vinculo = texto(elemento.get("vinculo"));
            if (nombre != null || vinculo != null) {
                personas.add(new PersonaExtraida(nombre, vinculo));
            }
        }
        return personas;
    }

    private static List<String> lugares(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return List.of();
        }
        if (!nodo.isArray()) {
            throw new RespuestaInvalida(ERROR_CAMPO_INVALIDO);
        }
        List<String> lugares = new ArrayList<>();
        for (JsonNode elemento : nodo) {
            String lugar = texto(elemento);
            if (lugar != null) {
                lugares.add(lugar);
            }
        }
        return lugares;
    }

    // ---------- Validación (pasos 4 y 5) ----------

    private void validarEstructura(String titulo, String descripcion, Integer anio, Integer edad) {
        if (titulo == null || descripcion == null) {
            throw new RespuestaInvalida(ERROR_TEXTO_VACIO);
        }
        int anioActual = LocalDate.now(clock).getYear();
        if (anio != null && (anio < ANIO_MINIMO || anio > anioActual)) {
            throw new RespuestaInvalida("El campo anio debe estar entre " + ANIO_MINIMO + " y " + anioActual
                    + ", o ser null.");
        }
        if (edad != null && (edad < 0 || edad > EDAD_MAXIMA)) {
            throw new RespuestaInvalida("El campo edadMencionada debe estar entre 0 y " + EDAD_MAXIMA
                    + ", o ser null.");
        }
    }

    /**
     * "No inventar datos" (5.4, paso 5): el año y la edad solo valen si ese número está escrito en cifras en la
     * respuesta; se descarta (sin reintento) a la propia persona mayor. Una década escrita no pasa el control.
     */
    private static RespuestaExtraida validarContenido(RespuestaExtraida r, TrabajoDeExtraccion trabajo) {
        Integer anio = r.anio() != null && aparece(trabajo.respuesta(), r.anio()) ? r.anio() : null;
        Integer edad = r.edadMencionada() != null && aparece(trabajo.respuesta(), r.edadMencionada())
                ? r.edadMencionada() : null;
        String propio = NombreNormalizado.de(trabajo.nombre());
        String propioCompleto = NombreNormalizado.de(trabajo.nombre() + " " + trabajo.apellido());
        Map<String, PersonaExtraida> personas = new LinkedHashMap<>();
        List<PersonaExtraida> sinNombre = new ArrayList<>();
        for (PersonaExtraida p : r.personas()) {
            String normalizado = NombreNormalizado.de(p.nombre());
            if (normalizado == null) {
                sinNombre.add(p);
            } else if (!normalizado.equals(propio) && !normalizado.equals(propioCompleto)) {
                personas.putIfAbsent(normalizado, p);
            }
        }
        List<PersonaExtraida> resultado = new ArrayList<>(personas.values());
        resultado.addAll(sinNombre);
        return new RespuestaExtraida(r.titulo(), r.descripcion(), anio, anio != null && r.anioAproximado(), edad,
                resultado, r.lugares());
    }

    /** ¿El número está escrito en cifras, como número entero y no como parte de otro? */
    static boolean aparece(String texto, int numero) {
        return Pattern.compile("(?<![\\d])" + numero + "(?![\\d])").matcher(texto).find();
    }

    // ---------- Respaldo (paso 7) ----------

    /** Título igual a la pregunta, descripción igual a la respuesta, sin personas ni lugares ni año. */
    private static ResultadoDeExtraccion respaldo(TrabajoDeExtraccion trabajo, int pedidos) {
        RespuestaExtraida respuesta = new RespuestaExtraida(trabajo.textoPregunta(), trabajo.respuesta().strip(),
                null, false, null, List.of(), List.of());
        return new ResultadoDeExtraccion(respuesta, pedidos, true);
    }

    // ---------- Utilidades ----------

    private static void registrar(String que, int pedidos, long inicioNanos, boolean respaldo) {
        log.info("Extracción: {} (pedidos al modelo: {}, respaldo: {}, {} ms)", que, pedidos, respaldo,
                (System.nanoTime() - inicioNanos) / 1_000_000);
    }

    private static String leerPrompt() {
        try (InputStream entrada = new ClassPathResource(RECURSO_PROMPT).getInputStream()) {
            return StreamUtils.copyToString(entrada, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + RECURSO_PROMPT, e);
        }
    }

    /** Una falla de estructura o de tipo. El mensaje es fijo y se le muestra al modelo en el reintento. */
    static class RespuestaInvalida extends RuntimeException {
        RespuestaInvalida(String mensaje) {
            super(mensaje);
        }
    }
}

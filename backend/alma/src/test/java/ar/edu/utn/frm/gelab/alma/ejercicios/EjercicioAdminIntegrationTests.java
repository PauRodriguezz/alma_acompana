package ar.edu.utn.frm.gelab.alma.ejercicios;

import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.bearer;
import static ar.edu.utn.frm.gelab.alma.SesionDePrueba.token;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioRequest;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.OpcionRequest;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * CU-10 desde el panel de operador (contrato del panel, secciones 3.2 y 4). Usan el Postgres de Docker
 * ("docker compose up -d"). Cada test corre en una transacción que se deshace al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EjercicioAdminIntegrationTests {

    private static final String URL = "/api/admin/ejercicios";
    private static final String CAMPOS_REQUERIDOS = "Completá todos los campos requeridos del ejercicio.";
    private static final String OPCIONES_MULTIPLE = "Cargá de 3 a 4 opciones y marcá una sola como correcta.";
    private static final String OPCIONES_ORDENAR = "Cargá de 3 a 5 pasos y asignale a cada uno una posición distinta.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String admin;

    @BeforeEach
    void ingresar() throws Exception {
        admin = token(mockMvc, "admin");
    }

    @Test
    void crearListarVerEditarYDarDeBaja() throws Exception {
        EjercicioRequest refran = opcionMultiple(CategoriaEjercicio.REFRAN, "Completá el refrán de prueba",
                correcta("…que cien volando"), incorrecta("…que dos en el aire"), incorrecta("…que uno en la mano"));
        int id = crear(refran)
                .andExpect(jsonPath("$.fechaAlta").value(Fechas.hoy().toString()))
                .andExpect(jsonPath("$.opciones", hasSize(3)))
                .andExpect(jsonPath("$.opciones[0].esCorrecta").value(true))
                .andExpect(jsonPath("$.opciones[0].ordenCorrecto", nullValue()))
                .andReturn().getResponse().getContentAsString().transform(cuerpo -> JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(conAdmin(get(URL)))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(conAdmin(get(URL).param("categoria", "REFRAN")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.categoria != 'REFRAN')]").isEmpty());
        mockMvc.perform(conAdmin(get(URL).param("categoria", "SERIE")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mockMvc.perform(conAdmin(get(URL + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consigna").value("Completá el refrán de prueba"));

        // Editar cambiando el formato: se aplican las reglas de ORDENAR y se reemplazan todas las opciones.
        EjercicioRequest ahoraOrdenar = ordenar("Ordená de menor a mayor", paso("Tres", 3), paso("Uno", 1),
                paso("Dos", 2), paso("Cuatro", 4));
        mockMvc.perform(conAdmin(put(URL + "/" + id)).content(json(ahoraOrdenar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formato").value("ORDENAR"))
                .andExpect(jsonPath("$.opciones", hasSize(4)))
                .andExpect(jsonPath("$.opciones[*].texto", contains("Uno", "Dos", "Tres", "Cuatro")))
                .andExpect(jsonPath("$.opciones[*].ordenCorrecto", contains(1, 2, 3, 4)))
                .andExpect(jsonPath("$.opciones[*].esCorrecta", contains(false, false, false, false)));
        entityManager.flush();
        assertThat(contarOpciones(id)).isEqualTo(4);

        mockMvc.perform(conAdmin(delete(URL + "/" + id)))
                .andExpect(status().isNoContent());
        mockMvc.perform(conAdmin(get(URL)))
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
        mockMvc.perform(conAdmin(get(URL + "/" + id)))
                .andExpect(status().isNotFound());

        // Baja lógica: la fila y sus opciones quedan, con la fecha de baja de hoy.
        entityManager.flush();
        LocalDate fechaBaja = jdbcTemplate.queryForObject(
                "select fecha_baja from ejercicio where id = ?", LocalDate.class, id);
        assertThat(fechaBaja).isEqualTo(Fechas.hoy());
        assertThat(contarOpciones(id)).isEqualTo(4);
    }

    @Test
    void ordenarGuardaLosPasosEnElOrdenCorrecto() throws Exception {
        crear(ordenar("Ordená los pasos para preparar un té", paso("Servir", 3), paso("Calentar el agua", 1),
                paso("Poner el saquito", 2)))
                .andExpect(jsonPath("$.opciones[*].texto", contains("Calentar el agua", "Poner el saquito", "Servir")));
    }

    @Test
    void textoAMemorizarSoloSeGuardaEnMemoria() throws Exception {
        EjercicioRequest memoria = new EjercicioRequest(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                "¿Qué estaba en la lista?", NivelDificultad.BASICO, null, "Pan, leche y yerba",
                List.of(correcta("Yerba"), incorrecta("Café"), incorrecta("Arroz")));
        crear(memoria).andExpect(jsonPath("$.textoAMemorizar").value("Pan, leche y yerba"));

        EjercicioRequest serie = new EjercicioRequest(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                "¿Qué número sigue? 1, 2, 3…", NivelDificultad.BASICO, null, "no se guarda",
                List.of(correcta("4"), incorrecta("5"), incorrecta("6")));
        crear(serie).andExpect(jsonPath("$.textoAMemorizar", nullValue()));
    }

    @Test
    void camposVaciosDevuelve400ConErroresPorCampo() throws Exception {
        mockMvc.perform(conAdmin(post(URL)).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(CAMPOS_REQUERIDOS))
                .andExpect(jsonPath("$.errores[?(@.campo == 'consigna')].mensaje").value("Ingresá la consigna."));
    }

    @Test
    void camposFaltantesTienenPrioridadSobreLasOpciones() throws Exception {
        EjercicioRequest sinConsignaYConDosOpciones = new EjercicioRequest(CategoriaEjercicio.REFRAN,
                FormatoEjercicio.OPCION_MULTIPLE, " ", NivelDificultad.BASICO, null, null,
                List.of(correcta("a"), correcta("b")));
        rechazar(sinConsignaYConDosOpciones, CAMPOS_REQUERIDOS);
    }

    @Test
    void memoriaSinTextoAMemorizarDevuelve400() throws Exception {
        EjercicioRequest memoria = new EjercicioRequest(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                "¿Qué estaba en la lista?", NivelDificultad.BASICO, null, null,
                List.of(correcta("a"), incorrecta("b"), incorrecta("c")));
        rechazar(memoria, CAMPOS_REQUERIDOS)
                .andExpect(jsonPath("$.errores[0].campo").value("textoAMemorizar"));
    }

    @Test
    void opcionMultipleConDosOpcionesDevuelve400() throws Exception {
        rechazar(opcionMultiple(CategoriaEjercicio.REFRAN, "c", correcta("a"), incorrecta("b")), OPCIONES_MULTIPLE)
                .andExpect(jsonPath("$.errores[0].campo").value("opciones"));
    }

    @Test
    void opcionMultipleConDosCorrectasDevuelve400() throws Exception {
        rechazar(opcionMultiple(CategoriaEjercicio.REFRAN, "c", correcta("a"), correcta("b"), incorrecta("c")),
                OPCIONES_MULTIPLE);
    }

    @Test
    void ordenarConPosicionesRepetidasDevuelve400() throws Exception {
        rechazar(ordenar("c", paso("a", 1), paso("b", 1), paso("c", 2)), OPCIONES_ORDENAR)
                .andExpect(jsonPath("$.errores[0].campo").value("opciones"));
    }

    @Test
    void ordenarConPosicionesSalteadasDevuelve400() throws Exception {
        rechazar(ordenar("c", paso("a", 1), paso("b", 2), paso("c", 4)), OPCIONES_ORDENAR);
    }

    private ResultActions crear(EjercicioRequest datos) throws Exception {
        return mockMvc.perform(conAdmin(post(URL)).content(json(datos)))
                .andExpect(status().isCreated());
    }

    private ResultActions rechazar(EjercicioRequest datos, String detalle) throws Exception {
        return mockMvc.perform(conAdmin(post(URL)).content(json(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(detalle));
    }

    private int contarOpciones(int ejercicioId) {
        Integer cantidad = jdbcTemplate.queryForObject(
                "select count(*) from opcion_respuesta where ejercicio_id = ?", Integer.class, ejercicioId);
        return cantidad == null ? 0 : cantidad;
    }

    private static EjercicioRequest opcionMultiple(CategoriaEjercicio categoria, String consigna,
                                                   OpcionRequest... opciones) {
        return new EjercicioRequest(categoria, FormatoEjercicio.OPCION_MULTIPLE, consigna, NivelDificultad.BASICO,
                "Explicación de prueba", null, List.of(opciones));
    }

    private static EjercicioRequest ordenar(String consigna, OpcionRequest... pasos) {
        return new EjercicioRequest(CategoriaEjercicio.SERIE, FormatoEjercicio.ORDENAR, consigna,
                NivelDificultad.INTERMEDIO, null, null, List.of(pasos));
    }

    private static OpcionRequest correcta(String texto) {
        return new OpcionRequest(texto, true, null, null);
    }

    private static OpcionRequest incorrecta(String texto) {
        return new OpcionRequest(texto, false, null, null);
    }

    private static OpcionRequest paso(String texto, int posicion) {
        return new OpcionRequest(texto, null, null, posicion);
    }

    private MockHttpServletRequestBuilder conAdmin(MockHttpServletRequestBuilder pedido) {
        return pedido.header(HttpHeaders.AUTHORIZATION, bearer(admin)).contentType(MediaType.APPLICATION_JSON);
    }

    private String json(Object cuerpo) {
        return jsonMapper.writeValueAsString(cuerpo);
    }
}

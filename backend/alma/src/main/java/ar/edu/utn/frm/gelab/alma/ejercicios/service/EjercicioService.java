package ar.edu.utn.frm.gelab.alma.ejercicios.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;
import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.common.imagenes.RutaImagen;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioRequest;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioResponse;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.EjercicioResumenResponse;
import ar.edu.utn.frm.gelab.alma.ejercicios.dto.OpcionRequest;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;

/**
 * Catálogo de ejercicios del panel de operador (CU-10). Baja lógica con fechaBaja.
 */
@Service
public class EjercicioService {

    static final String CAMPOS_REQUERIDOS = "Completá todos los campos requeridos del ejercicio.";
    static final String OPCIONES_MULTIPLE = "Cargá de 3 a 4 opciones y marcá una sola como correcta.";
    static final String OPCIONES_ORDENAR = "Cargá de 3 a 5 pasos y asignale a cada uno una posición distinta.";

    private static final int MAX_CONSIGNA = 300;
    private static final int MAX_EXPLICACION = 500;
    private static final int MAX_TEXTO_A_MEMORIZAR = 1000;
    private static final int MAX_TEXTO_OPCION = 150;

    private final EjercicioRepository ejercicioRepository;

    public EjercicioService(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    @Transactional(readOnly = true)
    public List<EjercicioResumenResponse> listar(CategoriaEjercicio categoria) {
        return ejercicioRepository.listarActivos(categoria).stream()
                .map(EjercicioResumenResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public EjercicioResponse obtener(Long id) {
        return EjercicioResponse.de(activo(id));
    }

    @Transactional
    public EjercicioResponse crear(EjercicioRequest datos) {
        validar(datos);
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setFechaAlta(Fechas.hoy());
        copiar(datos, ejercicio);
        ejercicioRepository.saveAndFlush(ejercicio);
        return EjercicioResponse.de(ejercicio);
    }

    /** Reemplaza el ejercicio completo; las opciones anteriores se borran (orphanRemoval). */
    @Transactional
    public EjercicioResponse actualizar(Long id, EjercicioRequest datos) {
        Ejercicio ejercicio = activo(id);
        validar(datos);
        copiar(datos, ejercicio);
        // El flush asigna los ids de las opciones nuevas antes de armar la respuesta.
        ejercicioRepository.flush();
        return EjercicioResponse.de(ejercicio);
    }

    /** Baja lógica: solo se completa fechaBaja. No se tocan las opciones ni los intentos. */
    @Transactional
    public void darDeBaja(Long id) {
        activo(id).setFechaBaja(Fechas.hoy());
    }

    private Ejercicio activo(Long id) {
        return ejercicioRepository.findByIdAndFechaBajaIsNull(id)
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private static void copiar(EjercicioRequest datos, Ejercicio ejercicio) {
        ejercicio.setCategoria(datos.categoria());
        ejercicio.setFormato(datos.formato());
        ejercicio.setConsigna(datos.consigna().strip());
        ejercicio.setNivelDificultad(datos.nivelDificultad());
        ejercicio.setExplicacion(sinBlancos(datos.explicacion()));
        ejercicio.setTextoAMemorizar(datos.categoria() == CategoriaEjercicio.MEMORIA
                ? datos.textoAMemorizar().strip()
                : null);

        List<OpcionRequest> opciones = new ArrayList<>(datos.opciones());
        if (datos.formato() == FormatoEjercicio.ORDENAR) {
            // En ORDENAR se guardan en el orden de ordenCorrecto.
            opciones.sort(Comparator.comparing(OpcionRequest::ordenCorrecto));
        }
        ejercicio.getOpciones().clear();
        for (OpcionRequest datosOpcion : opciones) {
            OpcionRespuesta opcion = new OpcionRespuesta();
            opcion.setTexto(datosOpcion.texto().strip());
            opcion.setImagen(sinBlancos(datosOpcion.imagen()));
            if (datos.formato() == FormatoEjercicio.ORDENAR) {
                opcion.setEsCorrecta(false);
                opcion.setOrdenCorrecto(datosOpcion.ordenCorrecto());
            } else {
                opcion.setEsCorrecta(Boolean.TRUE.equals(datosOpcion.esCorrecta()));
                opcion.setOrdenCorrecto(null);
            }
            ejercicio.getOpciones().add(opcion);
        }
    }

    /**
     * Orden de las reglas: 1) campos obligatorios, 2) opciones según el formato, 3) largos e imágenes.
     */
    private static void validar(EjercicioRequest datos) {
        List<OpcionRequest> opciones = datos.opciones() == null ? List.of() : datos.opciones();

        List<ErrorCampo> faltantes = new ArrayList<>();
        if (datos.categoria() == null) {
            faltantes.add(new ErrorCampo("categoria", "Elegí una categoría."));
        }
        if (datos.formato() == null) {
            faltantes.add(new ErrorCampo("formato", "Elegí un formato."));
        }
        if (estaVacio(datos.consigna())) {
            faltantes.add(new ErrorCampo("consigna", "Ingresá la consigna."));
        }
        if (datos.nivelDificultad() == null) {
            faltantes.add(new ErrorCampo("nivelDificultad", "Elegí un nivel de dificultad."));
        }
        if (datos.categoria() == CategoriaEjercicio.MEMORIA && estaVacio(datos.textoAMemorizar())) {
            faltantes.add(new ErrorCampo("textoAMemorizar", "Ingresá el texto a memorizar."));
        }
        for (int i = 0; i < opciones.size(); i++) {
            OpcionRequest opcion = opciones.get(i);
            if (opcion == null || estaVacio(opcion.texto())) {
                faltantes.add(new ErrorCampo("opciones[" + i + "].texto", "Ingresá el texto de la opción."));
            }
        }
        if (!faltantes.isEmpty()) {
            throw new DatosInvalidosException(CAMPOS_REQUERIDOS, faltantes);
        }

        if (datos.formato() == FormatoEjercicio.OPCION_MULTIPLE && !opcionMultipleValida(opciones)) {
            throw new DatosInvalidosException(OPCIONES_MULTIPLE, List.of(new ErrorCampo("opciones", OPCIONES_MULTIPLE)));
        }
        if (datos.formato() == FormatoEjercicio.ORDENAR && !ordenarValido(opciones)) {
            throw new DatosInvalidosException(OPCIONES_ORDENAR, List.of(new ErrorCampo("opciones", OPCIONES_ORDENAR)));
        }

        List<ErrorCampo> errores = new ArrayList<>();
        if (datos.consigna().strip().length() > MAX_CONSIGNA) {
            errores.add(new ErrorCampo("consigna", "La consigna puede tener hasta " + MAX_CONSIGNA + " caracteres."));
        }
        if (!estaVacio(datos.explicacion()) && datos.explicacion().strip().length() > MAX_EXPLICACION) {
            errores.add(new ErrorCampo("explicacion",
                    "La explicación puede tener hasta " + MAX_EXPLICACION + " caracteres."));
        }
        if (datos.categoria() == CategoriaEjercicio.MEMORIA
                && datos.textoAMemorizar().strip().length() > MAX_TEXTO_A_MEMORIZAR) {
            errores.add(new ErrorCampo("textoAMemorizar",
                    "El texto a memorizar puede tener hasta " + MAX_TEXTO_A_MEMORIZAR + " caracteres."));
        }
        for (int i = 0; i < opciones.size(); i++) {
            OpcionRequest opcion = opciones.get(i);
            if (opcion.texto().strip().length() > MAX_TEXTO_OPCION) {
                errores.add(new ErrorCampo("opciones[" + i + "].texto",
                        "El texto de la opción puede tener hasta " + MAX_TEXTO_OPCION + " caracteres."));
            }
            String imagen = sinBlancos(opcion.imagen());
            if (imagen != null && !RutaImagen.esValida(imagen)) {
                errores.add(new ErrorCampo("opciones[" + i + "].imagen", "La imagen no es válida. Volvé a elegirla."));
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(MensajesError.DATOS_INVALIDOS, errores);
        }
    }

    /** De 3 a 4 opciones, exactamente una correcta y sin ordenCorrecto. */
    private static boolean opcionMultipleValida(List<OpcionRequest> opciones) {
        long correctas = opciones.stream().filter(o -> Boolean.TRUE.equals(o.esCorrecta())).count();
        boolean sinOrden = opciones.stream().allMatch(o -> o.ordenCorrecto() == null);
        return opciones.size() >= 3 && opciones.size() <= 4 && correctas == 1 && sinOrden;
    }

    /** De 3 a 5 pasos, posiciones 1..n todas distintas y ninguno marcado como correcto. */
    private static boolean ordenarValido(List<OpcionRequest> opciones) {
        int n = opciones.size();
        if (n < 3 || n > 5) {
            return false;
        }
        Set<Integer> posiciones = new HashSet<>();
        for (OpcionRequest opcion : opciones) {
            Integer posicion = opcion.ordenCorrecto();
            if (posicion == null || posicion < 1 || posicion > n || !posiciones.add(posicion)
                    || Boolean.TRUE.equals(opcion.esCorrecta())) {
                return false;
            }
        }
        return true;
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String sinBlancos(String valor) {
        return estaVacio(valor) ? null : Objects.requireNonNull(valor).strip();
    }
}

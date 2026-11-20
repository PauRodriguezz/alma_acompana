package ar.edu.utn.frm.gelab.alma.microcapsulas.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.ErrorCampo;
import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.common.MensajesError;
import ar.edu.utn.frm.gelab.alma.common.exception.DatosInvalidosException;
import ar.edu.utn.frm.gelab.alma.common.exception.RecursoNoEncontradoException;
import ar.edu.utn.frm.gelab.alma.common.imagenes.RutaImagen;
import ar.edu.utn.frm.gelab.alma.microcapsulas.domain.Microcapsula;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaRequest;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.dto.MicrocapsulaResumenResponse;
import ar.edu.utn.frm.gelab.alma.microcapsulas.repository.MicrocapsulaRepository;

/**
 * Gestión de microcápsulas desde el panel de operador (CU-11). Publicación inmediata y baja lógica.
 */
@Service
public class MicrocapsulaService {

    static final String CAMPOS_REQUERIDOS = "Completá todos los campos requeridos de la microcápsula.";

    private static final int MAX_TITULO = 120;
    private static final int MAX_TEXTO = 5000;

    static final String FORMATO_NO_PERMITIDO = "El texto tiene un formato que no se puede usar. "
            + "Usá solo título, subtítulo, negrita, cursiva, listas y destacado.";

    /**
     * El texto es Markdown limitado: títulos (##), subtítulos (###), negrita, cursiva, listas y
     * destacado (>). Se rechaza lo demás: HTML, enlaces, imágenes, código, líneas horizontales y los
     * demás niveles de título. Un "<" o un ">" sueltos, o "<5", son texto normal.
     */
    private static final List<Pattern> FORMATOS_NO_PERMITIDOS = List.of(
            Pattern.compile("<[!?]"),
            Pattern.compile("</?[A-Za-z][A-Za-z0-9-]*(\\s[^<>]*)?/?>"),
            Pattern.compile("<[A-Za-z][A-Za-z0-9+.-]*:[^\\s<>]*>"),
            Pattern.compile("\\[[^\\]\\n]*\\]\\([^)\\n]*\\)"),
            Pattern.compile("\\[[^\\]\\n]+\\]\\[[^\\]\\n]*\\]"),
            Pattern.compile("^ {0,3}\\[[^\\]\\n]+\\]:\\s*\\S", Pattern.MULTILINE),
            Pattern.compile("`|^ {0,3}~~~", Pattern.MULTILINE),
            Pattern.compile("^ {0,3}([-*_])( *\\1){2,} *$", Pattern.MULTILINE),
            Pattern.compile("^ {0,3}=+ *$", Pattern.MULTILINE),
            Pattern.compile("^ {0,3}(#|#{4,6})([ \\t]|$)", Pattern.MULTILINE));

    private final MicrocapsulaRepository microcapsulaRepository;

    public MicrocapsulaService(MicrocapsulaRepository microcapsulaRepository) {
        this.microcapsulaRepository = microcapsulaRepository;
    }

    @Transactional(readOnly = true)
    public List<MicrocapsulaResumenResponse> listar() {
        return microcapsulaRepository.findByFechaBajaIsNullOrderByFechaPublicacionDescIdDesc().stream()
                .map(MicrocapsulaResumenResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public MicrocapsulaResponse obtener(Long id) {
        return MicrocapsulaResponse.de(vigente(id));
    }

    @Transactional
    public MicrocapsulaResponse crear(MicrocapsulaRequest datos) {
        validar(datos);
        Microcapsula microcapsula = new Microcapsula();
        copiar(datos, microcapsula);
        return MicrocapsulaResponse.de(microcapsulaRepository.save(microcapsula));
    }

    @Transactional
    public MicrocapsulaResponse actualizar(Long id, MicrocapsulaRequest datos) {
        Microcapsula microcapsula = vigente(id);
        validar(datos);
        copiar(datos, microcapsula);
        return MicrocapsulaResponse.de(microcapsula);
    }

    /** Baja lógica: queda la fila con la fecha de baja; no se tocan las favoritas. */
    @Transactional
    public void darDeBaja(Long id) {
        vigente(id).setFechaBaja(Fechas.hoy());
    }

    private Microcapsula vigente(Long id) {
        return microcapsulaRepository.findByIdAndFechaBajaIsNull(id)
                .orElseThrow(RecursoNoEncontradoException::new);
    }

    private static void copiar(MicrocapsulaRequest datos, Microcapsula microcapsula) {
        microcapsula.setTitulo(datos.titulo().strip());
        microcapsula.setTexto(datos.texto().strip());
        microcapsula.setCategoria(datos.categoria());
        microcapsula.setImagen(sinBlancos(datos.imagen()));
        microcapsula.setFechaPublicacion(datos.fechaPublicacion());
    }

    /**
     * Primero los obligatorios (detail de CU-11); si están completos, el resto de las reglas
     * con el detail genérico.
     */
    private static void validar(MicrocapsulaRequest datos) {
        List<ErrorCampo> faltantes = new ArrayList<>();
        if (estaVacio(datos.titulo())) {
            faltantes.add(new ErrorCampo("titulo", "Ingresá el título."));
        }
        if (estaVacio(datos.texto())) {
            faltantes.add(new ErrorCampo("texto", "Ingresá el texto."));
        }
        if (datos.categoria() == null) {
            faltantes.add(new ErrorCampo("categoria", "Elegí una categoría."));
        }
        if (datos.fechaPublicacion() == null) {
            faltantes.add(new ErrorCampo("fechaPublicacion", "Ingresá la fecha de publicación."));
        }
        if (!faltantes.isEmpty()) {
            throw new DatosInvalidosException(CAMPOS_REQUERIDOS, faltantes);
        }

        List<ErrorCampo> errores = new ArrayList<>();
        if (datos.titulo().strip().length() > MAX_TITULO) {
            errores.add(new ErrorCampo("titulo", "El título puede tener hasta " + MAX_TITULO + " caracteres."));
        }
        if (datos.texto().strip().length() > MAX_TEXTO) {
            errores.add(new ErrorCampo("texto", "El texto puede tener hasta " + MAX_TEXTO + " caracteres."));
        }
        if (usaFormatoNoPermitido(datos.texto())) {
            errores.add(new ErrorCampo("texto", FORMATO_NO_PERMITIDO));
        }
        String imagen = sinBlancos(datos.imagen());
        if (imagen != null && !RutaImagen.esValida(imagen)) {
            errores.add(new ErrorCampo("imagen", "La imagen no es válida. Volvé a elegirla."));
        }
        if (datos.fechaPublicacion().isAfter(Fechas.hoy())) {
            errores.add(new ErrorCampo("fechaPublicacion", "La fecha de publicación no puede ser futura."));
        }
        if (!errores.isEmpty()) {
            throw new DatosInvalidosException(MensajesError.DATOS_INVALIDOS, errores);
        }
    }

    private static boolean usaFormatoNoPermitido(String texto) {
        return FORMATOS_NO_PERMITIDOS.stream().anyMatch(patron -> patron.matcher(texto).find());
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Una imagen vacía ("" o espacios) cuenta como "sin imagen". */
    private static String sinBlancos(String valor) {
        return estaVacio(valor) ? null : valor.strip();
    }
}

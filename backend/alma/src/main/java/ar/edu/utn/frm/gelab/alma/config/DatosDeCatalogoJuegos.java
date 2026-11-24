package ar.edu.utn.frm.gelab.alma.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frm.gelab.alma.common.Fechas;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.OpcionRespuesta;
import ar.edu.utn.frm.gelab.alma.ejercicios.repository.EjercicioRepository;

/**
 * Ejercicios de prueba para el juego (contrato de juegos, anexo A): 18 propios más los 36 de
 * {@link EjerciciosNuevos}, que se suman a los 3 que quedan en DatosDeCatalogo (57 en total), para tener al
 * menos 2 por categoría y nivel. Solo con alma.seed.enabled=true.
 * Es idempotente por categoría, nivel y consigna, aunque el ejercicio esté dado de baja (no se recrea lo que
 * se eliminó).
 */
@Component
@ConditionalOnProperty(name = "alma.seed.enabled", havingValue = "true")
public class DatosDeCatalogoJuegos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDeCatalogoJuegos.class);

    private final EjercicioRepository ejercicioRepository;

    public DatosDeCatalogoJuegos(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int creados = refranes() + series() + asociaciones() + memoria() + nuevos();
        if (creados > 0) {
            log.info("Catálogo de juegos: {} ejercicios creados", creados);
        }
    }

    private int refranes() {
        CategoriaEjercicio c = CategoriaEjercicio.REFRAN;
        int creados = 0;
        creados += opcionMultiple(c, NivelDificultad.BASICO,
                "Completá el refrán: «A caballo regalado…»",
                "Quiere decir que un regalo se agradece sin fijarse en sus defectos. "
                        + "El refrán completo es: «A caballo regalado no se le miran los dientes».",
                null,
                "…no se le miran los dientes", "…se le da de comer", "…se le pone montura");
        creados += opcionMultiple(c, NivelDificultad.BASICO,
                "Completá el refrán: «Al que madruga…»",
                "Quiere decir que levantarse temprano ayuda a hacer más cosas. "
                        + "El refrán completo es: «Al que madruga Dios lo ayuda».",
                null,
                "…Dios lo ayuda", "…se le hace tarde", "…le da sueño");
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "Completá el refrán: «Camarón que se duerme…»",
                "Quiere decir que quien se descuida pierde la oportunidad. "
                        + "El refrán completo es: «Camarón que se duerme se lo lleva la corriente».",
                null,
                "…se lo lleva la corriente", "…sueña con el mar", "…nada más rápido", "…encuentra más comida");
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "Completá el refrán: «En casa de herrero…»",
                "Quiere decir que a veces quien sabe hacer algo no lo tiene resuelto en su propia casa. "
                        + "El refrán completo es: «En casa de herrero, cuchillo de palo».",
                null,
                "…cuchillo de palo", "…siempre hay fuego", "…todo está en orden", "…nunca falta trabajo");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "Completá el refrán: «Quien a buen árbol se arrima…»",
                "Quiere decir que acercarse a buenas personas trae beneficios. "
                        + "El refrán completo es: «Quien a buen árbol se arrima, buena sombra lo cobija».",
                null,
                "…buena sombra lo cobija", "…buen fruto le cae", "…nunca le falta el sol");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "Completá el refrán: «El que mucho abarca…»",
                "Quiere decir que quien quiere hacer demasiado a la vez termina haciendo poco. "
                        + "El refrán completo es: «El que mucho abarca poco aprieta».",
                null,
                "…poco aprieta", "…todo lo pierde", "…nunca descansa", "…más gana");
        return creados;
    }

    private int series() {
        CategoriaEjercicio c = CategoriaEjercicio.SERIE;
        int creados = 0;
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "¿Qué número sigue? 3, 6, 12, 24…",
                "Cada número es el doble del anterior.",
                null,
                "48", "36", "30", "27");
        creados += ordenar(c, NivelDificultad.INTERMEDIO,
                "Ordená estos meses, del primero al último del año",
                "En el año vienen en este orden: marzo, junio, septiembre y diciembre.",
                "Marzo", "Junio", "Septiembre", "Diciembre");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "¿Qué número sigue? 1, 4, 9, 16…",
                "Son los números multiplicados por sí mismos: 1×1, 2×2, 3×3, 4×4. Sigue 5×5, que es 25.",
                null,
                "25", "20", "24", "32");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "¿Qué número sigue? 2, 3, 5, 8, 12…",
                "Se suma 1, después 2, después 3, después 4, y ahora toca sumar 5.",
                null,
                "17", "16", "15", "18");
        return creados;
    }

    private int asociaciones() {
        CategoriaEjercicio c = CategoriaEjercicio.ASOCIACION;
        int creados = 0;
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "¿Cuál es el intruso? Lunes, Martes, Julio, Viernes",
                "Lunes, martes y viernes son días de la semana. Julio es un mes.",
                null,
                "Julio", "Lunes", "Martes", "Viernes");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "¿Cuál es el intruso? Rosa, Margarita, Tulipán, Pino",
                "Rosa, margarita y tulipán son flores. El pino es un árbol.",
                null,
                "Pino", "Rosa", "Margarita", "Tulipán");
        return creados;
    }

    private int memoria() {
        CategoriaEjercicio c = CategoriaEjercicio.MEMORIA;
        int creados = 0;
        creados += opcionMultiple(c, NivelDificultad.BASICO,
                "¿Qué día es la cita con el médico?",
                "La cita era el martes. Repetir en voz alta ayuda a recordar.",
                "Leé con atención: tu cita con el médico es el martes a las cuatro de la tarde.",
                "Martes", "Jueves", "Sábado");
        creados += opcionMultiple(c, NivelDificultad.BASICO,
                "¿Cuál de estas cosas compró Marta?",
                "Marta compró manzanas, pan y queso.",
                "Leé con atención: hoy Marta compró manzanas, pan y queso.",
                "Queso", "Pescado", "Fideos", "Leche");
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "¿Cómo se llama el perro de Lucía?",
                "El perro de Lucía se llama Bruno. El gato es Tito.",
                "Leé con atención: Lucía tiene un perro que se llama Bruno y un gato que se llama Tito.",
                "Bruno", "Tito", "Lucho");
        creados += opcionMultiple(c, NivelDificultad.INTERMEDIO,
                "¿Cuál de estas cosas estaba en la lista?",
                "En la lista estaban el lápiz, la tijera, la cinta, la regla y el sobre.",
                "Leé con atención esta lista: lápiz, tijera, cinta, regla y sobre.",
                "Tijera", "Pincel", "Goma", "Compás");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "¿Qué postre llevamos?",
                "El postre era de durazno.",
                "Leé con atención: el domingo vamos a almorzar a lo de Rosa. "
                        + "Llevamos un postre de durazno y una botella de agua.",
                "De durazno", "De frutilla", "De manzana", "De banana");
        creados += opcionMultiple(c, NivelDificultad.AVANZADO,
                "¿Cuál de estos ingredientes estaba en la lista?",
                "En la lista estaban la harina, el azúcar, los huevos, la manteca, la vainilla y el limón.",
                "Leé con atención esta lista: harina, azúcar, huevos, manteca, vainilla y limón.",
                "Vainilla", "Chocolate", "Canela", "Leche");
        return creados;
    }

    /** Los ejercicios de {@link EjerciciosNuevos}: los carga con los mismos métodos que el resto. */
    private int nuevos() {
        int creados = 0;
        for (EjercicioSemilla semilla : EjerciciosNuevos.TODOS) {
            String[] opciones = semilla.opciones().toArray(String[]::new);
            creados += semilla.formato() == FormatoEjercicio.ORDENAR
                    ? ordenar(semilla.categoria(), semilla.nivelDificultad(), semilla.consigna(),
                            semilla.explicacion(), opciones)
                    : opcionMultiple(semilla.categoria(), semilla.nivelDificultad(), semilla.consigna(),
                            semilla.explicacion(), semilla.textoAMemorizar(), opciones);
        }
        return creados;
    }

    /** La primera opción es la correcta (el juego las mezcla al mostrarlas). */
    private int opcionMultiple(CategoriaEjercicio categoria, NivelDificultad nivel, String consigna,
                               String explicacion, String textoAMemorizar, String... opciones) {
        if (ejercicioRepository.existsByCategoriaAndNivelDificultadAndConsigna(categoria, nivel, consigna)) {
            return 0;
        }
        Ejercicio ejercicio = ejercicio(categoria, FormatoEjercicio.OPCION_MULTIPLE, nivel, consigna,
                explicacion, textoAMemorizar);
        for (int i = 0; i < opciones.length; i++) {
            ejercicio.getOpciones().add(opcion(opciones[i], i == 0, null));
        }
        ejercicioRepository.save(ejercicio);
        return 1;
    }

    /** Los pasos van en el orden correcto (ordenCorrecto 1..n). */
    private int ordenar(CategoriaEjercicio categoria, NivelDificultad nivel, String consigna,
                        String explicacion, String... pasos) {
        if (ejercicioRepository.existsByCategoriaAndNivelDificultadAndConsigna(categoria, nivel, consigna)) {
            return 0;
        }
        Ejercicio ejercicio = ejercicio(categoria, FormatoEjercicio.ORDENAR, nivel, consigna, explicacion, null);
        for (int i = 0; i < pasos.length; i++) {
            ejercicio.getOpciones().add(opcion(pasos[i], false, i + 1));
        }
        ejercicioRepository.save(ejercicio);
        return 1;
    }

    private static Ejercicio ejercicio(CategoriaEjercicio categoria, FormatoEjercicio formato, NivelDificultad nivel,
                                       String consigna, String explicacion, String textoAMemorizar) {
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setCategoria(categoria);
        ejercicio.setFormato(formato);
        ejercicio.setNivelDificultad(nivel);
        ejercicio.setConsigna(consigna);
        ejercicio.setExplicacion(explicacion);
        ejercicio.setTextoAMemorizar(textoAMemorizar);
        ejercicio.setFechaAlta(Fechas.hoy());
        return ejercicio;
    }

    private static OpcionRespuesta opcion(String texto, boolean esCorrecta, Integer ordenCorrecto) {
        OpcionRespuesta opcion = new OpcionRespuesta();
        opcion.setTexto(texto);
        opcion.setEsCorrecta(esCorrecta);
        opcion.setOrdenCorrecto(ordenCorrecto);
        return opcion;
    }
}

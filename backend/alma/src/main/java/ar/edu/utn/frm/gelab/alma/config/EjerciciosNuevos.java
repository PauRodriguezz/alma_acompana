package ar.edu.utn.frm.gelab.alma.config;

import java.util.List;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.CategoriaEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.FormatoEjercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;

/**
 * Los 36 ejercicios nuevos del seed (contrato de juegos, anexo A). Los carga {@link DatosDeCatalogoJuegos}.
 * En opción múltiple la primera opción es la correcta (el back las mezcla al mostrarlas); en ordenar, los
 * pasos van en el orden correcto. Los usa también EjerciciosNuevosTest, con el validador del servicio.
 */
public final class EjerciciosNuevos {

    public static final List<EjercicioSemilla> TODOS = List.of(
            // 1
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "Completá el refrán: «Ojos que no ven…»",
                    "Quiere decir que lo que no vemos no nos preocupa. El refrán completo es: «Ojos que no "
                            + "ven, corazón que no siente».",
                    null,
                    List.of("…corazón que no siente", "…boca que no habla", "…oídos que no escuchan")),
            // 2
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "Completá el refrán: «Dime con quién andas…»",
                    "Quiere decir que las compañías de una persona muestran cómo es. El refrán completo es: "
                            + "«Dime con quién andas y te diré quién eres».",
                    null,
                    List.of("…y te diré quién eres", "…y te diré adónde vas", "…y te daré un consejo")),
            // 3
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "Completá el refrán: «Perro que ladra…»",
                    "Quiere decir que quien amenaza mucho casi nunca lo cumple. El refrán completo es: "
                            + "«Perro que ladra no muerde».",
                    null,
                    List.of("…no muerde", "…se cansa", "…cuida la casa", "…tiene hambre")),
            // 4
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "Completá el refrán: «A mal tiempo…»",
                    "Quiere decir que ante las dificultades conviene mantener el buen ánimo. El refrán "
                            + "completo es: «A mal tiempo, buena cara».",
                    null,
                    List.of("…buena cara", "…paraguas", "…más abrigo", "…puerta cerrada")),
            // 5
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué quiere decir «Más vale tarde que nunca»?",
                    "Quiere decir que, aunque llegues tarde, es mejor hacer algo que no hacerlo.",
                    null,
                    List.of("Que es mejor hacer algo tarde que no hacerlo", "Que conviene llegar siempre tarde", "Que lo que se hace tarde no sirve", "Que nunca hay que apurarse")),
            // 6
            new EjercicioSemilla(CategoriaEjercicio.REFRAN, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué quiere decir «Del dicho al hecho hay mucho trecho»?",
                    "Quiere decir que decir algo es mucho más fácil que hacerlo.",
                    null,
                    List.of("Que decir algo es más fácil que hacerlo", "Que hay que hablar menos", "Que los caminos son largos", "Que lo que se dice siempre se cumple")),
            // 7
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "¿Qué número sigue? 100, 95, 90, 85…",
                    "Cada número es 5 menos que el anterior. Después del 85 viene el 80.",
                    null,
                    List.of("80", "75", "85", "70")),
            // 8
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "¿Qué número sigue? 7, 14, 21, 28…",
                    "Cada número es 7 más que el anterior. Después del 28 viene el 35.",
                    null,
                    List.of("35", "32", "42", "36")),
            // 9
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué letra sigue? A, C, E, G…",
                    "La serie va saltando una letra: A, C, E, G, I.",
                    null,
                    List.of("I", "H", "J", "K")),
            // 10
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué número sigue? 2, 6, 18, 54…",
                    "Cada número es el triple del anterior. 54 por 3 es 162.",
                    null,
                    List.of("162", "108", "72", "148")),
            // 11
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué mes sigue? Enero, Marzo, Mayo, Julio…",
                    "La serie va saltando un mes: enero, marzo, mayo, julio, septiembre.",
                    null,
                    List.of("Septiembre", "Agosto", "Octubre", "Noviembre")),
            // 12
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué número sigue? 2, 5, 3, 6, 4, 7…",
                    "Hay dos series mezcladas: 2, 3, 4 y 5, 6, 7. Después del 7 vuelve la primera serie y "
                            + "sigue el 5.",
                    null,
                    List.of("5", "8", "4", "6")),
            // 13
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué número sigue? 1, 1, 2, 3, 5, 8…",
                    "Cada número es la suma de los dos anteriores. 5 más 8 es 13.",
                    null,
                    List.of("13", "11", "12", "16")),
            // 14
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.ORDENAR,
                    NivelDificultad.BASICO,
                    "Ordená estos números de menor a mayor",
                    "Primero va el número de una cifra, después los de dos y al final el de tres.",
                    null,
                    List.of("9", "19", "90", "109")),
            // 15
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.ORDENAR,
                    NivelDificultad.INTERMEDIO,
                    "Ordená estas fechas del año, de la primera a la última",
                    "Mayo viene antes que julio, y diciembre cierra el año. Dentro de mayo, el 1 va antes "
                            + "que el 25.",
                    null,
                    List.of("1 de mayo", "25 de mayo", "9 de julio", "25 de diciembre")),
            // 16
            new EjercicioSemilla(CategoriaEjercicio.SERIE, FormatoEjercicio.ORDENAR,
                    NivelDificultad.AVANZADO,
                    "Ordená estos números de menor a mayor",
                    "Primero van los de dos cifras (35 y 53) y después los de tres (305 y 530).",
                    null,
                    List.of("35", "53", "305", "530")),
            // 17
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "¿Cuál es el intruso? Panadería, Carnicería, Farmacia, Verdulería",
                    "En las otras tres se compran alimentos. La farmacia vende medicamentos.",
                    null,
                    List.of("Farmacia", "Panadería", "Carnicería", "Verdulería")),
            // 18
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "¿Qué tienen en común el paraguas, el impermeable y las botas de goma?",
                    "Los tres sirven para protegerse de la lluvia.",
                    null,
                    List.of("Protegen de la lluvia", "Protegen del sol", "Protegen del frío", "Protegen del viento")),
            // 19
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Cuál es el intruso? Martillo, Serrucho, Destornillador, Cuchara",
                    "Los otros tres son herramientas de taller. La cuchara se usa en la cocina.",
                    null,
                    List.of("Cuchara", "Martillo", "Serrucho", "Destornillador")),
            // 20
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué tienen en común un reloj, un calendario y una agenda?",
                    "Los tres nos ayudan a saber qué hacemos y cuándo.",
                    null,
                    List.of("Sirven para organizar el tiempo", "Sirven para cocinar", "Sirven para viajar", "Sirven para abrigarse")),
            // 21
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué tienen en común el tango, la zamba y el chamamé?",
                    "Los tres son ritmos y bailes tradicionales de la Argentina.",
                    null,
                    List.of("Son ritmos de música argentina", "Son bebidas típicas", "Son deportes de equipo", "Son comidas regionales")),
            // 22
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Cuál de estos NO es un río argentino? Paraná, Uruguay, Colorado, Amazonas",
                    "El Amazonas está en Brasil y en otros países. Los otros tres forman parte de la "
                            + "geografía argentina.",
                    null,
                    List.of("Amazonas", "Paraná", "Uruguay", "Colorado")),
            // 23
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué tienen en común una receta, un mapa y un manual de instrucciones?",
                    "Los tres nos orientan para hacer algo o llegar a algún lugar.",
                    null,
                    List.of("Nos guían para llegar a algo siguiendo indicaciones", "Se guardan en el botiquín", "Se usan solo en la cocina", "Sirven para escribir cartas")),
            // 24
            new EjercicioSemilla(CategoriaEjercicio.ASOCIACION, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué palabra significa lo contrario de «escaso»?",
                    "Escaso es lo que hay poco. Lo contrario es lo que hay mucho: abundante.",
                    null,
                    List.of("Abundante", "Barato", "Pequeño", "Lento")),
            // 25
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.BASICO,
                    "¿Qué día es el turno del dentista?",
                    "Hoy es martes, así que el turno es mañana, el miércoles.",
                    "Leé con atención: hoy es martes y mañana tenés el turno del dentista a las diez.",
                    List.of("El miércoles", "El martes", "El jueves")),
            // 26
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿A qué hora pasa el colectivo 41?",
                    "El 41 pasa a las 7:30. El 25 es el que pasa a las 7:10.",
                    "Leé con atención: el colectivo 25 pasa a las 7:10 y el 41 pasa a las 7:30.",
                    List.of("A las 7:30", "A las 7:10", "A las 7:25", "A las 7:40")),
            // 27
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Dónde es la reunión del club?",
                    "La reunión es en el salón principal.",
                    "Leé con atención: la reunión del club es el segundo viernes de cada mes, a las cinco de "
                            + "la tarde, en el salón principal.",
                    List.of("En el salón principal", "En el patio", "En la biblioteca", "En el comedor")),
            // 28
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué se agrega en segundo lugar?",
                    "Primero se baten los huevos, en segundo lugar se agrega el azúcar y al final la harina.",
                    "Leé con atención: la receta empieza batiendo los huevos, después se agrega el azúcar y "
                            + "al final la harina.",
                    List.of("El azúcar", "La harina", "Los huevos", "La leche")),
            // 29
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Con qué números termina el teléfono de la farmacia?",
                    "El teléfono termina en 4721.",
                    "Leé con atención: el teléfono de la farmacia termina en 4721.",
                    List.of("4721", "4712", "4217", "7421")),
            // 30
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Qué se olvidó Rosa?",
                    "Rosa se olvidó la billetera y volvió a buscarla.",
                    "Leé con atención: esta mañana Rosa fue a la feria con su hermano. Compró zapallo y un "
                            + "queso de cabra, pero se olvidó la billetera y volvió a buscarla.",
                    List.of("La billetera", "El zapallo", "Las llaves", "El queso")),
            // 31
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.INTERMEDIO,
                    "¿Cuántos nietos tienen entre los dos?",
                    "Marta tiene tres y Pedro dos. Entre los dos tienen cinco nietos.",
                    "Leé con atención: Marta tiene tres nietos y Pedro tiene dos.",
                    List.of("Cinco", "Cuatro", "Seis", "Tres")),
            // 32
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Qué producto venía justo después del azúcar?",
                    "El orden era: sal, azúcar, aceite, vinagre y pimienta. Después del azúcar venía el "
                            + "aceite.",
                    "Leé con atención esta lista: sal, azúcar, aceite, vinagre y pimienta.",
                    List.of("El aceite", "El vinagre", "La sal", "La pimienta")),
            // 33
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Se puede comprar pan el jueves a las cinco de la tarde?",
                    "La panadería cierra a las 13 y a la tarde no atiende, así que a las cinco de la tarde "
                            + "ya está cerrada.",
                    "Leé con atención: el jueves la panadería abre a las 8 y cierra a las 13. A la tarde no "
                            + "atiende.",
                    List.of("No, porque ya cerró", "Sí, porque abre todo el día", "Sí, hasta las ocho de la noche", "No, porque no abre los jueves")),
            // 34
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Cuánto gasté en total?",
                    "Las manzanas costaron 1000 y las peras 1500. En total gasté 2500 pesos.",
                    "Leé con atención: en la feria compré un kilo de manzanas a 1000 pesos y un kilo de "
                            + "peras a 1500 pesos.",
                    List.of("2500 pesos", "2000 pesos", "3000 pesos", "1500 pesos")),
            // 35
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Cuántos años tiene Julio?",
                    "Marta tiene 70 y Julio tiene 5 años menos, o sea 65.",
                    "Leé con atención: Marta tiene 70 años y su hermano Julio tiene 5 años menos.",
                    List.of("65", "75", "60", "70")),
            // 36
            new EjercicioSemilla(CategoriaEjercicio.MEMORIA, FormatoEjercicio.OPCION_MULTIPLE,
                    NivelDificultad.AVANZADO,
                    "¿Cuál de estas cosas NO estaba en la lista?",
                    "Las peras no estaban en la lista. Las otras tres sí estaban.",
                    "Leé con atención esta lista de la feria: zapallo, choclo, ciruelas, queso de cabra, "
                            + "miel y nueces.",
                    List.of("Peras", "Miel", "Choclo", "Nueces"))
    );

    private EjerciciosNuevos() {
    }
}

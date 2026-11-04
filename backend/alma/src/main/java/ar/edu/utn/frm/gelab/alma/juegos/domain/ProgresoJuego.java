package ar.edu.utn.frm.gelab.alma.juegos.domain;

import java.time.LocalDate;

import ar.edu.utn.frm.gelab.alma.ejercicios.domain.Ejercicio;
import ar.edu.utn.frm.gelab.alma.ejercicios.domain.NivelDificultad;
import ar.edu.utn.frm.gelab.alma.persona.domain.PersonaMayor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Progreso del juego de una persona mayor (DC: ProgresoJuego, CU-05). Se crea con el primer intento:
 * pedir el siguiente ejercicio no escribe nada (contrato de juegos, sección 2).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProgresoJuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_mayor_id", nullable = false, unique = true)
    private PersonaMayor personaMayor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelDificultad nivelActual = NivelDificultad.BASICO;

    @Column(nullable = false)
    private int aciertosConsecutivos;

    @Column(nullable = false)
    private int erroresConsecutivos;

    private LocalDate fechaUltimaSesion;

    /** Último ejercicio respondido; null si todavía no jugó ninguno. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ultimo_ejercicio_id")
    private Ejercicio ultimoEjercicio;

    static final int ACIERTOS_PARA_SUBIR = 3;
    static final int ERRORES_PARA_BAJAR = 2;

    /**
     * Suma un acierto y corta la racha de errores. Con 3 seguidos sube un nivel (si no está en AVANZADO)
     * y los dos contadores vuelven a 0 (contrato de juegos, 5.5).
     */
    public void registrarAcierto() {
        aciertosConsecutivos++;
        erroresConsecutivos = 0;
        if (aciertosConsecutivos >= ACIERTOS_PARA_SUBIR) {
            nivelActual = switch (nivelActual) {
                case BASICO -> NivelDificultad.INTERMEDIO;
                case INTERMEDIO, AVANZADO -> NivelDificultad.AVANZADO;
            };
            reiniciarContadores();
        }
    }

    /**
     * Suma un error y corta la racha de aciertos. Con 2 seguidos baja un nivel (si no está en BASICO)
     * y los dos contadores vuelven a 0 (contrato de juegos, 5.5).
     */
    public void registrarError() {
        erroresConsecutivos++;
        aciertosConsecutivos = 0;
        if (erroresConsecutivos >= ERRORES_PARA_BAJAR) {
            nivelActual = switch (nivelActual) {
                case AVANZADO -> NivelDificultad.INTERMEDIO;
                case INTERMEDIO, BASICO -> NivelDificultad.BASICO;
            };
            reiniciarContadores();
        }
    }

    private void reiniciarContadores() {
        aciertosConsecutivos = 0;
        erroresConsecutivos = 0;
    }
}

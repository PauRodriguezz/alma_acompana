package ar.edu.utn.frm.gelab.alma.ejercicios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Opción de un ejercicio (DC: OpcionRespuesta). Pertenece a un Ejercicio (composición).
 * En OPCION_MULTIPLE importa esCorrecta; en ORDENAR, ordenCorrecto.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OpcionRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Obligatorio aunque haya imagen: sirve de descripción accesible. */
    @Column(nullable = false, length = 150)
    private String texto;

    @Column(nullable = false)
    private Boolean esCorrecta;

    /** Ruta devuelta por la subida de imágenes ("/imagenes/..."), o null. */
    @Column(length = 255)
    private String imagen;

    /** Posición 1..n en ORDENAR; null en OPCION_MULTIPLE. */
    private Integer ordenCorrecto;
}

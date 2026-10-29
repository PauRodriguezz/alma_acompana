package ar.edu.utn.frm.gelab.alma.microcapsulas.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Microcápsula de cuidado y prevención (DC: Microcapsula, CU-11).
 * Se publica al guardarla; la baja es lógica (fechaBaja).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Microcapsula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 5000)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaMicrocapsula categoria;

    /** Ruta devuelta por la subida de imágenes ("/imagenes/..."), o null. */
    @Column(length = 255)
    private String imagen;

    @Column(nullable = false)
    private LocalDate fechaPublicacion;

    private LocalDate fechaBaja;
}

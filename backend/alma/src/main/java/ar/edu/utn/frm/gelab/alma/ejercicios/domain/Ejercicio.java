package ar.edu.utn.frm.gelab.alma.ejercicios.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ejercicio del catálogo de estimulación cognitiva (DC: Ejercicio, CU-10).
 * La baja es lógica: se completa fechaBaja y la fila queda.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaEjercicio categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormatoEjercicio formato;

    @Column(nullable = false, length = 300)
    private String consigna;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelDificultad nivelDificultad;

    @Column(length = 500)
    private String explicacion;

    /** Solo para la categoría MEMORIA; en las demás queda null. */
    // Nombre explícito: la estrategia por defecto generaría "textoamemorizar" (DC: texto_a_memorizar).
    @Column(name = "texto_a_memorizar", length = 1000)
    private String textoAMemorizar;

    @Column(nullable = false)
    private LocalDate fechaAlta;

    private LocalDate fechaBaja;

    /** Composición 1 a 1..*: las opciones viven y mueren con el ejercicio. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ejercicio_id", nullable = false)
    @OrderBy("id ASC")
    private List<OpcionRespuesta> opciones = new ArrayList<>();
}

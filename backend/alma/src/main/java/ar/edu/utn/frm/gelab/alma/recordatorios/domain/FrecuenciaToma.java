package ar.edu.utn.frm.gelab.alma.recordatorios.domain;

/**
 * Cada cuánto se toma una medicación (DC: FrecuenciaToma).
 */
public enum FrecuenciaToma {

    DIARIA(1),
    DIA_POR_MEDIO(2),
    SEMANAL(7);

    private final int saltoDias;

    FrecuenciaToma(int saltoDias) {
        this.saltoDias = saltoDias;
    }

    /** Días entre una toma y la siguiente (contrato, 5.1). */
    public int getSaltoDias() {
        return saltoDias;
    }
}

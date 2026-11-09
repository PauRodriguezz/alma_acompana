package ar.edu.utn.frm.gelab.alma.historia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ar.edu.utn.frm.gelab.alma.auth.repository.CuentaRepository;
import ar.edu.utn.frm.gelab.alma.historia.repository.EscrituraDelGrafo;
import ar.edu.utn.frm.gelab.alma.persona.repository.PersonaMayorRepository;

/**
 * La historia de ejemplo de {@code ana} (contrato del grafo, sección 2, "Datos de ejemplo"). Con el seed
 * activo (el valor por defecto), {@code CargaInicialDelGrafo} la carga al arrancar. Esta prueba SOLO LEE:
 * no escribe ni borra nada de {@code ana}, y no depende de su progreso, que se puede avanzar desde Swagger.
 */
@SpringBootTest
class CargaDeAnaTest {

    @Autowired
    private CuentaRepository cuentaRepository;
    @Autowired
    private PersonaMayorRepository personaMayorRepository;
    @Autowired
    private EscrituraDelGrafo escritura;

    @Test
    void anaTieneHistoriaDespuesDeArrancarConElSeedActivo() {
        Long anaId = cuentaRepository.findByNombreUsuario("ana")
                .flatMap(cuenta -> personaMayorRepository.findByCuentaId(cuenta.getId()))
                .orElseThrow().getId();

        // O la cargó el arranque, o ya tenía historia propia (recuerdos o progreso): en los dos casos tiene.
        assertThat(escritura.tieneRecuerdosOProgreso(anaId)).isTrue();
    }
}

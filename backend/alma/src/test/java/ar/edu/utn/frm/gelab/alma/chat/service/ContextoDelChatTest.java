package ar.edu.utn.frm.gelab.alma.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ar.edu.utn.frm.gelab.alma.recordatorios.domain.EstadoNotificacion;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.CitaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.ItemAgendaResponse;
import ar.edu.utn.frm.gelab.alma.recordatorios.dto.TipoRecordatorio;

/**
 * Formato de las líneas de cita que recibe el modelo (contrato de chat, 5.4). Sin base de datos: los
 * días de la semana se comparan con valores escritos a mano (el 05/11/2026 es jueves), no calculados.
 */
class ContextoDelChatTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 5);

    private static ItemAgendaResponse cita(String titulo, String especialidad, String medico, String lugar,
            LocalDateTime fechaHora, EstadoNotificacion estado) {
        CitaResponse cita = new CitaResponse(1L, TipoRecordatorio.CITA, fechaHora.minusDays(3), titulo, especialidad,
                medico, lugar, fechaHora, "Llevar los estudios y venir en ayunas.", 7L, estado);
        return new ItemAgendaResponse(7L, fechaHora, estado, null, false, cita);
    }

    private static ItemAgendaResponse cita(String medico, String lugar, LocalDateTime fechaHora) {
        return cita("Control de espalda", "Traumatología", medico, lugar, fechaHora, EstadoNotificacion.PENDIENTE);
    }

    // ---------- Citas próximas ----------

    @Test
    void mananaLlevaElDiaEntreParentesis() {
        ItemAgendaResponse item = cita("Dr. Pérez", "Clínica Sur", HOY.plusDays(1).atTime(16, 30));

        assertEquals("- mañana (viernes 06/11) a las 16:30, cita médica: Control de espalda, Traumatología, "
                + "con el Dr. Pérez, en Clínica Sur", ContextoDelChat.lineaDeCitaProxima(item, HOY));
    }

    @Test
    void otroDiaLlevaElNombreDelDiaYLaFechaSinMañana() {
        ItemAgendaResponse item = cita("Control de presión", "Cardiología", "Dra. Gómez", "Hospital Central",
                LocalDate.of(2026, 11, 10).atTime(9, 0), EstadoNotificacion.PENDIENTE);

        assertEquals("- martes 10/11 a las 09:00, cita médica: Control de presión, Cardiología, "
                + "con la Dra. Gómez, en Hospital Central", ContextoDelChat.lineaDeCitaProxima(item, HOY));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-11-07, sábado 07/11",
            "2026-11-08, domingo 08/11",
            "2026-11-09, lunes 09/11",
            "2026-11-10, martes 10/11",
            "2026-11-11, miércoles 11/11",
            "2026-11-12, jueves 12/11",
            "2026-11-13, viernes 13/11" })
    void losNombresDeDiaVanEnEspanolYEnMinuscula(String fecha, String esperado) {
        ItemAgendaResponse item = cita("Dr. Pérez", "Clínica Sur", LocalDate.parse(fecha).atTime(8, 5));

        String linea = ContextoDelChat.lineaDeCitaProxima(item, HOY);

        assertTrue(linea.startsWith("- " + esperado + " a las 08:05, cita médica: "), linea);
    }

    @Test
    void laLineaDeUnaCitaProximaNoLlevaEstadoNiIndicacionesPrevias() {
        for (EstadoNotificacion estado : EstadoNotificacion.values()) {
            ItemAgendaResponse item = cita("Control de espalda", "Traumatología", "Dr. Pérez", "Clínica Sur",
                    HOY.plusDays(2).atTime(10, 0), estado);

            String linea = ContextoDelChat.lineaDeCitaProxima(item, HOY);

            assertFalse(linea.contains("("), linea);
            assertFalse(linea.contains("pendiente") || linea.contains("ya vista"), linea);
            assertFalse(linea.contains("Llevar los estudios"), linea);
        }
    }

    // ---------- Médico y lugar ----------

    @ParameterizedTest
    @CsvSource({
            "Dra. Gómez, con la Dra. Gómez",
            "Dr. Pérez, con el Dr. Pérez",
            "dra. Gómez, con la dra. Gómez",
            "DR. Pérez, con el DR. Pérez",
            "Gómez, con Gómez",
            "Lic. Ruiz, con Lic. Ruiz",
            "Doctora Gómez, con Doctora Gómez" })
    void elArticuloDelMedicoDependeDeSuTitulo(String medico, String esperado) {
        ItemAgendaResponse item = cita(medico, "Clínica Sur", HOY.plusDays(1).atTime(16, 30));

        assertEquals("- mañana (viernes 06/11) a las 16:30, cita médica: Control de espalda, Traumatología, "
                + esperado + ", en Clínica Sur", ContextoDelChat.lineaDeCitaProxima(item, HOY));
    }

    @Test
    void siElMedicoEstaVacioSeOmiteSinDejarComasSueltas() {
        for (String vacio : new String[] { null, "", "   " }) {
            ItemAgendaResponse item = cita(vacio, "Clínica Sur", HOY.plusDays(1).atTime(16, 30));

            assertEquals("- mañana (viernes 06/11) a las 16:30, cita médica: Control de espalda, Traumatología, "
                    + "en Clínica Sur", ContextoDelChat.lineaDeCitaProxima(item, HOY));
        }
    }

    @Test
    void siElLugarEstaVacioSeOmiteSinDejarComasSueltas() {
        for (String vacio : new String[] { null, "", "   " }) {
            ItemAgendaResponse item = cita("Dra. Gómez", vacio, HOY.plusDays(1).atTime(16, 30));

            assertEquals("- mañana (viernes 06/11) a las 16:30, cita médica: Control de espalda, Traumatología, "
                    + "con la Dra. Gómez", ContextoDelChat.lineaDeCitaProxima(item, HOY));
        }
    }

    @Test
    void siFaltanElMedicoYElLugarQuedanSoloElTituloYLaEspecialidad() {
        ItemAgendaResponse item = cita(null, "  ", HOY.plusDays(1).atTime(16, 30));

        assertEquals("- mañana (viernes 06/11) a las 16:30, cita médica: Control de espalda, Traumatología",
                ContextoDelChat.lineaDeCitaProxima(item, HOY));
    }

    // ---------- Cita de hoy (en {recordatorios}) ----------

    @Test
    void laCitaDeHoyLlevaMedicoYLugarYElEstadoAlFinal() {
        ItemAgendaResponse item = cita("Control de presión", "Cardiología", "Dra. Gómez", "Hospital Central",
                HOY.atTime(16, 30), EstadoNotificacion.PENDIENTE);

        assertEquals("- 16:30, cita médica: Control de presión, Cardiología, con la Dra. Gómez, "
                + "en Hospital Central (pendiente)", ContextoDelChat.linea(item));
    }

    @Test
    void laCitaDeHoyOmiteMedicoYLugarVaciosYNuncaLlevaLasIndicaciones() {
        ItemAgendaResponse item = cita("Control de presión", "Cardiología", " ", null, HOY.atTime(16, 30),
                EstadoNotificacion.VISTA);

        String linea = ContextoDelChat.linea(item);

        assertEquals("- 16:30, cita médica: Control de presión, Cardiología (ya vista)", linea);
        assertFalse(linea.contains("Llevar los estudios"));
    }
}

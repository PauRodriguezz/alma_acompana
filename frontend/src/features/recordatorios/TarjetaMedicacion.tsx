import { horaDeToma } from "./fechas"
import { BOTON_EDITAR, ETIQUETA_FRECUENCIA } from "./mensajes"
import { TarjetaBase } from "./TarjetaBase"
import type { Medicacion } from "./tipos"

/**
 * Una medicación guardada ("Mis medicaciones"): la tarjeta entera lleva a su edición, donde
 * también se elimina. No tiene estado: eso es de cada aviso (TarjetaAviso).
 */
export function TarjetaMedicacion({ medicacion }: { medicacion: Medicacion }) {
  return (
    <TarjetaBase
      tipo="MEDICACION"
      titulo={medicacion.nombreMedicamento}
      secundario={`${medicacion.dosis} · ${ETIQUETA_FRECUENCIA[medicacion.frecuencia].toLowerCase()} · ${horaDeToma(medicacion.horaToma)}`}
      enlace={`/recordatorios/${medicacion.id}/editar`}
      textoEnlace={BOTON_EDITAR}
    />
  )
}

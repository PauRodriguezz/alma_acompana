import { format } from "date-fns"
import { z } from "zod"

import { ahoraArgentina } from "@/lib/fechas"
import * as M from "./mensajes"

// Esquemas de los dos formularios, con las reglas y los textos de 5.8 del contrato. La API
// vuelve a validar todo: esto es para avisar enseguida, sin esperar la respuesta.

export const FRECUENCIAS = ["DIARIA", "DIA_POR_MEDIO", "SEMANAL"] as const

function texto(obligatorio: string, largo: string, maximo: number) {
  return z.string().trim().min(1, obligatorio).max(maximo, largo)
}

export const esquemaMedicacion = z.object({
  nombreMedicamento: texto(M.NOMBRE_OBLIGATORIO, M.NOMBRE_LARGO, 100),
  dosis: texto(M.DOSIS_OBLIGATORIA, M.DOSIS_LARGA, 100),
  horaToma: z.string().min(1, M.HORA_OBLIGATORIA),
  frecuencia: z.enum(FRECUENCIAS, { error: M.FRECUENCIA_OBLIGATORIA }),
  // Vacío o con letras llega como NaN: se trata como "Indicá cuántos días."
  duracionDias: z
    .number({ error: M.DURACION_OBLIGATORIA })
    .int(M.DURACION_FUERA_DE_RANGO)
    .min(1, M.DURACION_FUERA_DE_RANGO)
    .max(365, M.DURACION_FUERA_DE_RANGO),
})

export const esquemaCita = z
  .object({
    titulo: texto(M.TITULO_OBLIGATORIO, M.TITULO_LARGO, 100),
    especialidad: texto(M.ESPECIALIDAD_OBLIGATORIA, M.ESPECIALIDAD_LARGA, 100),
    medico: texto(M.MEDICO_OBLIGATORIO, M.MEDICO_LARGO, 100),
    lugar: texto(M.LUGAR_OBLIGATORIO, M.LUGAR_LARGO, 150),
    // La fecha y la hora van en dos campos ("yyyy-MM-dd" y "HH:mm") y se juntan al enviar.
    fecha: z.string().min(1, M.FECHA_HORA_OBLIGATORIA),
    hora: z.string().min(1, M.FECHA_HORA_OBLIGATORIA),
    indicacionesPrevias: z.string().trim().max(500, M.INDICACIONES_LARGAS),
  })
  .superRefine((datos, contexto) => {
    // Posterior al momento actual (en hora de Argentina). Las dos son texto con el mismo
    // formato, así que se comparan como texto.
    const ahora = format(ahoraArgentina(), "yyyy-MM-dd'T'HH:mm")
    if (datos.fecha && datos.hora && `${datos.fecha}T${datos.hora}` <= ahora) {
      contexto.addIssue({ code: "custom", path: ["fecha"], message: M.FECHA_HORA_PASADA })
    }
  })

export type DatosFormularioMedicacion = z.infer<typeof esquemaMedicacion>
export type DatosFormularioCita = z.infer<typeof esquemaCita>

import { useState } from "react"
import { HugeiconsIcon } from "@hugeicons/react"
import { MedicineBottle01Icon, Stethoscope02Icon } from "@hugeicons/core-free-icons"

import { RadioGroup } from "@/components/ui/radio-group"
import { OpcionRadio } from "@/components/alma/opcion-radio"

import { EnlaceVolver } from "../components/EnlaceVolver"
import { FormularioCita } from "../components/FormularioCita"
import { FormularioMedicacion } from "../components/FormularioMedicacion"
import { BOTON_NUEVO, OPCION_CITA, OPCION_MEDICACION, TIPO_DE_RECORDATORIO } from "../mensajes"
import type { TipoRecordatorio } from "../tipos"

const PASTILLA =
  "min-h-(--altura-boton) flex-row items-center justify-center gap-2 rounded-full px-3 py-3 sm:px-5 font-heading text-button font-medium"

/** Alta (CU-07 y CU-08): con las dos opciones arriba, "Medicación" y "Cita médica". */
export function NuevoRecordatorioPage() {
  const [tipo, setTipo] = useState<TipoRecordatorio>("MEDICACION")

  return (
    <div className="mx-auto flex w-full max-w-(--narrow-max) flex-col gap-4 lg:mx-0">
      <EnlaceVolver />
      <h1>{BOTON_NUEVO}</h1>

      <RadioGroup
        aria-label={TIPO_DE_RECORDATORIO}
        value={tipo}
        onValueChange={(valor) => setTipo(valor as TipoRecordatorio)}
        className="grid-cols-2 gap-3 sm:flex"
      >
        <OpcionRadio value="MEDICACION" className={PASTILLA}>
          <HugeiconsIcon icon={MedicineBottle01Icon} className="size-6 shrink-0" aria-hidden="true" />
          {OPCION_MEDICACION}
        </OpcionRadio>
        <OpcionRadio value="CITA" className={PASTILLA}>
          <HugeiconsIcon icon={Stethoscope02Icon} className="size-6 shrink-0" aria-hidden="true" />
          {OPCION_CITA}
        </OpcionRadio>
      </RadioGroup>

      {tipo === "MEDICACION" ? <FormularioMedicacion /> : <FormularioCita />}
    </div>
  )
}

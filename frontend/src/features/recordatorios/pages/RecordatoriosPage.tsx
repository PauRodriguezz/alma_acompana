import { HugeiconsIcon } from "@hugeicons/react"
import { Calendar03Icon, ClipboardIcon } from "@hugeicons/core-free-icons"
import { Link } from "react-router-dom"

import { buttonVariants } from "@/components/ui/button-variants"
import { Tabs, TabsList, TabsPanel, TabsTab } from "@/components/ui/tabs"

import { CalendarioRecordatorios } from "../components/CalendarioRecordatorios"
import { ListaRecordatorios } from "../components/ListaRecordatorios"
import { BOTON_NUEVO, PESTANIA_CALENDARIO, PESTANIA_LISTA, TITULO_RECORDATORIOS } from "../mensajes"

/** Recordatorios: pestañas Lista y Calendario (las pestañas no cambian la URL). */
export function RecordatoriosPage() {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <h1>{TITULO_RECORDATORIOS}</h1>
        <Link to="/recordatorios/nuevo" className={buttonVariants({ className: "w-full lg:w-auto" })}>
          {BOTON_NUEVO}
        </Link>
      </div>

      <Tabs defaultValue="lista">
        {/* En celular las pestañas ocupan todo el ancho: con letra "Muy grande" no entrarían de otro modo */}
        <TabsList className="max-lg:-mx-(--page-x)">
          <TabsTab value="lista">
            <HugeiconsIcon icon={ClipboardIcon} className="size-6" aria-hidden="true" />
            {PESTANIA_LISTA}
          </TabsTab>
          <TabsTab value="calendario">
            <HugeiconsIcon icon={Calendar03Icon} className="size-6" aria-hidden="true" />
            {PESTANIA_CALENDARIO}
          </TabsTab>
        </TabsList>
        <TabsPanel value="lista">
          <ListaRecordatorios />
        </TabsPanel>
        <TabsPanel value="calendario">
          <CalendarioRecordatorios />
        </TabsPanel>
      </Tabs>
    </div>
  )
}

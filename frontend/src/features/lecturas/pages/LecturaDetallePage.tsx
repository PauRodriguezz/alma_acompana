import type { ReactNode } from "react"
import { Link, useSearchParams } from "react-router-dom"

import { EnlaceVolver } from "@/components/alma/enlace-volver"
import { TituloConFoco } from "@/components/alma/titulo-con-foco"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { useAviso } from "@/features/avisos/useAviso"
import { RUTA_LECTURAS } from "@/features/entretenimiento/rutas"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"
import { MENSAJE_NO_ENCONTRADO, ACCION_PROBAR_DE_NUEVO, ESTADO_CARGANDO } from "@/lib/mensajes"
import { useIdDeRuta } from "@/lib/useIdDeRuta"

import { BotonGuardar } from "../components/BotonGuardar"
import { EtiquetasLectura } from "../components/EtiquetasLectura"
import { AVISO_GUARDADA, AVISO_QUITADA, TITULO_LEER, VOLVER_A_LEER } from "../mensajes"
import { conPestania, pestaniaDe } from "../pestanias"
import type { LecturaDetalle } from "../tipos"
import { useCambiarGuardada, useLectura } from "../useLecturas"

// Ancho de lectura (PROPUESTO, no está en el Figma): unos 70 caracteres por línea.
const ANCHO_LECTURA = "max-w-[calc(680rem/17)]"

/**
 * Detalle de una lectura (contrato, 10.2; no está en el Figma): "‹ Leer" a la pestaña de
 * origen (?ver=guardadas), categoría y marca "Guardada", título, el botón de guardar, la imagen
 * decorativa si hay y el texto en párrafos. En escritorio es la misma columna, con el ancho
 * de lectura limitado.
 */
export function LecturaDetallePage() {
  const id = useIdDeRuta()
  const [parametros] = useSearchParams()
  const rutaVolver = conPestania(RUTA_LECTURAS, pestaniaDe(parametros))
  const consulta = useLectura(id)

  let contenido: ReactNode
  if (id === undefined || (consulta.isError && esNoEncontrado(consulta.error))) {
    contenido = (
      <>
        <TituloConFoco>{TITULO_LEER}</TituloConFoco>
        <Alert variant="info">{MENSAJE_NO_ENCONTRADO}</Alert>
        <Link to={rutaVolver} className={buttonVariants({ variant: "outline", className: "self-start" })}>
          {VOLVER_A_LEER}
        </Link>
      </>
    )
  } else if (consulta.isError) {
    contenido = (
      <>
        <TituloConFoco>{TITULO_LEER}</TituloConFoco>
        <Alert variant="error">{mensajeDeError(consulta.error)}</Alert>
        <Button variant="outline" className="self-start" onClick={() => consulta.refetch()}>
          {ACCION_PROBAR_DE_NUEVO}
        </Button>
      </>
    )
  } else if (consulta.isPending) {
    contenido = (
      <p role="status" className="text-muted-foreground">
        {ESTADO_CARGANDO}
      </p>
    )
  } else {
    contenido = <Lectura lectura={consulta.data} />
  }

  return (
    <div className={`flex flex-col gap-4 ${ANCHO_LECTURA}`}>
      <EnlaceVolver to={rutaVolver} texto={TITULO_LEER} />
      {contenido}
    </div>
  )
}

function esNoEncontrado(error: unknown) {
  return error instanceof ApiError && error.status === 404
}

function Lectura({ lectura }: { lectura: LecturaDetalle }) {
  const { avisar } = useAviso()
  const cambiar = useCambiarGuardada()

  function alternar() {
    const guardar = !lectura.guardada
    cambiar.mutate(
      { id: lectura.id, guardar },
      { onSuccess: () => avisar(guardar ? AVISO_GUARDADA : AVISO_QUITADA) }
    )
  }

  return (
    <article className="flex flex-col gap-4">
      <EtiquetasLectura categoria={lectura.categoria} guardada={lectura.guardada} />
      <TituloConFoco>{lectura.titulo}</TituloConFoco>

      {/* El error va arriba del botón (10.3); se limpia al volver a intentar */}
      {cambiar.isError ? <Alert variant="error">{mensajeDeError(cambiar.error)}</Alert> : null}
      <BotonGuardar guardada={lectura.guardada} enviando={cambiar.isPending} onClick={alternar} />

      {lectura.imagen ? (
        <img src={lectura.imagen} alt="" className="aspect-video w-full rounded-lg object-cover" />
      ) : null}

      <div className="flex flex-col gap-4 text-body">
        {parrafos(lectura.texto).map((parrafo, i) => (
          <p key={i} className="whitespace-pre-line wrap-break-word">
            {parrafo}
          </p>
        ))}
      </div>
    </article>
  )
}

/** Los párrafos van separados por una línea en blanco; un salto simple se respeta dentro del párrafo. */
function parrafos(texto: string): string[] {
  return texto
    .split(/\n\s*\n/)
    .map((parrafo) => parrafo.trim())
    .filter((parrafo) => parrafo !== "")
}

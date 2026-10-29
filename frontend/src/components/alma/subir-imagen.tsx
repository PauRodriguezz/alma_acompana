import { useId, useRef, useState, type ChangeEvent } from "react"
import { useMutation } from "@tanstack/react-query"
import { HugeiconsIcon } from "@hugeicons/react"
import { ImageUpload01Icon } from "@hugeicons/core-free-icons"

import { MensajeError } from "@/components/alma/mensaje-error"
import { Button } from "@/components/ui/button"
import { ApiError, api } from "@/lib/api"
import {
  CAMPO_OPCIONAL,
  IMAGEN_CAMBIAR,
  IMAGEN_ELEGIR,
  IMAGEN_MUY_PESADA,
  IMAGEN_QUITAR,
  IMAGEN_SUBIENDO,
  IMAGEN_TIPO_INVALIDO,
  MENSAJE_ALGO_SALIO_MAL,
  MENSAJE_SIN_CONEXION,
} from "@/lib/mensajes"

// Reglas de la subida (contrato del panel, sección 3.4). El back las vuelve a controlar
// mirando el contenido real del archivo; acá solo se avisa antes de subir.
const TIPOS_PERMITIDOS = ["image/jpeg", "image/png", "image/webp"]
const TAMANIO_MAXIMO = 2 * 1024 * 1024

function subirImagen(archivo: File): Promise<{ ruta: string }> {
  const datos = new FormData()
  datos.append("archivo", archivo)
  return api<{ ruta: string }>("/admin/imagenes", { method: "POST", cuerpo: datos })
}

type SubirImagenProps = {
  /** Texto visible arriba (por ejemplo "Imagen"); es el nombre del grupo. */
  etiqueta: string
  /** Texto del botón cuando no hay imagen ("Elegir imagen" o "Agregar imagen"). */
  textoBoton?: string
  /** Ruta guardada ("/imagenes/…") o null si no hay imagen. */
  valor: string | null
  alCambiar: (ruta: string | null) => void
  /** Texto alternativo de la miniatura (por ejemplo el título o el texto de la opción). */
  textoAlternativo: string
  /** Error que viene del formulario (por ejemplo del back: "La imagen no es válida…"). */
  error?: string
}

/**
 * Imagen opcional (contrato del panel, sección 3.4): el botón abre el selector de archivos
 * de la computadora, se controla tipo y tamaño, se sube con POST /api/admin/imagenes y
 * devuelve la ruta al formulario. Con imagen muestra la miniatura, "Cambiar imagen" y
 * "Quitar imagen". El botón principal es siempre el mismo elemento, así el foco no se
 * pierde al terminar la subida; al quitar la imagen, el foco vuelve a ese botón.
 * Figma (52:2224): borde punteado color heading, ícono y label semibold.
 */
export function SubirImagen({
  etiqueta,
  textoBoton = IMAGEN_ELEGIR,
  valor,
  alCambiar,
  textoAlternativo,
  error,
}: SubirImagenProps) {
  const id = useId()
  const idEtiqueta = `${id}-etiqueta`
  const idError = `${id}-error`
  const selector = useRef<HTMLInputElement>(null)
  const botonPrincipal = useRef<HTMLButtonElement>(null)
  const [errorLocal, setErrorLocal] = useState<string | null>(null)

  const subida = useMutation({
    mutationFn: subirImagen,
    onSuccess: (respuesta) => alCambiar(respuesta.ruta),
    onError: (e) => {
      if (e instanceof ApiError && e.status === 0) setErrorLocal(MENSAJE_SIN_CONEXION)
      else if (e instanceof ApiError) setErrorLocal(e.detalle ?? MENSAJE_ALGO_SALIO_MAL)
      else setErrorLocal(MENSAJE_ALGO_SALIO_MAL)
    },
  })

  function alElegir(evento: ChangeEvent<HTMLInputElement>) {
    const archivo = evento.target.files?.[0]
    // Se limpia para que se pueda volver a elegir el mismo archivo
    evento.target.value = ""
    if (!archivo) return
    setErrorLocal(null)
    if (!TIPOS_PERMITIDOS.includes(archivo.type)) {
      setErrorLocal(IMAGEN_TIPO_INVALIDO)
      return
    }
    if (archivo.size > TAMANIO_MAXIMO) {
      setErrorLocal(IMAGEN_MUY_PESADA)
      return
    }
    subida.mutate(archivo)
  }

  function quitar() {
    setErrorLocal(null)
    alCambiar(null)
    botonPrincipal.current?.focus()
  }

  const mensaje = errorLocal ?? error
  const subiendo = subida.isPending

  return (
    <div role="group" aria-labelledby={idEtiqueta} className="flex flex-col gap-2">
      <span id={idEtiqueta} className="text-label font-semibold">
        {etiqueta}
        <span className="font-normal text-muted-foreground">{CAMPO_OPCIONAL}</span>
      </span>

      <input
        ref={selector}
        type="file"
        accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
        onChange={alElegir}
      />

      {valor ? (
        <img
          src={valor}
          alt={textoAlternativo}
          className="size-28 rounded-md border-2 border-border bg-secondary object-cover"
        />
      ) : null}

      <div className="flex flex-wrap gap-x-3 gap-y-1">
        <button
          ref={botonPrincipal}
          type="button"
          onClick={() => selector.current?.click()}
          disabled={subiendo}
          aria-describedby={mensaje ? idError : undefined}
          className="inline-flex min-h-(--altura-boton) cursor-pointer items-center justify-center gap-2 rounded-lg border-2 border-dashed border-heading px-4 py-2 text-label font-semibold text-heading transition-colors hover:bg-outline-hover disabled:cursor-not-allowed disabled:border-input disabled:text-disabled-foreground"
        >
          <HugeiconsIcon icon={ImageUpload01Icon} className="size-6 shrink-0" aria-hidden="true" />
          {subiendo ? IMAGEN_SUBIENDO : valor ? IMAGEN_CAMBIAR : textoBoton}
        </button>
        {valor && !subiendo ? (
          <Button variant="outline" onClick={quitar}>
            {IMAGEN_QUITAR}
          </Button>
        ) : null}
      </div>

      {/* Siempre presente: anuncia el resultado de la subida (también los errores). */}
      <div aria-live="polite">
        {mensaje ? <MensajeError id={idError}>{mensaje}</MensajeError> : null}
        {subiendo ? <p className="sr-only">{IMAGEN_SUBIENDO}</p> : null}
      </div>
    </div>
  )
}

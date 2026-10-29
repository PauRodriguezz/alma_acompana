import { useState, type ReactElement } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { HugeiconsIcon } from "@hugeicons/react"
import { Delete02Icon, PencilEdit02Icon } from "@hugeicons/core-free-icons"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"
import { tamanioTextoBoton } from "@/components/alma/estilos"
import { SelectorChips } from "@/components/alma/selector-chips"
import { Alert } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { buttonVariants } from "@/components/ui/button-variants"
import { Card } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCaption,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { AvisoExito } from "@/features/admin/AvisoExito"
import { useAvisoExito } from "@/features/admin/aviso"
import { EncabezadoPagina } from "@/features/admin/EncabezadoPagina"
import { mensajeDeError } from "@/lib/errores"
import { estiloBotonIcono } from "@/features/admin/estilos"
import {
  CATEGORIAS_EJERCICIO,
  TEXTO_CATEGORIA_EJERCICIO,
  TEXTO_FORMATO_TABLA,
  TEXTO_NIVEL,
  type CategoriaEjercicio,
} from "@/features/admin/etiquetas"

import {
  CLAVES_EJERCICIOS,
  eliminarEjercicio,
  listarEjercicios,
  refrescarEjercicios,
} from "./api"
import { EtiquetaCategoriaEjercicio } from "./EtiquetaCategoriaEjercicio"
import type { EjercicioResumen } from "./tipos"

const RUTA_LISTA = "/admin/ejercicios"
const TODAS = "TODAS"
type Filtro = CategoriaEjercicio | typeof TODAS

const OPCIONES_FILTRO: { valor: Filtro; texto: string }[] = [
  { valor: TODAS, texto: "Todas" },
  ...CATEGORIAS_EJERCICIO.map((valor) => ({ valor, texto: TEXTO_CATEGORIA_EJERCICIO[valor] })),
]

function esCategoria(valor: string | null): valor is CategoriaEjercicio {
  return (CATEGORIAS_EJERCICIO as readonly string[]).includes(valor ?? "")
}

/** Confirmación de baja lógica (Figma 52:2333, con el texto ajustado a baja lógica). */
function DialogoEliminar({
  ejercicio,
  alConfirmar,
  disparador,
}: {
  ejercicio: EjercicioResumen
  alConfirmar: (id: number) => void
  disparador: ReactElement
}) {
  return (
    <DialogoConfirmar
      titulo="¿Eliminar este ejercicio?"
      descripcion="Se va a eliminar del catálogo. Esta acción no se puede deshacer."
      textoConfirmar="Sí, eliminar"
      variante="destructive"
      alConfirmar={() => alConfirmar(ejercicio.id)}
      disparador={disparador}
    />
  )
}

/**
 * Lista de ejercicios (Figma 51:1493, CU-10). Filtros por categoría como chips (grupo de
 * radio: se anuncian como "Filtrar por categoría, Refranes, seleccionado, 2 de 5"); el
 * filtro queda en la dirección (?categoria=) y lo aplica el back. Contenido ancho: tabla con la
 * consigna recortada (el lector de pantalla la lee entera). Celular (PROPUESTO): tarjetas.
 */
export function EjerciciosPage() {
  const clienteConsultas = useQueryClient()
  const [parametros, setParametros] = useSearchParams()
  const valorParametro = parametros.get("categoria")
  const categoria = esCategoria(valorParametro) ? valorParametro : null
  const filtro: Filtro = categoria ?? TODAS

  const lista = useQuery({
    queryKey: CLAVES_EJERCICIOS.lista(categoria),
    queryFn: () => listarEjercicios(categoria),
  })
  const [aviso, mostrarAviso] = useAvisoExito()
  const [errorBaja, setErrorBaja] = useState<string | null>(null)

  const baja = useMutation({
    mutationFn: eliminarEjercicio,
    onMutate: () => setErrorBaja(null),
    onSuccess: async () => {
      await refrescarEjercicios(clienteConsultas)
      mostrarAviso("El ejercicio fue eliminado.")
    },
    onError: (e) => setErrorBaja(mensajeDeError(e)),
  })

  const eliminar = (id: number) => baja.mutate(id)

  function cambiarFiltro(nuevo: Filtro) {
    setParametros(nuevo === TODAS ? {} : { categoria: nuevo }, { replace: true })
  }

  const textoVacio =
    categoria === null
      ? "Todavía no hay ejercicios."
      : `No hay ejercicios de ${TEXTO_CATEGORIA_EJERCICIO[categoria]}.`

  return (
    <>
      <EncabezadoPagina
        titulo="Ejercicios"
        migas={[{ texto: "Panel de operador", a: "/admin" }, { texto: "Ejercicios" }]}
        accion={
          <Link to={`${RUTA_LISTA}/nuevo`} className={buttonVariants()}>
            Nuevo ejercicio
          </Link>
        }
      />

      <AvisoExito aviso={aviso} />
      {errorBaja ? <Alert variant="error">{errorBaja}</Alert> : null}

      <SelectorChips
        etiqueta="Filtrar por categoría"
        opciones={OPCIONES_FILTRO}
        valor={filtro}
        alCambiar={cambiarFiltro}
        variante="filtro"
      />

      {/* Anuncia cuántos hay después de cambiar el filtro */}
      <p role="status" className="sr-only">
        {lista.isSuccess
          ? lista.data.length === 1
            ? "Se muestra 1 ejercicio."
            : `Se muestran ${lista.data.length} ejercicios.`
          : ""}
      </p>

      {lista.isPending ? (
        <p role="status">Cargando…</p>
      ) : lista.isError ? (
        <Alert variant="error">
          <div className="flex flex-col items-start gap-3">
            <p>{mensajeDeError(lista.error)}</p>
            <Button
              variant="outline"
              className={tamanioTextoBoton}
              onClick={() => lista.refetch()}
            >
              Probar de nuevo
            </Button>
          </div>
        </Alert>
      ) : lista.data.length === 0 ? (
        <Card>
          <p>{textoVacio}</p>
        </Card>
      ) : (
        <>
          {/* Contenido ancho (desde 42rem, crece con la letra): tabla */}
          <Card className="hidden px-6 py-3 @2xl:flex">
            <Table>
              <TableCaption className="sr-only">Ejercicios del catálogo</TableCaption>
              <TableHeader>
                <TableRow>
                  <TableHead>Categoría</TableHead>
                  {/* La consigna ocupa el resto del ancho y se recorta con "…" */}
                  <TableHead className="w-full">Consigna</TableHead>
                  <TableHead className="whitespace-nowrap">Tipo</TableHead>
                  <TableHead>Dificultad</TableHead>
                  <TableHead>Acciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lista.data.map((e) => (
                  <TableRow key={e.id}>
                    <TableCell>
                      <EtiquetaCategoriaEjercicio categoria={e.categoria} />
                    </TableCell>
                    <TableCell className="max-w-0">
                      <span className="block truncate" title={e.consigna}>
                        {e.consigna}
                      </span>
                    </TableCell>
                    <TableCell className="whitespace-nowrap">
                      {TEXTO_FORMATO_TABLA[e.formato]}
                    </TableCell>
                    <TableCell>{TEXTO_NIVEL[e.nivelDificultad]}</TableCell>
                    <TableCell>
                      <div className="flex gap-4">
                        <Link
                          to={`${RUTA_LISTA}/${e.id}/editar`}
                          aria-label={`Editar ejercicio: ${e.consigna}`}
                          className={`${estiloBotonIcono} text-heading`}
                        >
                          <HugeiconsIcon icon={PencilEdit02Icon} aria-hidden="true" />
                        </Link>
                        <DialogoEliminar
                          ejercicio={e}
                          alConfirmar={eliminar}
                          disparador={
                            <button
                              type="button"
                              aria-label={`Eliminar ejercicio: ${e.consigna}`}
                              className={`${estiloBotonIcono} text-destructive`}
                            >
                              <HugeiconsIcon icon={Delete02Icon} aria-hidden="true" />
                            </button>
                          }
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>

          {/* Contenido angosto (celular o letra grande): tarjetas */}
          <ul className="flex flex-col gap-4 @2xl:hidden">
            {lista.data.map((e) => (
              <li key={e.id}>
                <Card className="items-start gap-3">
                  <EtiquetaCategoriaEjercicio categoria={e.categoria} />
                  <h2 className="max-w-full font-sans text-body font-bold wrap-break-word text-foreground">
                    {e.consigna}
                  </h2>
                  <dl className="flex flex-wrap gap-x-6 gap-y-1">
                    <div className="flex gap-1">
                      <dt className="font-semibold">Tipo:</dt>
                      <dd>{TEXTO_FORMATO_TABLA[e.formato]}</dd>
                    </div>
                    <div className="flex gap-1">
                      <dt className="font-semibold">Dificultad:</dt>
                      <dd>{TEXTO_NIVEL[e.nivelDificultad]}</dd>
                    </div>
                  </dl>
                  <div className="flex flex-wrap gap-3">
                    <Link
                      to={`${RUTA_LISTA}/${e.id}/editar`}
                      aria-label={`Editar ejercicio: ${e.consigna}`}
                      className={buttonVariants({ variant: "outline" })}
                    >
                      <HugeiconsIcon icon={PencilEdit02Icon} className="size-6" aria-hidden="true" />
                      Editar
                    </Link>
                    <DialogoEliminar
                      ejercicio={e}
                      alConfirmar={eliminar}
                      disparador={
                        <Button
                          variant="outline"
                          className={tamanioTextoBoton}
                          aria-label={`Eliminar ejercicio: ${e.consigna}`}
                        >
                          <HugeiconsIcon
                            icon={Delete02Icon}
                            className="size-6 text-destructive"
                            aria-hidden="true"
                          />
                          Eliminar
                        </Button>
                      }
                    />
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}

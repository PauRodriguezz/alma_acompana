import { useState, type ReactElement } from "react"
import { Link } from "react-router-dom"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { format, parseISO } from "date-fns"
import { HugeiconsIcon } from "@hugeicons/react"
import { Delete02Icon, PencilEdit02Icon } from "@hugeicons/core-free-icons"

import { DialogoConfirmar } from "@/components/alma/dialogo-confirmar"
import { Alert } from "@/components/ui/alert"
import { tamanioTextoBoton } from "@/components/alma/estilos"
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
  CLAVES_MICROCAPSULAS,
  darDeBajaMicrocapsula,
  listarMicrocapsulas,
  refrescarMicrocapsulas,
} from "./api"
import { EtiquetaCategoriaMicrocapsula } from "./EtiquetaCategoriaMicrocapsula"
import type { MicrocapsulaResumen } from "./tipos"

const RUTA_LISTA = "/admin/microcapsulas"

function fecha(iso: string) {
  return format(parseISO(iso), "dd/MM/yyyy")
}

type DialogoBajaProps = {
  microcapsula: MicrocapsulaResumen
  alConfirmar: (id: number) => void
  /** Botón que lo abre: ícono en la tabla, con texto en las tarjetas de celular. */
  disparador: ReactElement
}

/** Confirmación de baja (Figma 52:2362, CU-11). */
function DialogoBaja({ microcapsula, alConfirmar, disparador }: DialogoBajaProps) {
  return (
    <DialogoConfirmar
      titulo="¿Dar de baja esta microcápsula?"
      descripcion="Los usuarios que la guardaron como favorita seguirán viéndola."
      textoConfirmar="Sí, dar de baja"
      variante="destructive"
      alConfirmar={() => alConfirmar(microcapsula.id)}
      disparador={disparador}
    />
  )
}

/**
 * Lista de microcápsulas (Figma 51:1694, CU-11). Contenido ancho: tabla. Angosto (PROPUESTO):
 * una tarjeta por microcápsula, con "Editar" y "Dar de baja" con texto.
 */
export function MicrocapsulasPage() {
  const clienteConsultas = useQueryClient()
  const lista = useQuery({ queryKey: CLAVES_MICROCAPSULAS.lista, queryFn: listarMicrocapsulas })
  const [aviso, mostrarAviso] = useAvisoExito()
  const [errorBaja, setErrorBaja] = useState<string | null>(null)

  const baja = useMutation({
    mutationFn: darDeBajaMicrocapsula,
    onMutate: () => setErrorBaja(null),
    onSuccess: async () => {
      await refrescarMicrocapsulas(clienteConsultas)
      mostrarAviso("La microcápsula fue dada de baja.")
    },
    onError: (e) => setErrorBaja(mensajeDeError(e)),
  })

  const darDeBaja = (id: number) => baja.mutate(id)

  return (
    <>
      <EncabezadoPagina
        titulo="Microcápsulas"
        migas={[{ texto: "Panel de operador", a: "/admin" }, { texto: "Microcápsulas" }]}
        accion={
          <Link to={`${RUTA_LISTA}/nueva`} className={buttonVariants()}>
            Nueva microcápsula
          </Link>
        }
      />

      <AvisoExito aviso={aviso} />
      {errorBaja ? <Alert variant="error">{errorBaja}</Alert> : null}

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
          <p>Todavía no hay microcápsulas.</p>
        </Card>
      ) : (
        <>
          {/* Contenido ancho (desde 42rem, crece con la letra): tabla */}
          <Card className="hidden px-6 py-3 @2xl:flex">
            <Table>
              <TableCaption className="sr-only">Microcápsulas publicadas</TableCaption>
              <TableHeader>
                <TableRow>
                  <TableHead>Título</TableHead>
                  <TableHead>Categoría</TableHead>
                  <TableHead>Publicada</TableHead>
                  <TableHead>Acciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lista.data.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="wrap-break-word">{m.titulo}</TableCell>
                    <TableCell>
                      <EtiquetaCategoriaMicrocapsula categoria={m.categoria} />
                    </TableCell>
                    <TableCell className="whitespace-nowrap">{fecha(m.fechaPublicacion)}</TableCell>
                    <TableCell>
                      <div className="flex gap-4">
                        <Link
                          to={`${RUTA_LISTA}/${m.id}/editar`}
                          aria-label={`Editar microcápsula: ${m.titulo}`}
                          className={`${estiloBotonIcono} text-heading`}
                        >
                          <HugeiconsIcon icon={PencilEdit02Icon} aria-hidden="true" />
                        </Link>
                        <DialogoBaja
                          microcapsula={m}
                          alConfirmar={darDeBaja}
                          disparador={
                            <button
                              type="button"
                              aria-label={`Dar de baja microcápsula: ${m.titulo}`}
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
            {lista.data.map((m) => (
              <li key={m.id}>
                <Card className="items-start gap-3">
                  <h2 className="max-w-full font-sans text-body font-bold wrap-break-word text-foreground">
                    {m.titulo}
                  </h2>
                  <EtiquetaCategoriaMicrocapsula categoria={m.categoria} />
                  <p>Publicada: {fecha(m.fechaPublicacion)}</p>
                  <div className="flex flex-wrap gap-3">
                    <Link
                      to={`${RUTA_LISTA}/${m.id}/editar`}
                      aria-label={`Editar microcápsula: ${m.titulo}`}
                      className={buttonVariants({ variant: "outline" })}
                    >
                      <HugeiconsIcon icon={PencilEdit02Icon} className="size-6" aria-hidden="true" />
                      Editar
                    </Link>
                    <DialogoBaja
                      microcapsula={m}
                      alConfirmar={darDeBaja}
                      disparador={
                        <Button
                          variant="outline"
                          className={tamanioTextoBoton}
                          aria-label={`Dar de baja microcápsula: ${m.titulo}`}
                        >
                          <HugeiconsIcon
                            icon={Delete02Icon}
                            className="size-6 text-destructive"
                            aria-hidden="true"
                          />
                          Dar de baja
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

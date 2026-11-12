import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import { dibujarMapa } from "./dibujarMapa"
import { LADO_DE_LA_IMAGEN } from "./diseno"
import { FUENTES_DE_LA_IMAGEN, fuenteCss } from "./tipografias"

export const NOMBRE_DEL_ARCHIVO = "mapa-de-vida.png"

/** Texto con letras de todas las tipografías de la imagen, para que el navegador cargue el subconjunto correcto. */
const MUESTRA = "La vida de Aáñ 123"

/**
 * Antes de dibujar, las fuentes tienen que estar cargadas: si no, el canvas usa otra letra sin avisar. Si una no
 * se puede cargar (o no está declarada), se lanza un error en lugar de crear una imagen con letra equivocada.
 */
async function cargarFuentes(): Promise<void> {
  await Promise.all(
    FUENTES_DE_LA_IMAGEN.map(async (fuente) => {
      const caras = await document.fonts.load(fuenteCss(fuente, 20), MUESTRA)
      if (caras.length === 0 || caras.some((cara) => cara.status !== "loaded")) {
        throw new Error(`No se pudo cargar la fuente ${fuente.familia}`)
      }
    })
  )
}

function crearPng(grupos: EtapaConRecuerdos[], nombre: string | null | undefined): Promise<Blob> {
  const canvas = document.createElement("canvas")
  canvas.width = LADO_DE_LA_IMAGEN
  canvas.height = LADO_DE_LA_IMAGEN
  const ctx = canvas.getContext("2d")
  if (!ctx) return Promise.reject(new Error("El navegador no puede dibujar la imagen"))
  dibujarMapa(ctx, grupos, nombre)
  return new Promise((resolver, rechazar) => {
    canvas.toBlob((blob) => (blob ? resolver(blob) : rechazar(new Error("No se pudo crear el PNG"))), "image/png")
  })
}

function bajar(blob: Blob) {
  const url = URL.createObjectURL(blob)
  const enlace = document.createElement("a")
  enlace.href = url
  enlace.download = NOMBRE_DEL_ARCHIVO
  document.body.append(enlace)
  enlace.click()
  enlace.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}

/**
 * Crea la imagen cuadrada del mapa de vida (contrato del mapa de vida, 2.5) y la baja como `mapa-de-vida.png`, en
 * todos los dispositivos. Todo se hace en el dispositivo: la imagen no pasa por el back ni se manda a ningún
 * servicio. Si algo falla al crearla, lanza el error.
 */
export async function descargarMapa(grupos: EtapaConRecuerdos[], nombre: string | null | undefined): Promise<void> {
  await cargarFuentes()
  bajar(await crearPng(grupos, nombre))
}

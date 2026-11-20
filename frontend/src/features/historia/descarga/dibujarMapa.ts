import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import type { IdEtapa } from "../etapas"
import { ETAPA_SIN_RECUERDOS, PIE_DE_LA_IMAGEN } from "../mensajes"
import {
  ALTO_DE_LA_IMAGEN,
  ANCHO_DE_LA_IMAGEN,
  ANCHO_MAXIMO_DEL_TITULO,
  CINTA_TAMANIO,
  CIRCULO_RADIO,
  PUNTO_RADIO,
  TAMANIO_DEL_ANIO,
  armarDiseno,
  partesDelTitulo,
  type BanderinDelMapa,
  type DisenoDelMapa,
  type ParadaDelMapa,
  type TarjetaVacia,
} from "./diseno"
import {
  FUENTE_DE_TEXTO,
  FUENTE_DE_TEXTO_FUERTE,
  FUENTE_DE_TITULOS,
  FUENTE_DEL_NOMBRE,
  fuenteCss,
  type Fuente,
} from "./tipografias"

/**
 * Colores de la imagen (contrato del mapa de vida, Anexo A). Salen de los tokens de `index.css`, que el lienzo lee
 * al dibujar con `getComputedStyle` (el canvas no usa clases, pero sí puede leer variables CSS). Esos tokens no se
 * redefinen en alto contraste, así que la imagen no cambia con las preferencias de accesibilidad.
 */
type Paleta = {
  crema: string
  rosa: string
  linea: string
  titulo: string
  violeta: string
  texto: string
  textoDeApoyo: string
  mastil: string
  corazon: string
  blanco: string
  arbolTronco: string
  arbolCopa: string
  arbolBrillo: string
  /** El color de una etapa: acepta `var(--etapa-…)` (así vienen en `Etapa`) o el nombre de la variable. */
  de: (valor: string) => string
}

function leerPaleta(): Paleta {
  const estilos = getComputedStyle(document.documentElement)
  const leer = (variable: string) => {
    const color = estilos.getPropertyValue(variable).trim()
    if (!color) throw new Error(`Falta el token ${variable} en index.css`)
    return color
  }
  return {
    crema: leer("--mapa-fondo"),
    rosa: leer("--mapa-camino"),
    linea: leer("--mapa-linea"),
    titulo: leer("--mapa-titulo"),
    violeta: leer("--mapa-violeta"),
    texto: leer("--mapa-texto"),
    textoDeApoyo: leer("--mapa-apoyo"),
    mastil: leer("--mapa-mastil"),
    corazon: leer("--mapa-corazon"),
    blanco: leer("--mapa-blanco"),
    arbolTronco: leer("--arbol-tronco"),
    arbolCopa: leer("--arbol-copa"),
    arbolBrillo: leer("--arbol-brillo"),
    de: (valor) => leer(valor.match(/^var\((--[\w-]+)\)$/)?.[1] ?? valor),
  }
}

const TAMANIO_DEL_PREFIJO = 78
const TAMANIO_DEL_NOMBRE = 104
const SEPARACION_DEL_NOMBRE = 26
const LINEA_BASE_DEL_TITULO = 128

/** Dibuja la imagen del mapa de vida (contrato del mapa de vida, 2.5) en un canvas de 1920 × 1080. */
export function dibujarMapa(
  ctx: CanvasRenderingContext2D,
  grupos: EtapaConRecuerdos[],
  nombre: string | null | undefined
): DisenoDelMapa {
  ctx.save()
  ctx.clearRect(0, 0, ANCHO_DE_LA_IMAGEN, ALTO_DE_LA_IMAGEN)

  // Todo se mide con la tipografía real antes de dibujar: el título de cada tarjeta se parte en líneas, no se corta.
  const diseno = armarDiseno(grupos, (texto, fuente, tamanio) => {
    ctx.font = fuenteCss(fuente, tamanio)
    return ctx.measureText(texto).width
  })

  const p = leerPaleta()
  dibujarFondo(ctx, p)
  dibujarTitulo(ctx, p, nombre)
  dibujarCamino(ctx, p, diseno.camino)
  diseno.arboles.forEach(([x, y]) => dibujarArbolito(ctx, p, x, y))
  diseno.banderines.forEach((banderin) => dibujarBanderin(ctx, p, banderin))
  diseno.paradas.forEach((parada) => dibujarParada(ctx, p, parada))
  diseno.vacias.forEach((vacia) => dibujarTarjetaVacia(ctx, p, vacia))
  dibujarPie(ctx, p)
  ctx.restore()
  return diseno
}

function usar(ctx: CanvasRenderingContext2D, f: Fuente, tamanio: number) {
  ctx.font = fuenteCss(f, tamanio)
}

function dibujarFondo(ctx: CanvasRenderingContext2D, p: Paleta) {
  ctx.fillStyle = p.crema
  ctx.fillRect(0, 0, ANCHO_DE_LA_IMAGEN, ALTO_DE_LA_IMAGEN)
  ctx.strokeStyle = p.rosa
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.roundRect(24, 24, ANCHO_DE_LA_IMAGEN - 48, ALTO_DE_LA_IMAGEN - 48, 60)
  ctx.stroke()
}

/** "La vida de" en Poppins y el nombre en cursiva, centrados juntos; sin nombre, "Mi vida" sin cursiva. */
function dibujarTitulo(ctx: CanvasRenderingContext2D, p: Paleta, nombre: string | null | undefined) {
  const partes = partesDelTitulo(nombre)
  ctx.textAlign = "left"
  ctx.textBaseline = "alphabetic"

  if (partes.nombre === null) {
    usar(ctx, FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO)
    ctx.fillStyle = p.titulo
    ctx.textAlign = "center"
    ctx.fillText(partes.prefijo, ANCHO_DE_LA_IMAGEN / 2, LINEA_BASE_DEL_TITULO)
    return
  }

  // Si el nombre es tan largo que el título no entra, se achican las dos partes en la misma proporción.
  const medida = (escala: number) => {
    usar(ctx, FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO * escala)
    const prefijo = ctx.measureText(partes.prefijo).width
    usar(ctx, FUENTE_DEL_NOMBRE, TAMANIO_DEL_NOMBRE * escala)
    const propio = ctx.measureText(partes.nombre as string).width
    return { prefijo, total: prefijo + SEPARACION_DEL_NOMBRE * escala + propio }
  }
  const natural = medida(1)
  const escala = natural.total > ANCHO_MAXIMO_DEL_TITULO ? ANCHO_MAXIMO_DEL_TITULO / natural.total : 1
  const { prefijo, total } = medida(escala)
  const inicio = (ANCHO_DE_LA_IMAGEN - total) / 2

  usar(ctx, FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO * escala)
  ctx.fillStyle = p.titulo
  ctx.fillText(partes.prefijo, inicio, LINEA_BASE_DEL_TITULO)
  usar(ctx, FUENTE_DEL_NOMBRE, TAMANIO_DEL_NOMBRE * escala)
  ctx.fillStyle = p.violeta
  ctx.fillText(partes.nombre, inicio + prefijo + SEPARACION_DEL_NOMBRE * escala, LINEA_BASE_DEL_TITULO)
}

function dibujarCamino(ctx: CanvasRenderingContext2D, p: Paleta, camino: string) {
  const trazado = new Path2D(camino)
  ctx.lineCap = "round"
  ctx.lineJoin = "round"
  ctx.strokeStyle = p.rosa
  ctx.lineWidth = 44
  ctx.stroke(trazado)
  ctx.strokeStyle = p.linea
  ctx.lineWidth = 3
  ctx.setLineDash([14, 14])
  ctx.stroke(trazado)
  ctx.setLineDash([])
}

function dibujarArbolito(ctx: CanvasRenderingContext2D, p: Paleta, x: number, y: number) {
  ctx.fillStyle = p.arbolTronco
  ctx.fillRect(x - 3, y + 14, 6, 22)
  ctx.fillStyle = p.arbolCopa
  ctx.beginPath()
  ctx.arc(x, y, 22, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = p.arbolBrillo
  ctx.beginPath()
  ctx.arc(x - 6, y - 6, 10, 0, Math.PI * 2)
  ctx.fill()
}

/** Poste con su bolita, cinta con el nombre de la etapa, ícono y círculo numerado sobre el camino. */
function dibujarBanderin(ctx: CanvasRenderingContext2D, p: Paleta, b: BanderinDelMapa) {
  const { etapa, poloX, poloArriba, caminoY, cinta, icono } = b
  ctx.strokeStyle = p.mastil
  ctx.lineWidth = 4
  ctx.lineCap = "round"
  ctx.beginPath()
  ctx.moveTo(poloX, poloArriba)
  ctx.lineTo(poloX, caminoY - CIRCULO_RADIO)
  ctx.stroke()
  ctx.fillStyle = p.mastil
  ctx.beginPath()
  ctx.arc(poloX, poloArriba, 7, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = p.de(etapa.fuerte)
  ctx.beginPath()
  ctx.roundRect(cinta.x, cinta.y, cinta.ancho, cinta.alto, 5)
  ctx.fill()
  usar(ctx, FUENTE_DE_TITULOS, CINTA_TAMANIO)
  ctx.fillStyle = p.blanco
  ctx.textAlign = "center"
  ctx.fillText(etapa.nombre, cinta.x + cinta.ancho / 2, cinta.y + cinta.alto / 2 + 9)

  ctx.fillStyle = p.de(etapa.claro)
  ctx.beginPath()
  ctx.arc(icono.x, icono.y, icono.radio, 0, Math.PI * 2)
  ctx.fill()
  ctx.save()
  ctx.translate(icono.x, icono.y)
  ctx.scale(1.05, 1.05)
  dibujarIlustracion(ctx, etapa.id, p.de(etapa.fuerte), p.de(etapa.claro))
  ctx.restore()

  ctx.fillStyle = p.blanco
  ctx.strokeStyle = p.de(etapa.fuerte)
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.arc(poloX, caminoY, CIRCULO_RADIO - 3, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  usar(ctx, FUENTE_DE_TITULOS, 26)
  ctx.fillStyle = p.de(etapa.fuerte)
  ctx.textAlign = "center"
  ctx.fillText(String(b.numero), poloX, caminoY + 9)
}

/** Un recuerdo: punto del color de la etapa sobre el camino, tarjeta blanca con el año y el título completo. */
function dibujarParada(ctx: CanvasRenderingContext2D, p: Paleta, parada: ParadaDelMapa) {
  const { etapa, punto, tarjeta } = parada
  ctx.strokeStyle = p.de(etapa.claro)
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.moveTo(punto.x, punto.y + PUNTO_RADIO)
  ctx.lineTo(punto.x, tarjeta.y)
  ctx.stroke()

  ctx.fillStyle = p.de(etapa.fuerte)
  ctx.strokeStyle = p.crema
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.arc(punto.x, punto.y, PUNTO_RADIO - 2, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()

  ctx.fillStyle = p.blanco
  ctx.strokeStyle = p.de(etapa.claro)
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.roundRect(tarjeta.x, tarjeta.y, tarjeta.ancho, tarjeta.alto, 16)
  ctx.fill()
  ctx.stroke()

  ctx.textAlign = "left"
  const izquierda = tarjeta.x + 20
  if (tarjeta.anio) {
    usar(ctx, FUENTE_DE_TEXTO_FUERTE, TAMANIO_DEL_ANIO)
    ctx.fillStyle = p.de(etapa.fuerte)
    ctx.fillText(tarjeta.anio, izquierda, tarjeta.lineaBaseDelAnio)
  }
  usar(ctx, FUENTE_DE_TEXTO, tarjeta.tamanio)
  ctx.fillStyle = p.texto
  tarjeta.lineas.forEach((linea, i) => {
    ctx.fillText(linea, izquierda, tarjeta.lineaBaseDelTitulo + i * tarjeta.interlineado)
  })
}

/** Etapa sin recuerdos: tarjeta punteada con "Todavía sin recuerdos". */
function dibujarTarjetaVacia(ctx: CanvasRenderingContext2D, p: Paleta, vacia: TarjetaVacia) {
  ctx.fillStyle = p.crema
  ctx.strokeStyle = p.de(vacia.etapa.claro)
  ctx.lineWidth = 3
  ctx.setLineDash([9, 7])
  ctx.beginPath()
  ctx.roundRect(vacia.x, vacia.y, vacia.ancho, vacia.alto, 16)
  ctx.fill()
  ctx.stroke()
  ctx.setLineDash([])
  usar(ctx, FUENTE_DE_TEXTO, 26)
  ctx.fillStyle = p.textoDeApoyo
  ctx.textAlign = "center"
  ctx.fillText(ETAPA_SIN_RECUERDOS, vacia.x + vacia.ancho / 2, vacia.y + vacia.alto / 2 + 9)
}

/** Las ilustraciones de `IlustracionDeEtapa.tsx`, centradas en (0, 0), para el canvas. */
function dibujarIlustracion(ctx: CanvasRenderingContext2D, id: IdEtapa, fuerte: string, claro: string) {
  ctx.fillStyle = fuerte
  ctx.strokeStyle = fuerte
  ctx.lineCap = "round"
  switch (id) {
    case "INFANCIA":
      ctx.fill(new Path2D("M0 -14 L10 -3 L0 8 L-10 -3 Z"))
      ctx.strokeStyle = claro
      ctx.lineWidth = 1.2
      ctx.stroke(new Path2D("M0 -14 V8 M-10 -3 H10"))
      ctx.strokeStyle = fuerte
      ctx.lineWidth = 2
      ctx.stroke(new Path2D("M0 8 C-3 14 6 14 3 20"))
      break
    case "ADOLESCENCIA":
      ctx.fill(new Path2D("M-14 -8 L0 -4 V11 L-14 7 Z"))
      ctx.globalAlpha = 0.7
      ctx.fill(new Path2D("M14 -8 L0 -4 V11 L14 7 Z"))
      ctx.globalAlpha = 1
      break
    case "JUVENTUD":
      ctx.fill(new Path2D("M0 12 C-18 -2 -8 -16 0 -6 C8 -16 18 -2 0 12 Z"))
      break
    case "ADULTEZ":
      ctx.fill(new Path2D("M-14 -1 L0 -14 L14 -1 Z"))
      ctx.globalAlpha = 0.8
      ctx.fillRect(-10, -1, 20, 14)
      ctx.globalAlpha = 1
      ctx.fillStyle = claro
      ctx.fillRect(-3, 4, 6, 9)
      break
    case "VEJEZ": {
      ctx.beginPath()
      ctx.arc(0, 0, 7, 0, Math.PI * 2)
      ctx.fill()
      ctx.lineWidth = 2.2
      for (let i = 0; i < 8; i++) {
        const a = (i * Math.PI) / 4
        ctx.beginPath()
        ctx.moveTo(11 * Math.cos(a), 11 * Math.sin(a))
        ctx.lineTo(15 * Math.cos(a), 15 * Math.sin(a))
        ctx.stroke()
      }
      break
    }
    case "OTROS":
      ctx.fill(new Path2D("M0 -14 L4 -4 L14 0 L4 4 L0 14 L-4 4 L-14 0 L-4 -4 Z"))
      break
  }
}

/** "Alma Acompaña" con un corazón, abajo a la derecha. */
function dibujarPie(ctx: CanvasRenderingContext2D, p: Paleta) {
  usar(ctx, FUENTE_DE_TITULOS, 30)
  ctx.fillStyle = p.titulo
  ctx.textAlign = "right"
  ctx.fillText(PIE_DE_LA_IMAGEN, 1802, 1030)

  ctx.save()
  ctx.translate(1832, 1020)
  ctx.scale(1.3, 1.3)
  ctx.translate(-988, -1024)
  ctx.fillStyle = p.corazon
  ctx.fill(new Path2D("M988 1034 C972 1022 978 1010 988 1018 C998 1010 1004 1022 988 1034 Z"))
  ctx.restore()
}

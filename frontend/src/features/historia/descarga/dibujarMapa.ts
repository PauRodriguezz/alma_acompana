import type { EtapaConRecuerdos } from "../agruparPorEtapa"
import type { IdEtapa } from "../etapas"
import { ETAPA_SIN_RECUERDOS, PIE_DE_LA_IMAGEN } from "../mensajes"
import {
  ANCHO_MAXIMO_DEL_TITULO,
  ARBOLITOS,
  LADO_DE_LA_IMAGEN,
  PARADA,
  armarDiseno,
  partesDelTitulo,
  type ParadaDelMapa,
} from "./diseno"
import {
  FUENTE_DE_TEXTO,
  FUENTE_DE_TEXTO_FUERTE,
  FUENTE_DE_TITULOS,
  FUENTE_DEL_NOMBRE,
  fuenteCss,
} from "./tipografias"

// Colores de la imagen (contrato del mapa de vida, Anexo A). Son fijos a propósito: la imagen no depende de las
// preferencias de accesibilidad (tamaño de letra ni contraste) y no lee los tokens del tema.
const CREMA = "#FFFBF4"
const ROSA = "#F4C0D1"
const TITULO = "#0F4C6B"
const VIOLETA = "#8E1B9B"
const TEXTO = "#2F3E4A"
const TEXTO_DE_APOYO = "#5B6B78"
const MASTIL = "#3F4F5C"
const CORAZON = "#D4537E"

const TAMANIO_DEL_PREFIJO = 58
const TAMANIO_DEL_NOMBRE = 78
const SEPARACION_DEL_NOMBRE = 20
const LINEA_BASE_DEL_TITULO = 140

/** Dibuja la imagen cuadrada del mapa de vida (contrato del mapa de vida, 2.5) en un canvas de 1080 × 1080. */
export function dibujarMapa(
  ctx: CanvasRenderingContext2D,
  grupos: EtapaConRecuerdos[],
  nombre: string | null | undefined
): void {
  ctx.save()
  ctx.clearRect(0, 0, LADO_DE_LA_IMAGEN, LADO_DE_LA_IMAGEN)

  // Los títulos de las tarjetas se miden con la tipografía real para cortarlos con "…" si no entran.
  ctx.font = fuenteCss(FUENTE_DE_TEXTO, 22)
  const diseno = armarDiseno(grupos, (texto) => ctx.measureText(texto).width)

  dibujarFondo(ctx)
  dibujarTitulo(ctx, nombre)
  dibujarCamino(ctx, diseno.camino)
  ARBOLITOS.forEach(([x, y]) => dibujarArbolito(ctx, x, y))
  diseno.paradas.forEach((parada) => dibujarParada(ctx, parada))
  dibujarPie(ctx)
  ctx.restore()
}

function dibujarFondo(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = CREMA
  ctx.fillRect(0, 0, LADO_DE_LA_IMAGEN, LADO_DE_LA_IMAGEN)
  ctx.strokeStyle = ROSA
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.roundRect(22, 22, 1036, 1036, 36)
  ctx.stroke()
}

/** "La vida de" en Poppins y el nombre en cursiva, centrados juntos; sin nombre, "Mi vida" sin cursiva. */
function dibujarTitulo(ctx: CanvasRenderingContext2D, nombre: string | null | undefined) {
  const partes = partesDelTitulo(nombre)
  ctx.textAlign = "left"
  ctx.textBaseline = "alphabetic"

  if (partes.nombre === null) {
    ctx.font = fuenteCss(FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO)
    ctx.fillStyle = TITULO
    ctx.textAlign = "center"
    ctx.fillText(partes.prefijo, LADO_DE_LA_IMAGEN / 2, LINEA_BASE_DEL_TITULO)
    return
  }

  // Si el nombre es tan largo que el título no entra, se achican las dos partes en la misma proporción.
  const medida = (escala: number) => {
    ctx.font = fuenteCss(FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO * escala)
    const prefijo = ctx.measureText(partes.prefijo).width
    ctx.font = fuenteCss(FUENTE_DEL_NOMBRE, TAMANIO_DEL_NOMBRE * escala)
    const propio = ctx.measureText(partes.nombre as string).width
    return { prefijo, propio, total: prefijo + SEPARACION_DEL_NOMBRE * escala + propio }
  }
  let escala = 1
  const natural = medida(1)
  if (natural.total > ANCHO_MAXIMO_DEL_TITULO) escala = ANCHO_MAXIMO_DEL_TITULO / natural.total
  const { prefijo, total } = medida(escala)
  const inicio = (LADO_DE_LA_IMAGEN - total) / 2

  ctx.font = fuenteCss(FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO * escala)
  ctx.fillStyle = TITULO
  ctx.fillText(partes.prefijo, inicio, LINEA_BASE_DEL_TITULO)
  ctx.font = fuenteCss(FUENTE_DEL_NOMBRE, TAMANIO_DEL_NOMBRE * escala)
  ctx.fillStyle = VIOLETA
  ctx.fillText(partes.nombre, inicio + prefijo + SEPARACION_DEL_NOMBRE * escala, LINEA_BASE_DEL_TITULO)
}

function dibujarCamino(ctx: CanvasRenderingContext2D, camino: string) {
  const trazado = new Path2D(camino)
  ctx.lineCap = "round"
  ctx.lineJoin = "round"
  ctx.strokeStyle = ROSA
  ctx.lineWidth = 44
  ctx.stroke(trazado)
  ctx.strokeStyle = CREMA
  ctx.lineWidth = 3
  ctx.setLineDash([6, 14])
  ctx.stroke(trazado)
  ctx.setLineDash([])
}

function dibujarArbolito(ctx: CanvasRenderingContext2D, x: number, y: number) {
  ctx.save()
  ctx.translate(x, y)
  ctx.scale(1.8, 1.8)
  ctx.globalAlpha = 0.7
  ctx.fillStyle = "#854F0B"
  ctx.fillRect(-2, 8, 4, 9)
  ctx.globalAlpha = 1
  ctx.fillStyle = "#97C459"
  ctx.beginPath()
  ctx.arc(0, 0, 12, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = "#C0DD97"
  ctx.beginPath()
  ctx.arc(-3, -3, 6, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()
}

/** Punto numerado, mástil, banderín con el nombre de la etapa, ilustración y tarjeta. */
function dibujarParada(ctx: CanvasRenderingContext2D, parada: ParadaDelMapa) {
  const { etapa, x, y } = parada
  const mastilX = x + PARADA.mastilDx

  // Mástil con su bolita
  ctx.strokeStyle = MASTIL
  ctx.lineWidth = 4
  ctx.lineCap = "round"
  ctx.beginPath()
  ctx.moveTo(mastilX, y + PARADA.mastilArriba)
  ctx.lineTo(mastilX, y + PARADA.mastilAbajo)
  ctx.stroke()
  ctx.fillStyle = MASTIL
  ctx.beginPath()
  ctx.arc(mastilX, y + PARADA.mastilArriba, 5, 0, Math.PI * 2)
  ctx.fill()

  // Banderín con muesca en el extremo libre
  const derecha = mastilX + PARADA.banderinAncho
  const medio = y + (PARADA.banderinArriba + PARADA.banderinAbajo) / 2
  ctx.fillStyle = etapa.fuerte
  ctx.beginPath()
  ctx.moveTo(mastilX, y + PARADA.banderinArriba)
  ctx.lineTo(derecha, y + PARADA.banderinArriba)
  ctx.lineTo(derecha - PARADA.muesca, medio)
  ctx.lineTo(derecha, y + PARADA.banderinAbajo)
  ctx.lineTo(mastilX, y + PARADA.banderinAbajo)
  ctx.closePath()
  ctx.fill()
  ctx.font = fuenteCss(FUENTE_DE_TITULOS, 21)
  ctx.fillStyle = "#fff"
  ctx.textAlign = "center"
  ctx.fillText(etapa.nombre, mastilX + 98, medio + 7)

  // Ilustración en su círculo
  const cx = mastilX + PARADA.ilustracionDx
  const cy = y + PARADA.ilustracionDy
  ctx.fillStyle = etapa.claro
  ctx.beginPath()
  ctx.arc(cx, cy, 30, 0, Math.PI * 2)
  ctx.fill()
  ctx.save()
  ctx.translate(cx, cy)
  ctx.scale(1.1, 1.1)
  dibujarIlustracion(ctx, etapa.id, etapa.fuerte, etapa.claro)
  ctx.restore()

  // Tarjeta con los títulos
  const tarjetaY = y + PARADA.tarjetaArriba
  ctx.fillStyle = "#fff"
  ctx.strokeStyle = etapa.claro
  ctx.lineWidth = 3
  ctx.setLineDash(parada.vacia ? [6, 5] : [])
  ctx.beginPath()
  ctx.roundRect(mastilX, tarjetaY, PARADA.tarjetaAncho, parada.altoTarjeta, 12)
  ctx.fill()
  ctx.stroke()
  ctx.setLineDash([])

  ctx.textAlign = "left"
  const textoX = mastilX + PARADA.tarjetaMargen
  if (parada.vacia) {
    ctx.font = fuenteCss(FUENTE_DE_TEXTO, 22)
    ctx.fillStyle = TEXTO_DE_APOYO
    ctx.fillText(ETAPA_SIN_RECUERDOS, textoX, tarjetaY + parada.lineasBase[0])
  } else {
    // Una línea va al medio de la tarjeta; dos o tres, una debajo de la otra (la tarjeta crece con tres).
    parada.lineas.forEach((linea, i) => {
      ctx.font = fuenteCss(linea.destacada ? FUENTE_DE_TEXTO_FUERTE : FUENTE_DE_TEXTO, 22)
      ctx.fillStyle = linea.destacada ? etapa.fuerte : TEXTO
      ctx.fillText(linea.texto, textoX, tarjetaY + parada.lineasBase[i])
    })
  }

  // Punto numerado, sobre el camino
  ctx.fillStyle = "#fff"
  ctx.strokeStyle = etapa.fuerte
  ctx.lineWidth = 5
  ctx.beginPath()
  ctx.arc(x, y, 19, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  ctx.font = fuenteCss(FUENTE_DE_TITULOS, 20)
  ctx.fillStyle = etapa.fuerte
  ctx.textAlign = "center"
  ctx.fillText(String(parada.numero), x, y + 7)
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
function dibujarPie(ctx: CanvasRenderingContext2D) {
  ctx.font = fuenteCss(FUENTE_DE_TITULOS, 24)
  ctx.fillStyle = TITULO
  ctx.textAlign = "right"
  ctx.fillText(PIE_DE_LA_IMAGEN, 968, 1030)

  ctx.save()
  ctx.translate(0, -2)
  ctx.scale(1.1, 1.1)
  ctx.translate(-90, -102)
  ctx.fillStyle = CORAZON
  ctx.fill(new Path2D("M988 1034 C972 1022 978 1010 988 1018 C998 1010 1004 1022 988 1034 Z"))
  ctx.restore()
}

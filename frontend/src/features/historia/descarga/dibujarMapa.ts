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

  dibujarFondo(ctx)
  dibujarTitulo(ctx, nombre)
  dibujarCamino(ctx, diseno.camino)
  diseno.arboles.forEach(([x, y]) => dibujarArbolito(ctx, x, y))
  diseno.banderines.forEach((banderin) => dibujarBanderin(ctx, banderin))
  diseno.paradas.forEach((parada) => dibujarParada(ctx, parada))
  diseno.vacias.forEach((vacia) => dibujarTarjetaVacia(ctx, vacia))
  dibujarPie(ctx)
  ctx.restore()
  return diseno
}

function usar(ctx: CanvasRenderingContext2D, f: Fuente, tamanio: number) {
  ctx.font = fuenteCss(f, tamanio)
}

function dibujarFondo(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = CREMA
  ctx.fillRect(0, 0, ANCHO_DE_LA_IMAGEN, ALTO_DE_LA_IMAGEN)
  ctx.strokeStyle = ROSA
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.roundRect(24, 24, ANCHO_DE_LA_IMAGEN - 48, ALTO_DE_LA_IMAGEN - 48, 60)
  ctx.stroke()
}

/** "La vida de" en Poppins y el nombre en cursiva, centrados juntos; sin nombre, "Mi vida" sin cursiva. */
function dibujarTitulo(ctx: CanvasRenderingContext2D, nombre: string | null | undefined) {
  const partes = partesDelTitulo(nombre)
  ctx.textAlign = "left"
  ctx.textBaseline = "alphabetic"

  if (partes.nombre === null) {
    usar(ctx, FUENTE_DE_TITULOS, TAMANIO_DEL_PREFIJO)
    ctx.fillStyle = TITULO
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
  ctx.fillStyle = TITULO
  ctx.fillText(partes.prefijo, inicio, LINEA_BASE_DEL_TITULO)
  usar(ctx, FUENTE_DEL_NOMBRE, TAMANIO_DEL_NOMBRE * escala)
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
  ctx.setLineDash([14, 14])
  ctx.stroke(trazado)
  ctx.setLineDash([])
}

function dibujarArbolito(ctx: CanvasRenderingContext2D, x: number, y: number) {
  ctx.fillStyle = "#854F0B"
  ctx.fillRect(x - 3, y + 14, 6, 22)
  ctx.fillStyle = "#97C459"
  ctx.beginPath()
  ctx.arc(x, y, 22, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = "#C0DD97"
  ctx.beginPath()
  ctx.arc(x - 6, y - 6, 10, 0, Math.PI * 2)
  ctx.fill()
}

/** Poste con su bolita, cinta con el nombre de la etapa, ícono y círculo numerado sobre el camino. */
function dibujarBanderin(ctx: CanvasRenderingContext2D, b: BanderinDelMapa) {
  const { etapa, poloX, poloArriba, caminoY, cinta, icono } = b
  ctx.strokeStyle = MASTIL
  ctx.lineWidth = 4
  ctx.lineCap = "round"
  ctx.beginPath()
  ctx.moveTo(poloX, poloArriba)
  ctx.lineTo(poloX, caminoY - CIRCULO_RADIO)
  ctx.stroke()
  ctx.fillStyle = MASTIL
  ctx.beginPath()
  ctx.arc(poloX, poloArriba, 7, 0, Math.PI * 2)
  ctx.fill()

  ctx.fillStyle = etapa.fuerte
  ctx.beginPath()
  ctx.roundRect(cinta.x, cinta.y, cinta.ancho, cinta.alto, 5)
  ctx.fill()
  usar(ctx, FUENTE_DE_TITULOS, CINTA_TAMANIO)
  ctx.fillStyle = "#FFFFFF"
  ctx.textAlign = "center"
  ctx.fillText(etapa.nombre, cinta.x + cinta.ancho / 2, cinta.y + cinta.alto / 2 + 9)

  ctx.fillStyle = etapa.claro
  ctx.beginPath()
  ctx.arc(icono.x, icono.y, icono.radio, 0, Math.PI * 2)
  ctx.fill()
  ctx.save()
  ctx.translate(icono.x, icono.y)
  ctx.scale(1.05, 1.05)
  dibujarIlustracion(ctx, etapa.id, etapa.fuerte, etapa.claro)
  ctx.restore()

  ctx.fillStyle = "#FFFFFF"
  ctx.strokeStyle = etapa.fuerte
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.arc(poloX, caminoY, CIRCULO_RADIO - 3, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  usar(ctx, FUENTE_DE_TITULOS, 26)
  ctx.fillStyle = etapa.fuerte
  ctx.textAlign = "center"
  ctx.fillText(String(b.numero), poloX, caminoY + 9)
}

/** Un recuerdo: punto del color de la etapa sobre el camino, tarjeta blanca con el año y el título completo. */
function dibujarParada(ctx: CanvasRenderingContext2D, parada: ParadaDelMapa) {
  const { etapa, punto, tarjeta } = parada
  ctx.strokeStyle = etapa.claro
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.moveTo(punto.x, punto.y + PUNTO_RADIO)
  ctx.lineTo(punto.x, tarjeta.y)
  ctx.stroke()

  ctx.fillStyle = etapa.fuerte
  ctx.strokeStyle = CREMA
  ctx.lineWidth = 4
  ctx.beginPath()
  ctx.arc(punto.x, punto.y, PUNTO_RADIO - 2, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()

  ctx.fillStyle = "#FFFFFF"
  ctx.strokeStyle = etapa.claro
  ctx.lineWidth = 3
  ctx.beginPath()
  ctx.roundRect(tarjeta.x, tarjeta.y, tarjeta.ancho, tarjeta.alto, 16)
  ctx.fill()
  ctx.stroke()

  ctx.textAlign = "left"
  const izquierda = tarjeta.x + 20
  if (tarjeta.anio) {
    usar(ctx, FUENTE_DE_TEXTO_FUERTE, TAMANIO_DEL_ANIO)
    ctx.fillStyle = etapa.fuerte
    ctx.fillText(tarjeta.anio, izquierda, tarjeta.lineaBaseDelAnio)
  }
  usar(ctx, FUENTE_DE_TEXTO, tarjeta.tamanio)
  ctx.fillStyle = TEXTO
  tarjeta.lineas.forEach((linea, i) => {
    ctx.fillText(linea, izquierda, tarjeta.lineaBaseDelTitulo + i * tarjeta.interlineado)
  })
}

/** Etapa sin recuerdos: tarjeta punteada con "Todavía sin recuerdos". */
function dibujarTarjetaVacia(ctx: CanvasRenderingContext2D, vacia: TarjetaVacia) {
  ctx.fillStyle = CREMA
  ctx.strokeStyle = vacia.etapa.claro
  ctx.lineWidth = 3
  ctx.setLineDash([9, 7])
  ctx.beginPath()
  ctx.roundRect(vacia.x, vacia.y, vacia.ancho, vacia.alto, 16)
  ctx.fill()
  ctx.stroke()
  ctx.setLineDash([])
  usar(ctx, FUENTE_DE_TEXTO, 26)
  ctx.fillStyle = TEXTO_DE_APOYO
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
function dibujarPie(ctx: CanvasRenderingContext2D) {
  usar(ctx, FUENTE_DE_TITULOS, 30)
  ctx.fillStyle = TITULO
  ctx.textAlign = "right"
  ctx.fillText(PIE_DE_LA_IMAGEN, 1802, 1030)

  ctx.save()
  ctx.translate(1832, 1020)
  ctx.scale(1.3, 1.3)
  ctx.translate(-988, -1024)
  ctx.fillStyle = CORAZON
  ctx.fill(new Path2D("M988 1034 C972 1022 978 1010 988 1018 C998 1010 1004 1022 988 1034 Z"))
  ctx.restore()
}

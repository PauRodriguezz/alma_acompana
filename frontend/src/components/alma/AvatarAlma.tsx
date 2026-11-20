import avatarAlma from "@/assets/alma-avatar.webp"
import { cn } from "@/lib/utils"

const TAMANIOS = {
  40: "size-[40px]",
  44: "size-[44px]",
} as const

/**
 * Avatar de Alma: la cara (src/assets/alma-avatar.webp) en un círculo blanco (`bg-card`), decorativa
 * (`alt` vacío y `aria-hidden`: lo que se anuncia es el texto de al lado) y con borde en alto contraste.
 * Es el avatar de todos los círculos chicos (mensajes del chat, menús, Entretenimiento); las
 * ilustraciones grandes de cuerpo completo (saludo del chat, final de ronda) no lo usan.
 * Para cambiar la imagen alcanza con reemplazar el archivo.
 * El tamaño es fijo en px (40 o 44) y no crece con el tamaño de letra: es decorativo (ver el contrato
 * del chat, sección 11, y el de juegos, sección 10.4). La cara ocupa 4/5 del círculo.
 */
export function AvatarAlma({ tamano = 40, className }: { tamano?: keyof typeof TAMANIOS; className?: string }) {
  return (
    <span
      aria-hidden="true"
      data-slot="avatar-alma"
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full bg-card contraste-alto:border-2 contraste-alto:border-foreground",
        TAMANIOS[tamano],
        className
      )}
    >
      <img src={avatarAlma} alt="" className="size-4/5 object-contain" />
    </span>
  )
}

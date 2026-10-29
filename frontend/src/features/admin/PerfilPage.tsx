import { Card } from "@/components/ui/card"
import { useAuth } from "@/features/auth/useAuth"

import { EncabezadoPagina } from "./EncabezadoPagina"
import {
  ADMIN_PANEL,
  ADMIN_PERFIL,
  ADMIN_PERFIL_APELLIDO,
  ADMIN_PERFIL_NOMBRE,
  ADMIN_PERFIL_NOMBRE_USUARIO,
  ADMIN_PERFIL_ROL,
  ADMIN_ROL_ADMINISTRADOR,
  ADMIN_ROL_PERSONA_MAYOR,
} from "./mensajes"

const TEXTO_ROL = {
  ADMINISTRADOR: ADMIN_ROL_ADMINISTRADOR,
  PERSONA_MAYOR: ADMIN_ROL_PERSONA_MAYOR,
} as const

/**
 * Perfil de la persona administradora (contrato del panel, secciones 1 y 3.5).
 * PROPUESTO: no hay pantalla en el Figma. Usa los datos de la sesión, que salen de
 * GET /api/auth/me al abrir la app (o del login), así que no hace otra llamada.
 */
export function PerfilPage() {
  const { usuario } = useAuth()

  const datos = usuario
    ? [
        { nombre: ADMIN_PERFIL_NOMBRE, valor: usuario.nombre },
        { nombre: ADMIN_PERFIL_APELLIDO, valor: usuario.apellido },
        { nombre: ADMIN_PERFIL_NOMBRE_USUARIO, valor: usuario.nombreUsuario },
        { nombre: ADMIN_PERFIL_ROL, valor: TEXTO_ROL[usuario.rol] },
      ]
    : []

  return (
    <>
      <EncabezadoPagina
        titulo={ADMIN_PERFIL}
        migas={[{ texto: ADMIN_PANEL, a: "/admin" }, { texto: ADMIN_PERFIL }]}
      />
      <Card className="max-w-(--narrow-max)">
        <dl className="flex flex-col gap-4">
          {datos.map((dato) => (
            <div key={dato.nombre} className="flex flex-col gap-0.5">
              <dt className="font-semibold text-muted-foreground">{dato.nombre}</dt>
              <dd className="wrap-break-word">{dato.valor}</dd>
            </div>
          ))}
        </dl>
      </Card>
    </>
  )
}

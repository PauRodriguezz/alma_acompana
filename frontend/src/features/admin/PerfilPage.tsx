import { Card } from "@/components/ui/card"
import { useAuth } from "@/features/auth/useAuth"

import { EncabezadoPagina } from "./EncabezadoPagina"

const TEXTO_ROL = {
  ADMINISTRADOR: "Administrador",
  PERSONA_MAYOR: "Persona mayor",
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
        { nombre: "Nombre", valor: usuario.nombre },
        { nombre: "Apellido", valor: usuario.apellido },
        { nombre: "Nombre de usuario", valor: usuario.nombreUsuario },
        { nombre: "Rol", valor: TEXTO_ROL[usuario.rol] },
      ]
    : []

  return (
    <>
      <EncabezadoPagina
        titulo="Perfil"
        migas={[{ texto: "Panel de operador", a: "/admin" }, { texto: "Perfil" }]}
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

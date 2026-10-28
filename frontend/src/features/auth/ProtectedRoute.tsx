import { Navigate, Outlet } from "react-router-dom"

import type { Rol } from "./api"
import { useAuth } from "./useAuth"

/** Exige sesión y, si se pasa `roles`, que el rol esté en la lista. Se usa como ruta padre. */
export function ProtectedRoute({ roles }: { roles?: Rol[] }) {
  const { usuario, cargando } = useAuth()

  if (cargando) {
    return (
      <p role="status" className="p-(--page-x)">
        Cargando…
      </p>
    )
  }
  if (!usuario) return <Navigate to="/login" replace />
  if (roles && !roles.includes(usuario.rol)) {
    return <Navigate to="/sin-permiso" replace />
  }
  return <Outlet />
}

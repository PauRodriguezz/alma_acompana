import { Button } from "@/components/ui/button"
import { useAuth } from "@/features/auth/useAuth"

// Pantalla provisoria: se reemplaza cuando exista el Inicio con el chat.
export function InicioPage() {
  const { usuario, logout } = useAuth()

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-(--content-max) flex-col items-start gap-4 px-(--page-x) pt-(--page-top)">
      <h1>Inicio</h1>
      <p>Usuario: {usuario?.nombreUsuario}</p>
      {/* TEMPORAL: reemplazar por confirmación '¿Querés cerrar sesión?' cuando exista Perfil y el diálogo */}
      <Button variant="outline" onClick={logout}>
        Salir
      </Button>
    </main>
  )
}

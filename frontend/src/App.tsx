import { QueryClientProvider } from "@tanstack/react-query"
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"

import { LayoutPersona } from "@/components/alma/layout-persona"
import { PaginaProvisoria } from "@/components/alma/pagina-provisoria"
import { PanelOperadorPage } from "@/features/admin/PanelOperadorPage"
import { AuthProvider } from "@/features/auth/AuthProvider"
import { LoginPage } from "@/features/auth/LoginPage"
import { ProtectedRoute } from "@/features/auth/ProtectedRoute"
import { SinPermisoPage } from "@/features/auth/SinPermisoPage"
import { InicioPage } from "@/features/inicio/InicioPage"
import { PerfilPage } from "@/features/perfil/PerfilPage"
import {
  DetalleCitaPage,
  EditarRecordatorioPage,
  NuevoRecordatorioPage,
  RecordatoriosPage,
} from "@/features/recordatorios"
import { queryClient } from "@/lib/query-client"

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute roles={["PERSONA_MAYOR"]} />}>
              {/* Layout compartido: navegación, Alma y botón de accesibilidad (CU-13) */}
              <Route element={<LayoutPersona />}>
                <Route path="/" element={<InicioPage />} />
                <Route
                  path="/entretenimiento"
                  element={<PaginaProvisoria titulo="Entretenimiento" />}
                />
                <Route
                  path="/historia-de-vida"
                  element={<PaginaProvisoria titulo="Historia de vida" />}
                />
                <Route path="/recordatorios" element={<RecordatoriosPage />} />
                <Route path="/recordatorios/nuevo" element={<NuevoRecordatorioPage />} />
                <Route path="/recordatorios/:id/editar" element={<EditarRecordatorioPage />} />
                <Route path="/recordatorios/:id" element={<DetalleCitaPage />} />
                <Route path="/perfil" element={<PerfilPage />} />
              </Route>
            </Route>
            <Route element={<ProtectedRoute roles={["ADMINISTRADOR"]} />}>
              <Route path="/admin" element={<PanelOperadorPage />} />
            </Route>
            <Route element={<ProtectedRoute />}>
              <Route path="/sin-permiso" element={<SinPermisoPage />} />
            </Route>
            {/* Sin sesión, "/" manda a /login */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App

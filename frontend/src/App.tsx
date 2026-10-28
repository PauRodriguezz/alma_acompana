import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"

import { PanelOperadorPage } from "@/features/admin/PanelOperadorPage"
import { AuthProvider } from "@/features/auth/AuthProvider"
import { LoginPage } from "@/features/auth/LoginPage"
import { ProtectedRoute } from "@/features/auth/ProtectedRoute"
import { SinPermisoPage } from "@/features/auth/SinPermisoPage"
import { InicioPage } from "@/features/inicio/InicioPage"

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute roles={["PERSONA_MAYOR"]} />}>
            <Route path="/" element={<InicioPage />} />
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
  )
}

export default App

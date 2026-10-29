import { Navigate, Route, Routes } from "react-router-dom"

import { EjercicioFormPage } from "@/features/ejercicios/EjercicioFormPage"
import { EjerciciosPage } from "@/features/ejercicios/EjerciciosPage"
import { MicrocapsulaFormPage } from "@/features/microcapsulas/MicrocapsulaFormPage"
import { MicrocapsulasPage } from "@/features/microcapsulas/MicrocapsulasPage"

import { AdminLayout } from "./AdminLayout"
import { PanelOperadorPage } from "./PanelOperadorPage"
import { PerfilPage } from "./PerfilPage"

/**
 * Rutas del panel de operador (contrato del panel, sección 6). Van montadas en App.tsx
 * bajo /admin/*, dentro del ProtectedRoute de ADMINISTRADOR.
 */
export function RutasAdmin() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<PanelOperadorPage />} />
        <Route path="ejercicios" element={<EjerciciosPage />} />
        <Route path="ejercicios/nuevo" element={<EjercicioFormPage />} />
        <Route path="ejercicios/:id/editar" element={<EjercicioFormPage />} />
        <Route path="microcapsulas" element={<MicrocapsulasPage />} />
        <Route path="microcapsulas/nueva" element={<MicrocapsulaFormPage />} />
        <Route path="microcapsulas/:id/editar" element={<MicrocapsulaFormPage />} />
        <Route path="perfil" element={<PerfilPage />} />
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Route>
    </Routes>
  )
}

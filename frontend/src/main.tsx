import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { iniciarAccesibilidad } from '@/features/accesibilidad/almacen'
import { QueryClientProvider } from '@tanstack/react-query'
import { clienteConsultas } from '@/lib/cliente-consultas'

// Antes de renderizar: aplica el tamaño de letra y el contraste guardados en el dispositivo,
// así no hay un parpadeo con el tamaño equivocado.
iniciarAccesibilidad()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={clienteConsultas}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)

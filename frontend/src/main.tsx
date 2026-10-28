import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { iniciarAccesibilidad } from '@/features/accesibilidad/almacen'

// Antes de renderizar: aplica el tamaño de letra y el contraste guardados en el dispositivo,
// así no hay un parpadeo con el tamaño equivocado.
iniciarAccesibilidad()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

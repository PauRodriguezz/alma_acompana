import { QueryClient } from "@tanstack/react-query"

import { ApiError } from "@/lib/api"

// Cliente de TanStack Query para toda la app. Las consultas reintentan una sola vez y
// solo si no hubo respuesta (status 0) o el error es del servidor (5xx): un 401, 403, 404
// o 400 no cambia por repetir el pedido. Las mutaciones no reintentan (no se guarda dos veces).
export const clienteConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (intentos, error) =>
        intentos < 1 &&
        (!(error instanceof ApiError) || error.status === 0 || error.status >= 500),
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})

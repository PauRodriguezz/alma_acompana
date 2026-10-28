import { QueryClient } from "@tanstack/react-query"

import { ApiError } from "./api"

// Un solo reintento, y solo si no fue un error de la petición (4xx): reintentar un 404 o un 400
// no cambia nada. Las consultas se refrescan al volver a la ventana (por defecto de TanStack).
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (intentos, error) =>
        intentos < 1 && !(error instanceof ApiError && error.status >= 400 && error.status < 500),
      refetchOnWindowFocus: true,
    },
  },
})

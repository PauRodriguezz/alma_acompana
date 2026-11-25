# Alma Acompaña · Frontend

Interfaz web de la persona mayor y del panel de operador. React 19, TypeScript, Vite, Tailwind CSS y shadcn/ui. Gestor de paquetes: **pnpm** (no usar npm ni yarn).

Para levantar todo el sistema (bases de datos, backend y configuración) mirá el [README de la raíz](../README.md).

## Comandos

Desde esta carpeta (`frontend/`):

```bash
pnpm install   # instala las dependencias
pnpm dev       # servidor de desarrollo en http://localhost:5173
pnpm lint      # revisa el código
pnpm test      # corre los tests (vitest)
pnpm build     # compila para producción (tsc + vite build)
```

En desarrollo, las llamadas a `/api` y a `/imagenes` pasan por el proxy de Vite hacia el backend en `http://localhost:8080` (ver `vite.config.ts`). El backend tiene que estar levantado.

Para probarlo desde el celular en la misma red wifi: `pnpm dev --host` y abrir `http://IP-DE-LA-COMPU:5173`.

## Estructura

```
src/
├── App.tsx, main.tsx     Rutas y arranque
├── index.css             Colores, tipografías y tamaños (variables de diseño)
├── components/
│   ├── ui/               Componentes base (shadcn/ui): botón, campo, tarjeta, diálogo…
│   ├── alma/             Piezas propias de Alma (avatar, mensajes del chat…)
│   ├── layout/           Menú y estructura de las pantallas
│   ├── formularios/      Campos de formulario del panel
│   └── accesibilidad/    Controles de tamaño de letra y contraste
├── features/             Una carpeta por módulo (chat, recordatorios, bienestar,
│                         juegos, lecturas, historia, perfil, admin…). Cada una tiene
│                         sus páginas, su api.ts (llamadas al backend), sus tipos
│                         y sus textos (mensajes.ts).
└── lib/                  Utilidades compartidas: cliente de la API, errores,
                          fechas, textos comunes (mensajes.ts) y manejo del token
```

## Convenciones

- Todo el texto que ve la persona está en español (voseo, frases cortas) y vive en archivos `mensajes.ts`, no en los componentes.
- Cada pantalla respeta el tamaño de letra y el contraste que elige la persona (100, 125 o 150 %, y alto contraste).

## Con Docker

El `Dockerfile` de esta carpeta construye la app y la sirve con nginx (`nginx.conf`), que también reenvía `/api` y `/imagenes` al backend. Normalmente no se usa a mano: se levanta con `docker compose up -d --build` desde la raíz (ver `../README.md`).
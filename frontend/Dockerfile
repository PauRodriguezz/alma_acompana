# syntax=docker/dockerfile:1

# --- Etapa 1: compilar con pnpm ---
FROM node:22-alpine AS build
WORKDIR /app

# El lockfile es v9 (pnpm 9 o 10). package.json no declara packageManager, así que se fija acá.
RUN corepack enable && corepack prepare pnpm@10 --activate

COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

# --- Etapa 2: nginx sin root sirviendo los estáticos (escucha en el 8080 interno) ---
FROM nginxinc/nginx-unprivileged:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

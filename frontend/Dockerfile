# /frontend/Dockerfile

# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./

# Force devDependencies (so ng exists) even if env defaults to production
RUN npm ci --include=dev

# Debug helpers (remove later if you want)
RUN node -v && npm -v && ls -la node_modules/.bin | head -n 20

COPY . .
RUN npm run build


# ---- runtime stage ----
FROM nginx:alpine

# SPA + reverse proxy config
COPY ./nginx/default.conf /etc/nginx/conf.d/default.conf

# IMPORTANT:
# Angular 17+ typically outputs to dist/<app-name>/browser
# So we copy everything under dist/*/browser into nginx web root.
COPY --from=build /app/dist/*/browser /usr/share/nginx/html

# /frontend/Dockerfile

# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci --include=dev

COPY . .
RUN npm run build -- --configuration production   # <-- THIS LINE CHANGED

# ---- runtime stage ----
FROM nginx:alpine
COPY ./nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/*/browser /usr/share/nginx/html

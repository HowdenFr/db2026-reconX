# ============================================================================
# TICKET-ADV147 — React/Vite Dockerfile (multi-stage Node -> Nginx)
#
# Stage 1: Build the production React/Vite application using Node.
# Stage 2: Copy ONLY the compiled dist/ files into a lightweight nginx image.
# ============================================================================
FROM node:22-alpine AS build

WORKDIR /app

# Copy dependency files first (improves Docker cache)
COPY package.json package-lock.json* ./

# Install dependencies
RUN npm install --no-audit --no-fund

# Copy application source
COPY . .

# Build production assets
RUN npm run build


# ============================================================================
# Runtime Image
# ============================================================================

FROM nginx:1.27-alpine

# Copy ONLY the built static site
COPY --from=build /app/dist /usr/share/nginx/html

# Custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
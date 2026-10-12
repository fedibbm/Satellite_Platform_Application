# =============================================================================
# SATELLITE FRONTEND - PRODUCTION DOCKERFILE
# Multi-Stage Build: Node 20 Alpine -> Builder -> Next.js Standalone Runner
# =============================================================================

# Stage 1: Dependencies Cache
FROM node:20-alpine AS deps
WORKDIR /app

# Check https://github.com/nodejs/docker-node/tree/b4117f9333da4138b03a546ec926ef50a31506c3#nodealpine to understand why libc6-compat might be needed.
RUN apk add --no-cache libc6-compat

COPY package.json package-lock.json* ./
RUN npm ci || npm install

# Stage 2: Next.js Production Builder
FROM node:20-alpine AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build arguments for frontend public API endpoints
ARG NEXT_PUBLIC_API_BASE_URL=http://api.satellite.localhost
ARG NEXT_PUBLIC_GEE_API_URL=http://localhost:5000
ARG NEXT_PUBLIC_IMAGE_API_URL=http://localhost:8000

ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_PUBLIC_GEE_API_URL=$NEXT_PUBLIC_GEE_API_URL \
    NEXT_PUBLIC_IMAGE_API_URL=$NEXT_PUBLIC_IMAGE_API_URL \
    NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# Stage 3: Minimal Production Runner
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME="0.0.0.0"

# Security: Dedicated unprivileged nodejs system user
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy static assets and compiled standalone server
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

# Verified healthcheck matching App Router /api/health
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000/api/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})" || exit 1

CMD ["node", "server.js"]

# Stage 1: Install dependencies
FROM node:22-alpine3.21 AS deps
WORKDIR /app

# Copy lockfiles and package files for dependency installation caching
COPY package*.json ./
COPY apps/web/package*.json ./apps/web/
COPY apps/partner/package*.json ./apps/partner/
COPY apps/owner/package*.json ./apps/owner/
COPY packages/shared/package*.json ./packages/shared/

# Install dependencies using clean install
RUN npm ci

# Stage 2: Build the application
FROM node:22-alpine3.21 AS builder
ARG APP_NAME=web
ARG NEXT_PUBLIC_API_URL
ARG NEXT_PUBLIC_WEB_URL
ARG NEXT_PUBLIC_PARTNER_URL
ARG NEXT_PUBLIC_OWNER_URL
ARG NEXT_PUBLIC_VIETMAP_API_KEY
ARG NEXT_PUBLIC_VIETMAP_TILE_KEY
ARG NEXT_PUBLIC_GOONG_API_KEY
ARG NEXT_PUBLIC_GOONG_MAPTILES_KEY
ARG NEXT_PUBLIC_TURNSTILE_SITE_KEY
ARG NEXT_PUBLIC_SENTRY_DSN

WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN mkdir -p apps/${APP_NAME}/public

# Set environment variables for production build
ENV NODE_ENV=production
ENV BACKEND_URL=http://server:8080
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_WEB_URL=$NEXT_PUBLIC_WEB_URL
ENV NEXT_PUBLIC_PARTNER_URL=$NEXT_PUBLIC_PARTNER_URL
ENV NEXT_PUBLIC_OWNER_URL=$NEXT_PUBLIC_OWNER_URL
ENV NEXT_PUBLIC_VIETMAP_API_KEY=$NEXT_PUBLIC_VIETMAP_API_KEY
ENV NEXT_PUBLIC_VIETMAP_TILE_KEY=$NEXT_PUBLIC_VIETMAP_TILE_KEY
ENV NEXT_PUBLIC_GOONG_API_KEY=$NEXT_PUBLIC_GOONG_API_KEY
ENV NEXT_PUBLIC_GOONG_MAPTILES_KEY=$NEXT_PUBLIC_GOONG_MAPTILES_KEY
ENV NEXT_PUBLIC_TURNSTILE_SITE_KEY=$NEXT_PUBLIC_TURNSTILE_SITE_KEY
ENV NEXT_PUBLIC_SENTRY_DSN=$NEXT_PUBLIC_SENTRY_DSN

RUN npm run build:${APP_NAME}

# Stage 3: Runner
FROM node:22-alpine3.21 AS runner
ARG APP_NAME=web
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy static assets and public files
COPY --from=builder /app/apps/${APP_NAME}/public ./apps/${APP_NAME}/public

# Set correct permission for prerender cache
RUN mkdir .next
RUN chown nextjs:nodejs .next

# Copy Next.js standalone build output and static bundle
COPY --from=builder --chown=nextjs:nodejs /app/apps/${APP_NAME}/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/apps/${APP_NAME}/.next/static ./apps/${APP_NAME}/.next/static

ENV APP_NAME_ENV=${APP_NAME}

USER nextjs
EXPOSE 3000

CMD node apps/${APP_NAME_ENV}/server.js

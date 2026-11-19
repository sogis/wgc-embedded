# Bundle the web assets on the builder's native platform.
FROM --platform=$BUILDPLATFORM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY viewer.jsx viewer.css ./
RUN npm run build

# Keep the runtime stage free of RUN instructions for cross-platform builds.
FROM node:22-alpine@sha256:1314fb30ec37592d5845e5a160d522c0e680972aa534549940b8d73bde88481a
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules/qwc2-lts/LICENSE ./QWC2-LICENSE
COPY LICENSE index.html server.mjs ./
USER node
EXPOSE 8080
CMD ["node", "server.mjs"]

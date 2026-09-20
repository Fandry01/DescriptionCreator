# DescriptionCreator

Monorepo for the DescriptionCreator application. This repository currently contains only the initial application scaffolding; business features have not been implemented.

## Repository structure

```text
.
├── backend/    Spring Boot API
├── frontend/   React + TypeScript + Vite client
└── docs/       Project documentation
```

## Prerequisites

- Java 21 or newer
- Node.js 22.12 or newer
- npm 10 or newer

Maven does not need to be installed globally because the backend includes the Maven wrapper.

## Environment

Copy `.env.example` to `.env` and adjust values for your local environment. Environment files are ignored by Git except for the example file.

For local Shopify OAuth, configure these values in the root `.env` file:

```properties
SHOPIFY_SHOP_DOMAIN=your-store.myshopify.com
SHOPIFY_CLIENT_ID=your-client-id
SHOPIFY_CLIENT_SECRET=your-client-secret
SHOPIFY_API_VERSION=2026-07
SHOPIFY_REDIRECT_URI=http://localhost:8080/api/shopify/callback
```

The backend reads the root `.env` file only when the `dev` Spring profile is active. Never commit the populated `.env` file.

## Run locally

Start the backend:

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Start the frontend in another terminal:

```sh
cd frontend
npm install
npm run dev
```

The backend defaults to `http://localhost:8080`; Vite prints the frontend development URL when it starts.

## Test Shopify OAuth locally

1. Start the backend with the `dev` profile as shown above.
2. Open `http://localhost:8080/api/shopify/auth` in a browser.
3. Approve the requested Shopify `read_products` access.
4. Shopify redirects to the configured localhost callback.
5. Open `http://localhost:8080/api/shopify/status` to confirm the connection.

The access token is kept only in backend memory and is lost when the application restarts.

## Verification

```sh
cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```

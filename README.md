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

## Run locally

Start the backend:

```sh
cd backend
./mvnw spring-boot:run
```

Start the frontend in another terminal:

```sh
cd frontend
npm install
npm run dev
```

The backend defaults to `http://localhost:8080`; Vite prints the frontend development URL when it starts.

## Verification

```sh
cd backend && ./mvnw test
cd frontend && npm run lint && npm run build
```


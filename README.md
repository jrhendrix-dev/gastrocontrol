# GastroControl

> **Full-stack restaurant management platform** — POS terminal, customer-facing ordering app, and multi-tenant portfolio demo — built for the Spanish hostelería sector.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-21-red?logo=angular)](https://angular.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)](https://docs.docker.com/compose/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Table of Contents

- [Overview](#overview)
- [Live Demo](#live-demo)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Run with Docker Compose](#run-with-docker-compose-recommended)
  - [Run Locally (Development)](#run-locally-development)
- [API Overview](#api-overview)
- [Deployment](#deployment)
- [Key Design Decisions](#key-design-decisions)
- [Known Limitations](#known-limitations)
- [Acknowledgements](#acknowledgements)
- [License](#license)

---

## Overview

GastroControl is a production-grade restaurant operations platform designed to replace third-party POS and delivery solutions with a single, self-hosted system. It covers two distinct user experiences:

- **Internal operations** — a staff POS terminal and admin/manager panel for day-to-day table service, takeaway orders, and back-office management.
- **Customer-facing ordering** — a public menu catalog and checkout flow (Stripe or cash) modelled after apps like Just Eat or Uber Eats, allowing restaurants to own their direct ordering channel without paying commissions.

The platform is live at **[jonathan-hendrix.dev/gastrocontrol](https://jonathan-hendrix.dev/gastrocontrol)** with an interactive multi-tenant demo that provisions an isolated database schema per visitor session.

---

## Live Demo

A fully functional demo environment is available at the link above. Each session gets its own isolated MySQL schema seeded with realistic sample data. No sign-up is required — demo credentials are provided on the landing page.

> ⚠️ Demo sessions are automatically cleaned up after 2 hours.

---

## Features

### Customer Ordering App
- Public product catalog with category navigation and IntersectionObserver scroll-spy
- Two-step checkout wizard (order details → payment method)
- Online payment via **Stripe Checkout** or **cash on pickup/delivery**
- UUID-based order tracking token (prevents order ID enumeration)
- Real-time order status tracking with 5-second polling

### Staff POS Terminal
- Create, update, and close dine-in, take-away, and delivery orders
- Phone order creation with customer note management (add / edit / delete)
- 15-second order list polling with optimistic UI updates
- Payment method override for managers
- Externos section for take-away and delivery queue management

### Admin & Manager Panel
- Role-aware tab shell (`MANAGER` vs `ADMIN` views)
- **Dashboard** — daily revenue, order stats (uses `closedAt` for accuracy)
- **Products** — full CRUD, hero image upload, discontinue/reactivate
- **Categories** — create, rename, reorder
- **Tables** — manage dine-in table layout
- **Users** (Admin only) — invite users by email, deactivate/reactivate, force password reset, change email

### Multi-Tenant Demo System
- Per-session MySQL schema isolation via `TenantContext` (InheritableThreadLocal)
- `TenantFilter` runs before Spring Security so schema context is set before `UserDetailsService` queries fire
- Automated schema provisioning: schema creation → Flyway migrations → seed data
- Scheduled cleanup job (every 30 minutes) purges expired sessions
- Angular `DemoSessionStore` with signal-based live countdown

### Security
- JWT-based authentication with role-based access control (`ADMIN`, `MANAGER`, `STAFF`, `CUSTOMER`)
- Password reset via tokenised email link (Mailgun)
- Stripe webhook signature verification
- Nginx rate limiting on all API endpoints

---

## Architecture

We follow **Clean Architecture** principles throughout:

```
Controller (HTTP) → Application Service (Use Case) → Domain → Infrastructure (DB / External)
```

- **Controllers** handle HTTP concerns only — request parsing and response serialisation.
- **Application Services** contain all business logic and orchestrate domain operations.
- **Domain** holds pure models, enums, and business rules with no framework dependencies.
- **Infrastructure** contains JPA entities, repositories, and external adapters (Stripe, Mailgun).
- **DTOs** are kept separate from domain models; mapping is explicit via dedicated `Mapper` classes.

---

## Tech Stack

### Backend
| Concern | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security + JWT |
| Database | MySQL 8.0 |
| Migrations | Flyway |
| ORM | Hibernate / Spring Data JPA |
| Build | Maven |
| Payments | Stripe Java SDK |
| Email | Mailgun (HTTP API) |
| Utilities | Lombok |

### Frontend
| Concern | Technology |
|---|---|
| Framework | Angular 21 (standalone components) |
| State | Angular Signals + OnPush change detection |
| Styling | Tailwind CSS |
| Language | TypeScript 5.9 |
| Testing | Vitest |

### Infrastructure
| Concern | Technology |
|---|---|
| Containerisation | Docker / Docker Compose (Podman-compatible) |
| Reverse Proxy | Nginx |
| Hosting | DigitalOcean Droplet (Ubuntu 22.04) |
| Local Dev | Bazzite/Fedora with Podman |

---

## Project Structure

```
gastrocontrol/
├── backend/
│   └── src/main/java/com/gastrocontrol/gastrocontrol/
│       ├── application/        # Use cases and port interfaces
│       │   └── service/        # One service class per use case
│       ├── common/             # Shared exceptions, ApiResponse, utilities
│       ├── config/             # Spring configuration (Security, Flyway, multi-tenancy)
│       │   └── multitenancy/   # TenantContext, TenantFilter, SchemaAwareDataSource
│       ├── controller/         # REST controllers grouped by actor
│       │   ├── admin/          # /api/admin/** (ADMIN only)
│       │   ├── manager/        # /api/manager/** (MANAGER+)
│       │   ├── staff/          # /api/staff/** (STAFF+)
│       │   └── customer/       # /api/customer/** (public + CUSTOMER)
│       ├── demo/               # DemoSessionService, DemoCleanupJob
│       ├── domain/             # Domain models, enums, business rules
│       ├── dto/                # Request/Response DTOs per actor
│       ├── infrastructure/     # JPA entities, repositories, Stripe/Mail adapters
│       ├── mapper/             # Domain ↔ DTO mapping
│       └── security/           # JWT filter, UserDetailsService, token utilities
├── frontend/
│   └── src/app/
│       ├── core/               # Auth service, interceptors, guards
│       ├── features/
│       │   ├── admin/          # Admin/Manager panel (tab shell + tabs)
│       │   ├── customer/       # Public catalog, cart, checkout, order tracking
│       │   └── staff/          # POS terminal, externos, order management
│       └── layout/             # Navbar, shared layout components
├── docker-compose.yml          # Local development stack
├── docker-compose.prod.yml     # Production deployment stack
└── docs/                       # Architecture and subsystem decision records
```

---

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose** (or Podman with the Docker CLI emulator)
- **Java 21** and **Maven 3.8+** (for running locally without Docker)
- A **Stripe** account (test keys are sufficient for development)
- A **Mailgun** account (optional — email features degrade gracefully when disabled)

### Environment Variables

Create a `.env` file in the project root for local development. **Do not use base64-padded secrets** (values containing `=`) — use hex secrets instead:

```bash
# Generate safe secrets
openssl rand -hex 32   # for JWT_SECRET, DB_PASSWORD, etc.
```

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | Database hostname | `db` (Docker service name) |
| `DB_PORT` | Database port | `3306` |
| `DB_NAME` | Database name | `gastrocontrol` |
| `DB_USER` | Database username | `gastrocontrol` |
| `DB_PASSWORD` | Database password | **Required** |
| `JWT_SECRET` | JWT signing key (≥ 32 hex chars) | **Required** |
| `MAIL_ENABLED` | Enable email sending | `false` |
| `MAILGUN_API_KEY` | Mailgun API key | — |
| `MAILGUN_DOMAIN` | Mailgun sending domain | — |
| `MAILGUN_BASE_URL` | Mailgun API base URL | `https://api.eu.mailgun.net` |
| `MAIL_FROM_EMAIL` | Sender address | `no-reply@jonathan-hendrix.dev` |
| `MAIL_FROM_NAME` | Sender display name | `GastroControl` |
| `STRIPE_API_KEY` | Stripe secret key (`sk_test_...`) | **Required** |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | **Required** |
| `STRIPE_CURRENCY` | Billing currency (ISO 4217) | `eur` |
| `STRIPE_CHECKOUT_SUCCESS_URL` | Redirect URL after successful payment | `http://localhost:4200/checkout/success?session_id={CHECKOUT_SESSION_ID}` |
| `STRIPE_CHECKOUT_CANCEL_URL` | Redirect URL on cancelled payment | `http://localhost:4200/checkout/cancel?session_id={CHECKOUT_SESSION_ID}` |
| `STRIPE_VERIFY_WEBHOOK_SIGNATURE` | Enable Stripe webhook signature check | `true` |

### Run with Docker Compose (Recommended)

This starts MySQL, the Spring Boot API, the Angular frontend, and a Stripe CLI webhook forwarder.

```bash
# In the project root
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| MySQL | localhost:3307 |

The database schema is created and migrated automatically by Flyway on first startup.

### Run Locally (Development)

Start only the database in Docker, then run the backend and frontend separately:

```bash
# 1. Start the database
docker compose up -d db

# 2. Run the backend (from /backend)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Run the frontend (from /frontend)
npx ng serve
```

> **Note:** The Angular CLI (`ng`) is not globally available on all systems. Use `npx ng` or the Docker build approach.

---

## API Overview

All endpoints return a consistent JSON envelope:

```json
// Success
{ "ok": true, "message": "...", "data": { ... } }

// Error
{ "error": { "code": "VALIDATION_FAILED", "details": { "field": "reason..." } } }
```

### Endpoint Groups

| Prefix | Auth Required | Roles | Description |
|---|---|---|---|
| `/api/auth/**` | No | — | Login, register, password reset |
| `/api/customer/**` | No | `CUSTOMER` | Public catalog, checkout, order tracking |
| `/api/staff/**` | Yes | `STAFF`+ | POS operations, order management |
| `/api/manager/**` | Yes | `MANAGER`+ | Product/category/table management, order overrides |
| `/api/admin/**` | Yes | `ADMIN` | User management |
| `/api/demo/**` | No | — | Demo session provisioning |
| `/api/webhooks/stripe` | No | — | Stripe event receiver (signature-verified) |

---

## Deployment

We use a manual deploy pipeline to a DigitalOcean droplet running Ubuntu 22.04. The backend is shipped as a Docker image; the frontend is compiled and synced as static files served by Nginx.

```bash
# ── Backend ───────────────────────────────────────────────────────────────
cd /var/home/silversoth/Repos/GastroControl/backend
docker compose build api
docker save gastrocontrol-api:latest | gzip > /tmp/gastrocontrol-backend.tar.gz
scp /tmp/gastrocontrol-backend.tar.gz $DEPLOY_USER@$DEPLOY_HOST:/var/www/gastrocontrol/gastrocontrol-backend.tar.gz

# ── Frontend ──────────────────────────────────────────────────────────────
cd /var/home/silversoth/Repos/GastroControl/frontend
docker compose build frontend 2>&1 | tail -5
docker compose up -d frontend
docker cp gastrocontrol-frontend:/usr/share/nginx/html/. ./dist/gastrocontrol-frontend/browser/
rsync -avz --delete \
  ./dist/gastrocontrol-frontend/browser/ \
  $DEPLOY_USER@$DEPLOY_HOST:/var/www/gastrocontrol/dist/

# ── Server ────────────────────────────────────────────────────────────────
ssh $DEPLOY_USER@$DEPLOY_HOST "cd /var/www/gastrocontrol && \
  docker load < gastrocontrol-backend.tar.gz && \
  docker tag gastrocontrol-api:latest localhost/gastrocontrol-backend:prod && \
  docker compose -f docker-compose.prod.yml --env-file .env up -d --force-recreate backend"
```

> Set `DEPLOY_HOST` and `DEPLOY_USER` in your shell environment or a local `.envrc` (not committed) before running.

### Production Infrastructure

```
Internet → Nginx (bfe_web container) → /gastrocontrol/api/** → gastrocontrol-backend container
                                      → /gastrocontrol/**     → static files at /var/www/gastrocontrol/dist/
```

- **Nginx config:** `/opt/business-first-overhaul/nginx/default.conf`
- **Static files:** `/var/www/gastrocontrol/dist/`
- **Docker Compose stack:** `/var/www/portfolio/docker-compose.prod.yml`
- **TLS:** Let's Encrypt via Certbot (webroot authenticator), auto-reload on renewal

---

## Key Design Decisions

We document the non-obvious *why* behind decisions as thoroughly as the *what*, since the reasoning is what ages well.

| Decision | Rationale |
|---|---|
| `TenantFilter` runs at `@Order(1)`, before Spring Security | Schema context must be set before `UserDetailsService` fires its DB query during authentication |
| Demo users exist in the **main schema** | Spring Security authenticates against the main schema; tenant switching only happens after auth succeeds |
| Revenue queries use `closedAt`, not `createdAt` | Orders opened early but closed today must appear in today's dashboard |
| Cash orders skip `DRAFT` status | Cash payment is confirmed at the point of order creation; no pending payment state needed |
| UUID tracking tokens instead of order IDs | Prevents order ID enumeration by external customers on the public tracking endpoint |
| `connectionInitSql` on HikariCP for schema switching | The most reliable hook for per-connection schema switching without full Hibernate multi-tenancy SPI |
| Hex secrets in `.env` files | Docker Compose doesn't support shell quoting — passwords with `=` (base64 padding) break silently |
| Direct `proxy_pass http://service-name:port` in Nginx | Podman's network stack doesn't expose Docker's internal DNS resolver; variable-based proxy_pass fails |
| `apiBase: ''` in `environment.prod.ts` | Enables relative URL routing through Nginx correctly without hardcoding a host |

---

## Known Limitations

- **Refunds** — the Stripe Refund API adapter is stubbed. Manager-initiated refunds currently record a manual adjustment and do not trigger a real Stripe refund.
- **`edited_by` on order notes** — deferred pending more robust per-user auth. `TODO` comments are left in both note services.
- **No real-time push** — order updates use polling (5 s for customers, 15 s for staff). WebSocket/SSE support is a future improvement.
- **Single restaurant** — multi-tenancy is implemented for the demo system only; true multi-restaurant support (separate accounts managing separate restaurants) is not yet built.
- **No e2e test suite** — tests cover unit and integration levels; an end-to-end suite (Playwright/Cypress) is planned.

---

## Acknowledgements

- **[Unsplash](https://unsplash.com)** — All product images used in the demo are sourced from Unsplash and are used in accordance with the [Unsplash License](https://unsplash.com/license). Images are for demonstration purposes only and are not included in this repository.
- **[Stripe](https://stripe.com)** — Payment processing and Stripe CLI for local webhook forwarding during development.
- **[Mailgun](https://www.mailgun.com)** — Transactional email delivery for password reset and user invite flows.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

You are free to use, copy, modify, and distribute this code for personal or commercial purposes with attribution. If you're considering using this as the basis of a commercial product, please reach out — contributions and collaboration are welcome.

---

<p align="center">Built by <a href="https://jonathan-hendrix.dev">Jonathan Hendrix</a> · Seville, Spain</p>

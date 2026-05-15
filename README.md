# Foods & Drinks

Full-stack food & drink ordering platform built with Spring Boot.

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Framework | Java 25 · Spring Boot 4.0.5 |
| Persistence | Spring Data JPA · Hibernate · MySQL 8 |
| Security | Spring Security · JWT (jjwt 0.12) · OAuth2 (Google, Facebook, Apple) |
| Templating | Thymeleaf (Admin UI) |
| Notifications | JavaMail (Gmail SMTP) · Slack Incoming Webhook |
| Build / Container | Maven 3.9 · Docker Compose |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |

---

## Features

### User (REST API)
- Register / Login / Logout with JWT
- Social login — Google, Facebook, Apple (OAuth2 + OIDC)
- Browse products with filters: letter, type (FOOD/DRINK), category, price range, min rating
- Shopping cart — add, update quantity, remove, clear
- Place orders from cart
- View order history and order detail
- Rate products (create or update own rating)
- Submit product suggestions to admin
- View personal summary (cart + full order history)
- View / update profile

### Admin (Web UI — `/admin/`)
- Dashboard with live statistics
- Manage users — view, activate/deactivate
- Manage categories — CRUD
- Manage products — CRUD with image uploads
- Manage orders — list, view detail, update status
- Review suggestions — view, approve/reject
- Manual trigger for monthly report email

### System Features
- **Slack notification** — posts to a Slack channel on every new order (async, fires only after DB commit)
- **Email notification** — sends order detail to admin inbox on every new order
- **Monthly report** — scheduled email at 08:00 on the 1st of each month with order statistics for the past 30 days; can be triggered manually from the Admin Dashboard

---

## Getting Started

### Prerequisites
- Java 25
- Maven 3.9+
- Docker & Docker Compose

### 1. Start MySQL
```bash
docker compose up -d
```

### 2. Configure `application.yml`

Copy the placeholder values and fill in real credentials (or set the corresponding environment variables):

| Property | Env var | Purpose |
|---|---|---|
| `app.jwt.secret` | — | JWT signing key (min 32 chars) |
| `spring.mail.username` | `MAIL_USERNAME` | Gmail sender address |
| `spring.mail.password` | `MAIL_PASSWORD` | Gmail App Password |
| `app.notification.recipient-email` | `NOTIFICATION_RECIPIENT_EMAIL` | Admin notification inbox |
| `app.slack.webhook-url` | `SLACK_WEBHOOK_URL` | Slack Incoming Webhook URL |
| `spring.security.oauth2.client.registration.google.*` | `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth2 |
| `spring.security.oauth2.client.registration.facebook.*` | `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | Facebook OAuth2 |
| `spring.security.oauth2.client.registration.apple.*` | `APPLE_CLIENT_ID` / `APPLE_TEAM_ID` / `APPLE_KEY_ID` / `APPLE_PRIVATE_KEY` | Apple Sign In |
| `app.oauth2.redirect-uri` | `APP_OAUTH2_REDIRECT_URI` | Post-OAuth2 redirect (default: `http://localhost:8080/oauth2/callback`) |

> **Gmail App Password**: Go to Google Account → Security → 2-Step Verification → App passwords. Use the generated 16-character password.

> **Slack Webhook**: Go to your Slack workspace → Apps → Incoming WebHooks → Add Configuration.

### 3. Run the application
```bash
./mvnw spring-boot:run
```

The app starts on **port 8080** and auto-creates the default admin account (`admin@foodsdrinks.com` / `Admin@123456`) on first run.

---

## Access Points

| URL | Description |
|---|---|
| `http://localhost:8080/api/` | REST API (JWT auth) |
| `http://localhost:8080/admin/` | Admin UI (form login) |
| `http://localhost:8080/swagger-ui.html` | Swagger UI (interactive API docs) |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 JSON spec |

---

## REST API Reference

All API responses follow the unified envelope:
```json
{
  "status": 200,
  "message": "OK",
  "data": { ... }
}
```

### Authentication

Endpoints that require authentication expect the header:
```
Authorization: Bearer <token>
```

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | — | Register a new user |
| POST | `/api/auth/login` | — | Login, returns JWT |
| POST | `/api/auth/logout` | Bearer | Logout (client-side token discard) |
| GET | `/oauth2/authorization/google` | — | Initiate Google OAuth2 login |
| GET | `/oauth2/authorization/facebook` | — | Initiate Facebook OAuth2 login |
| GET | `/oauth2/authorization/apple` | — | Initiate Apple Sign In |

### User Profile

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/users/me` | Bearer | Get own profile |
| PUT | `/api/users/me` | Bearer | Update own profile |

### Categories

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/categories` | — | List all categories |
| GET | `/api/categories/{id}` | — | Get category by ID |
| POST | `/api/categories` | Bearer | Create category |
| PUT | `/api/categories/{id}` | Bearer | Update category |
| DELETE | `/api/categories/{id}` | Bearer | Delete category |

### Products

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/products` | — | List products with filters |
| GET | `/api/products/{id}` | — | Get product by ID |
| POST | `/api/products` | Bearer | Create product |
| PUT | `/api/products/{id}` | Bearer | Update product |
| DELETE | `/api/products/{id}` | Bearer | Delete product |
| POST | `/api/products/{id}/images` | Bearer | Upload product image |
| DELETE | `/api/products/{id}/images/{imageId}` | Bearer | Delete product image |
| GET | `/api/products/{productId}/ratings` | — | List ratings for a product |

**Product filter query params:** `letter`, `type` (FOOD/DRINK), `categoryId`, `minPrice`, `maxPrice`, `minRating`, `page`, `size`, `sort`

### Cart

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/cart` | Bearer | View current cart |
| POST | `/api/cart/items` | Bearer | Add item (or increment quantity) |
| PUT | `/api/cart/items/{productId}` | Bearer | Set quantity for a cart item |
| DELETE | `/api/cart/items/{productId}` | Bearer | Remove one item |
| DELETE | `/api/cart` | Bearer | Clear entire cart |

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/orders` | Bearer | Place order from current cart |
| GET | `/api/orders` | Bearer | Order history (summary) |
| GET | `/api/orders/{id}` | Bearer | Order detail with items |

### Ratings

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/ratings` | Bearer | Create or update own rating |

### Suggestions

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/suggestions` | Bearer | Submit a product suggestion |
| GET | `/api/suggestions` | Bearer | List own suggestions |

### Summary

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/summary` | Bearer | Current cart + full order history |

---

## Admin UI Reference

Login at `http://localhost:8080/admin/login` with `admin@foodsdrinks.com` / `Admin@123456`.

| Path | Description |
|---|---|
| `/admin/dashboard` | Statistics overview + quick actions |
| `/admin/users` | User list |
| `/admin/users/{id}` | User detail + activate/deactivate |
| `/admin/categories` | Category list |
| `/admin/categories/new` | Create category |
| `/admin/categories/{id}/edit` | Edit category |
| `/admin/products` | Product list |
| `/admin/products/new` | Create product |
| `/admin/products/{id}/edit` | Edit product |
| `/admin/orders` | Order list |
| `/admin/orders/{id}` | Order detail + status update |
| `/admin/suggestions` | Suggestion list |
| `/admin/suggestions/{id}` | Suggestion detail + approve/reject |
| `/admin/notifications/monthly-report/trigger` | Manually trigger monthly report email (POST) |

---

## System Notifications

### Slack

Configure `app.slack.webhook-url` with an Incoming Webhook URL from your Slack workspace.
A message is posted after every successful order placement (async, fires only after DB transaction commits).

### Email — Order Alert

Configure `spring.mail.*` and `app.notification.recipient-email`.
An email with full order detail is sent to the recipient address on every new order.

### Email — Monthly Report

A plain-text statistics email is sent at **08:00 on the 1st of every month** covering orders from the past 30 days.
Can also be triggered manually from the Admin Dashboard.

Report includes: total orders, breakdown by status (pending/confirmed/delivering/completed/cancelled), and revenue from completed orders.

---

## Project Structure

```
src/main/java/com/fooddrinks/
├── common/          # ApiResponse envelope
├── config/          # Security, OAuth2, Async, Web, OpenAPI configs
├── controller/
│   ├── admin/       # Thymeleaf MVC controllers
│   └── api/         # REST API controllers
├── dto/
│   ├── request/     # Request DTOs (validated with Bean Validation)
│   └── response/    # Response DTOs
├── entity/          # JPA entities + enums (OrderStatus, ProductType, Role, Provider)
├── event/           # OrderPlacedEvent + NotificationEventListener
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── service/         # Service interfaces
│   └── impl/        # Service implementations
└── util/            # ApiPaths, AdminPaths, JwtUtil, FileUploadUtil, MonthlyReportScheduler
```
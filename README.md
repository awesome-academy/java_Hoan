# Foods & Drinks

Ordering platform for food and drinks built with Spring Boot.

## Tech Stack
- Java 25 + Spring Boot 4.0.5
- Spring Data JPA + Hibernate + MySQL 8
- Spring Security + JWT + OAuth2 (Google, Facebook, Apple)
- Thymeleaf (Admin UI) + REST API (User)
- Docker (MySQL)

## Getting Started

### 1. Start MySQL with Docker
```bash
docker compose up -d
```

### 2. Configure `application.yml`
Fill in the placeholder values:
- OAuth2 client IDs and secrets
- Mail credentials
- Slack webhook URL
- JWT secret (minimum 32 characters)

### 3. Run the application
```bash
./mvnw spring-boot:run
```

### API Base URLs
- REST API: `http://localhost:8080/api/`
- Admin UI: `http://localhost:8080/admin/`

## Project Structure
```
src/main/java/com/fooddrinks/
├── common/         # ApiResponse wrapper
├── config/         # Security, OAuth2, file upload configs
├── controller/
│   ├── admin/      # Thymeleaf controllers
│   └── api/        # REST API controllers
├── dto/
│   ├── request/    # Request bodies
│   └── response/   # Response bodies
├── entity/         # JPA entities + enums
├── exception/      # Custom exceptions + global handler
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
│   └── impl/
└── util/           # Utilities (JWT, file upload, etc.)
```
# E-Commerce Backend

Spring Boot 3.3 API for the e-commerce application.

## Prerequisites

- Java 17+
- PostgreSQL 14+ (or Docker)
- Maven Wrapper included (`mvnw` / `mvnw.cmd`)

## Database setup

### Option A — Docker Compose (from repo root)

```bash
docker compose up -d postgres
```

Creates database `ecommerce_db` with user/password `postgres` / `postgres` on port `5432`.

### Option B — Local PostgreSQL

```sql
CREATE DATABASE ecommerce_db;
```

### Schema (Flyway)

Tables are created by Flyway migrations in `src/main/resources/db/migration/`.

- Hibernate `ddl-auto` is set to `validate` (does not create/alter tables).
- On startup Flyway runs pending `V*.sql` scripts.
- Seed users/products still come from `DataSeeder`.

Default connection (override via env / `.env`):

```
DB_URL=jdbc:postgresql://localhost:5432/ecommerce_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Run from IntelliJ

1. Open the `backend` folder (or the `EcommerceApplication` main class).
2. Make sure `backend/.env` exists with your real Postgres password (`DB_PASSWORD=...`).
3. Set **Working directory** of the Run Configuration to the `backend` module folder.
4. Run `EcommerceApplication`.

The app auto-loads `.env` (so you don't need to paste env vars into IntelliJ).  
If login to Postgres still fails, your `DB_PASSWORD` in `.env` does not match the Postgres `postgres` user password.

API base: `http://localhost:8080/api`

### Seed accounts

| Role | Email | Password |
|------|-------|----------|
| ADMIN | admin@shop.com | Admin@123 |
| CUSTOMER | customer@shop.com | Customer@123 |

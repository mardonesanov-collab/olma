# MenuBot SaaS — Multi-Tenant Menu & Orders

Production-ready foundation for Telegram Mini App restaurant management.

## Stack

- **Backend:** Spring Boot 3.2, PostgreSQL 16, Flyway, JPA
- **Frontend:** React 18 + Vite + TypeScript
- **Bot:** Telegram Long Polling + WebApp `initData` validation (SHA-256)

## Quick start

```bash
# 1. PostgreSQL
docker compose up -d

# 2. Backend
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home"
./run.sh

# 3. Ngrok (Telegram WebApp)
./start-ngrok.sh
# Update app.base-url in application.properties

# 4. Frontend rebuild (included in run.sh)
cd frontend && npm run build
```

## Roles

| Role | Description |
|------|-------------|
| `SUPER_ADMIN` | `bot.admin.id` — approves restaurants via Telegram inline buttons |
| `VENDOR` | Restaurant owner — `/api/v1/vendor/**` |
| `CLIENT` | Customer — `/api/v1/client/**` |

## API (authenticated)

All `/api/v1/**` requests (except `/public/**`) require header:

```
X-Telegram-Init-Data: <Telegram.WebApp.initData>
```

### Public (no auth)

- `GET /api/v1/public/restaurants/{id}`
- `GET /api/v1/public/restaurants/{id}/menu?q=`
- `GET /api/v1/public/parse-startapp?startapp=123_table5`

### Vendor

- `POST /api/v1/vendor/restaurants` — register (PENDING)
- `GET /api/v1/vendor/restaurants/{id}/orders`
- `PATCH /api/v1/vendor/restaurants/{id}/orders/{orderId}/status`
- `GET /api/v1/vendor/restaurants/{id}/analytics`

### Client

- `POST /api/v1/client/restaurants/{id}/orders`
- `POST /api/v1/client/restaurants/{id}/call-waiter`
- `POST /api/v1/client/restaurants/{id}/reviews`

## Deep link format

```
https://t.me/MenuPointBot/app?startapp={restaurantId}_table{number}
```

## Database

Schema: `src/main/resources/db/migration/V1__saas_schema.sql`

Tables: `users`, `restaurants`, `categories`, `products`, `orders`, `order_items`, `reviews`

# PayPal Clone — Backend API

A production-ready PayPal-like payment backend built with Java 17, Spring Boot 3.5, MySQL, and Redis.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.4 |
| Database | MySQL 8.0 |
| Cache / Session | Redis 7.2 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Mapping | MapStruct 1.5.5 |
| Boilerplate | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## Getting Started

### Prerequisites

- Java 17+
- Docker + Docker Compose
- Maven (or use the included `mvnw` wrapper)

### 1. Start MySQL and Redis via Docker

```bash
docker-compose up -d
```

This starts:
- MySQL 8.0 on port `3306` — database `paypal_db`, root password `root`
- Redis 7.2 on port `6379`

Data is persisted via Docker volumes across restarts.

### 2. Run the application

```bash
./mvnw spring-boot:run
```

### 3. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Configuration

All config lives in `src/main/resources/application.yml`.

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | App port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/paypal_db` | MySQL URL |
| `spring.datasource.username` | `root` | MySQL username |
| `spring.datasource.password` | `root` | MySQL password |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `app.jwt.secret` | *(256-bit hex)* | JWT signing secret — change in production |
| `app.jwt.access-token-expiry-ms` | `900000` | Access token TTL (15 min) |
| `app.jwt.refresh-token-expiry-ms` | `604800000` | Refresh token TTL (7 days) |
| `app.rate-limit.capacity` | `20` | Max requests per window |
| `app.rate-limit.refill-seconds` | `60` | Rate limit window in seconds |
| `app.admin.emails` | `admin@paypal.com,superadmin@paypal.com` | Comma-separated admin emails |
| `app.admin.default-password` | `Admin@1234` | Default password for seeded admins |

---

## Project Structure

```
src/main/java/com/dailyCode/paypal_user/
│
├── PaypalUserApplication.java
│
├── common/                        # Shared across all modules
│   ├── dto/
│   │   ├── ApiResponse.java       # Standard API response wrapper
│   │   └── PageResponse.java      # Paginated response wrapper
│   ├── entity/
│   │   └── BaseEntity.java        # id + createdAt + updatedAt
│   ├── enums/
│   │   └── ApiStatus.java         # SUCCESS | ERROR | VALIDATION_ERROR
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BadRequestException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── UnauthorizedException.java
│   │   └── InsufficientFundsException.java
│   └── ratelimit/
│       ├── RateLimitFilter.java
│       └── RateLimitService.java
│
├── config/
│   ├── SecurityConfig.java        # Spring Security filter chain
│   ├── RedisConfig.java           # StringRedisTemplate bean
│   ├── SwaggerConfig.java         # OpenAPI + Bearer auth
│   └── DataSeeder.java            # Seeds admin users on startup
│
├── auth/                          # Authentication module
│   ├── controller/AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── CustomUserDetailsService.java
│   ├── filter/JwtAuthFilter.java
│   ├── util/JwtUtil.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RefreshTokenRequest.java
│       └── AuthResponse.java
│
├── user/                          # User module
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── repository/UserRepository.java
│   ├── entity/User.java
│   ├── enums/
│   │   ├── UserRole.java          # ROLE_USER | ROLE_ADMIN
│   │   └── UserStatus.java        # ACTIVE | INACTIVE | SUSPENDED | PENDING_VERIFICATION
│   ├── event/UserRegisteredEvent.java
│   ├── mapper/UserMapper.java
│   └── dto/
│       ├── RegisterRequest.java
│       ├── UpdateProfileRequest.java
│       └── UserResponse.java
│
├── wallet/                        # Wallet module
│   ├── controller/WalletController.java
│   ├── service/WalletService.java
│   ├── repository/WalletRepository.java
│   ├── entity/Wallet.java
│   ├── enums/
│   │   ├── Currency.java          # USD | EUR | GBP | INR | CAD | AUD
│   │   └── WalletStatus.java      # ACTIVE | SUSPENDED | CLOSED
│   ├── listener/WalletEventListener.java
│   ├── mapper/WalletMapper.java
│   └── dto/
│       ├── TopUpRequest.java
│       └── WalletResponse.java
│
└── transaction/                   # Transaction module
    ├── controller/TransactionController.java
    ├── service/TransactionService.java
    ├── repository/TransactionRepository.java
    ├── entity/Transaction.java
    ├── enums/
    │   ├── TransactionType.java   # TRANSFER | TOP_UP | REFUND
    │   └── TransactionStatus.java # PENDING | COMPLETED | FAILED | REFUNDED
    ├── mapper/TransactionMapper.java
    └── dto/
        ├── TransferRequest.java
        ├── TransactionFilterRequest.java
        └── TransactionResponse.java
```

---

## API Endpoints

All responses follow this structure:
```json
{
  "status": "SUCCESS",
  "message": "...",
  "data": { },
  "timestamp": "2026-03-15T10:00:00"
}
```

### Auth — `/api/v1/auth` (public)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/register` | Register a new user |
| POST | `/login` | Login, returns access + refresh tokens |
| POST | `/refresh` | Refresh access token |
| POST | `/logout` | Invalidate refresh token |

### User — `/api/v1/users` (authenticated)

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/me` | Any | Get own profile |
| PUT | `/me` | Any | Update own profile |
| GET | `/{id}` | Admin | Get any user by ID |
| PUT | `/{id}/status` | Admin | Update user status |

### Wallet — `/api/v1/wallet` (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/` | Get own wallet and balance |
| POST | `/top-up` | Add funds to wallet |

### Transactions — `/api/v1/transactions` (authenticated)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/transfer` | Send money to another user |
| POST | `/{id}/refund` | Refund a completed transaction |
| GET | `/` | Transaction history (paginated, filterable) |
| GET | `/{id}` | Get transaction by ID |
| GET | `/reference/{referenceId}` | Get transaction by reference ID |

### Actuator (public)

| Endpoint | Description |
|---|---|
| `/actuator/health` | App + DB + Redis health |
| `/actuator/info` | App name and version |
| `/actuator/metrics` | JVM and HTTP metrics (authenticated) |

---

## Security

- JWT access tokens — 15 min expiry, stateless
- Refresh tokens — 7 day expiry, stored in Redis, rotated on every refresh
- Logout invalidates the refresh token in Redis immediately
- Rate limiting — 20 requests per 60 seconds per user (keyed by email for authenticated, IP for anonymous)
- Passwords hashed with BCrypt
- Role-based access control via `@PreAuthorize`

---

## Admin Management

Admins are managed via `application.properties` — no public API endpoint for promotion/demotion.

```properties
app.admin.emails=admin@paypal.com,superadmin@paypal.com
```

On every startup, `DataSeeder`:
- Creates any admin in the list that doesn't exist yet
- Promotes existing users in the list to `ROLE_ADMIN`
- Demotes any `ROLE_ADMIN` user not in the list back to `ROLE_USER`

For immediate one-off changes, update directly in MySQL:
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'john@example.com';
```

---

## Database Schema

Three main tables auto-created by Hibernate (`ddl-auto=update`):

```
users
  id, first_name, last_name, email, password, phone_number,
  role, status, email_verified, created_at, updated_at

wallets
  id, user_id (FK), balance, currency, status, version,
  created_at, updated_at

transactions
  id, reference_id, sender_id (FK), receiver_id (FK),
  amount, currency, type, status, note, failure_reason,
  original_transaction_id, created_at, updated_at
```

---

## What's Coming Next

- Password change endpoint
- Email verification flow
- Admin dashboard endpoints
- Flyway database migrations
- Frontend integration

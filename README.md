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
- MySQL 8.0 on port `3306` — database `paypal_db`
- Redis 7.2 on port `6379`

Data is persisted via Docker volumes across restarts.

### 2. Configure local credentials

Copy the local config template and fill in your values:

```
src/main/resources/application-local.yml   ← gitignored, never committed
```

Set the following in that file:
- MySQL username and password
- Redis password (if applicable)
- JWT secret key (256-bit hex string)
- Admin email(s) and default password

The `local` profile is active by default — `application.yml` loads first, then `application-local.yml` overrides the sensitive values.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

### 4. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Configuration

All non-sensitive config lives in `src/main/resources/application.yml`.
All credentials and secrets go in `src/main/resources/application-local.yml` — this file is gitignored and never committed.

| Property | Description |
|---|---|
| `server.port` | App port (default `8080`) |
| `spring.datasource.url` | MySQL connection URL |
| `spring.datasource.username` | MySQL username — set in `application-local.yml` |
| `spring.datasource.password` | MySQL password — set in `application-local.yml` |
| `spring.data.redis.host` | Redis host (default `localhost`) |
| `spring.data.redis.port` | Redis port (default `6379`) |
| `app.jwt.secret` | JWT signing secret (256-bit) — set in `application-local.yml` |
| `app.jwt.access-token-expiry-ms` | Access token TTL (default 15 min) |
| `app.jwt.refresh-token-expiry-ms` | Refresh token TTL (default 7 days) |
| `app.rate-limit.capacity` | Max requests per window (default `20`) |
| `app.rate-limit.refill-seconds` | Rate limit window in seconds (default `60`) |
| `app.admin.emails` | Comma-separated admin emails — set in `application-local.yml` |
| `app.admin.default-password` | Default password for seeded admins — set in `application-local.yml` |

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
  "data": {},
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

Admins are managed via `application-local.yml` — no public API endpoint for promotion or demotion.

```yaml
app:
  admin:
    emails: # comma-separated list of admin emails
    default-password: # default password for seeded admin accounts
```

On every startup, `DataSeeder`:
- Creates any admin in the list that does not exist yet
- Promotes existing users in the list to `ROLE_ADMIN`
- Demotes any `ROLE_ADMIN` user not in the list back to `ROLE_USER`

For immediate one-off changes, update directly in MySQL:
```sql
UPDATE users SET role = 'ROLE_ADMIN' WHERE email = 'your-email@example.com';
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

## Postman Testing

Import both files from the `postman/` folder into Postman:
- `PayPal-Clone.postman_collection.json`
- `PayPal-Clone.postman_environment.json`

Select the `PayPal Clone - Local` environment from the top-right dropdown. Tokens are saved and reused automatically by the built-in test scripts — no manual copy-pasting needed.

### Testing Flow — Run in this order

| Step | Folder | Request | What it does |
|---|---|---|---|
| 1 | Health & Info | Health Check | Confirm app is running |
| 2 | Auth | Register | Creates a new user, saves `userId` |
| 3 | Auth | Login | Saves `accessToken` + `refreshToken` automatically |
| 4 | User | Get My Profile | Fetches profile using saved token |
| 5 | User | Update My Profile | Updates first name and phone number |
| 6 | Wallet | Get My Wallet | Confirms wallet was auto-created with balance 0.00 |
| 7 | Wallet | Top Up Wallet | Adds funds to wallet |
| 8 | Auth | Register | Change body email — creates a second user for transfer testing |
| 9 | Transactions | Transfer Money | Sends money to second user, saves `transactionId` + `referenceId` |
| 10 | Transactions | Get Transaction History | Lists all transactions with pagination |
| 11 | Transactions | Get Transaction by ID | Fetches the transfer using saved `transactionId` |
| 12 | Transactions | Get Transaction by Reference ID | Fetches using saved `referenceId` |
| 13 | Transactions | Refund Transaction | Refunds the transfer, money returns to sender |
| 14 | Auth | Refresh Token | Rotates tokens using saved `refreshToken` |
| 15 | Auth | Login as Admin | Logs in with admin credentials, saves `adminToken` |
| 16 | User | Get User by ID (Admin) | Uses `adminToken` + saved `userId` to look up a user |
| 17 | User | Update User Status (Admin) | Updates a user's account status |
| 18 | Auth | Logout | Invalidates refresh token, clears tokens from environment |

### Environment Variables (auto-managed by scripts)

| Variable | Set by | Used by |
|---|---|---|
| `baseUrl` | You (default: `http://localhost:8080`) | All requests |
| `accessToken` | Login script | All authenticated requests |
| `refreshToken` | Login script | Refresh Token request |
| `adminToken` | Login as Admin script | Admin requests |
| `userId` | Register / Get My Profile script | Get User by ID, Update Status |
| `transactionId` | Transfer Money script | Get by ID, Refund |
| `referenceId` | Transfer Money script | Get by Reference ID |

---

## What's Coming Next

- Password change endpoint
- Email verification flow
- Admin dashboard endpoints
- Flyway database migrations
- Frontend integration

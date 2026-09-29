# backend-developer-as-final-84060-aswini
Final Project Assignment - This repository contains the complete final project code and documentation.


# Resource Booking API

A secure REST API for booking shared resources like meeting rooms, vehicles and equipment. People log in, browse what's available and make reservations. Admins manage everything.

It's built with Java 17, Spring Boot 3, Spring Security (JWT), JPA/Hibernate and MySQL. PostgreSQL works too.

---

## What it does

- **Login with JWT.** `POST /auth/login` gives you a token. Send it as `Authorization: Bearer <token>` on every other request.
- **Two roles.**
  - **ADMIN** can do everything: create, edit and delete resources and reservations, and see all bookings.
  - **USER** can browse resources (read-only), create reservations, see only their own, and cancel their own.
- **Your identity comes from the token, not the request.** If a user sends someone else's `userId` in a booking, it's ignored. The booking is always theirs.
- **Reservations** have a status (`PENDING`, `CONFIRMED`, `CANCELLED`), start and end times, and a decimal price. Double-booking the same resource for overlapping times is rejected.
- **Filtering, paging and sorting** on the reservation list.
- **Clear errors.** Every failure comes back as the same JSON shape with the right status code (400, 401, 403, 404, 409).

---

## Getting started

### What you need

- JDK 17 or newer
- Maven 3.8 or newer
- MySQL 8 running locally (or PostgreSQL, see below)

### 1. Check the database settings

Open `src/main/resources/application.yml` and make sure the datasource matches your MySQL:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/booking?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: your_mysql_password
```

You don't need to create the `booking` database yourself. The `createDatabaseIfNotExist=true` part does it on first start, and Hibernate creates the tables.

### 2. Run it

```bash
mvn spring-boot:run
```

When you see `Started BookingApplication`, it's ready. By default it listens on **http://localhost:8888** (change it with the `SERVER_PORT` variable, or edit `application.yml`).

### 3. Try it in Swagger

Open **http://localhost:8888/swagger-ui.html**, then:

1. Run `POST /auth/login` with one of the demo accounts below.
2. Copy the `token` from the response.
3. Click **Authorize** at the top, paste the token, and you're in.

### Demo accounts

These are created automatically on first start:

| Username | Password   | Role  |
|----------|------------|-------|
| `admin`  | `admin123` | ADMIN |
| `user1`  | `user123`  | USER  |
| `user2`  | `user123`  | USER  |

A few sample resources are added too (a room, a car, a projector, and one that's unavailable).

---

## Configuration

Every setting can be overridden with an environment variable. The defaults are fine for local development.

| Variable            | Purpose                                              |
|---------------------|------------------------------------------------------|
| `SERVER_PORT`       | Port the app listens on                              |
| `DB_URL`            | JDBC connection URL                                  |
| `DB_USERNAME`       | Database user                                        |
| `DB_PASSWORD`       | Database password                                    |
| `JWT_SECRET`        | Secret used to sign tokens (at least 32 characters)  |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds (default 1 hour)      |
| `DDL_AUTO`          | Hibernate schema mode (`update` by default)          |
| `SEED_ENABLED`      | Set to `false` to skip the demo users and resources  |

Setting one in PowerShell:

```powershell
$env:JWT_SECRET="a-long-random-string-of-at-least-32-characters"
mvn spring-boot:run
```

Environment variables win over `application.yml`. If a setting seems ignored, check that an old variable isn't still set in your terminal.

**Using PostgreSQL instead?** Set `DB_URL=jdbc:postgresql://localhost:5432/booking` along with the matching username and password. A `docker-compose.yml` is included if you want a quick PostgreSQL container.

---

## Who can do what

| Endpoint | ADMIN | USER |
|----------|:-----:|:----:|
| `POST /auth/login` | public | public |
| `GET /resources`, `GET /resources/{id}` | yes | yes |
| `POST`, `PUT`, `DELETE /resources` | yes | no (403) |
| `GET /reservations` | all bookings | own only |
| `GET /reservations/{id}` | any | own only |
| `POST /reservations` | yes, can book for others | yes, always for themselves |
| `PATCH /reservations/{id}/cancel` | any | own only |
| `PUT /reservations/{id}` | yes | no (403) |
| `DELETE /reservations/{id}` | yes | no (403) |

### Listing reservations

```
GET /reservations?status=CONFIRMED&minPrice=20&maxPrice=100&page=0&size=10&sortBy=price&direction=desc
```

| Parameter | Default | Notes |
|-----------|---------|-------|
| `status` | none | `PENDING`, `CONFIRMED` or `CANCELLED` |
| `minPrice`, `maxPrice` | none | Inclusive. `minPrice` can't be higher than `maxPrice` |
| `page` | `0` | Starts at zero |
| `size` | `10` | Between 1 and 100 |
| `sortBy` | `id` | `id`, `price`, `startTime`, `endTime`, `status` or `createdAt` |
| `direction` | `asc` | `asc` or `desc` |

The response looks like:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### Quick example

```bash
# Log in
TOKEN=$(curl -s -X POST localhost:8888/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user1","password":"user123"}' | jq -r .token)

# Book a resource
curl -X POST localhost:8888/reservations \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"resourceId":1,"startTime":"2026-12-01T10:00:00","endTime":"2026-12-01T12:00:00","price":150.00}'

# See my confirmed bookings, cheapest first
curl -H "Authorization: Bearer $TOKEN" \
  "localhost:8888/reservations?status=CONFIRMED&sortBy=price"
```

---

## Errors

Every error uses the same shape:

```json
{
  "timestamp": "2026-09-29T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/reservations",
  "details": { "price": "price must not be negative" }
}
```

| Status | Meaning |
|--------|---------|
| 400 | Invalid input: missing fields, negative price, end before start, bad status or sort value |
| 401 | Missing, invalid or expired token, or wrong login credentials |
| 403 | Logged in but not allowed (wrong role, or someone else's reservation) |
| 404 | Resource, reservation or user not found |
| 409 | Overlapping booking, unavailable resource, or deleting a resource that still has reservations |

---

## Running the tests

```bash
mvn test
```

The tests use an in-memory H2 database, so they don't touch your MySQL. They cover login and token handling, role permissions on every endpoint, reservation ownership, validation, filtering, paging, sorting and the JWT service itself.

---

## Project structure

```
src/main/java/com/example/booking
├── config       Security setup, Swagger config, demo data seeder
├── controller   REST endpoints
├── dto          Request and response objects
├── entity       User, Resource, Reservation and their enums
├── exception    Custom exceptions and the global error handler
├── repository   Database access and dynamic reservation filters
├── security     JWT creation and validation, request filter
└── service      Business rules (ownership, overlaps, validation)
```

---

## Design notes

- **Two layers of protection.** URL rules in `SecurityConfig` check roles, and the service layer checks ownership again for single reservations.
- **Users can cancel but not edit.** A USER can create, view and cancel their own reservations. Changing prices or statuses and hard deletes are admin-only.
- **Price is supplied by the caller** and stored as `DECIMAL(10,2)`. If you'd rather calculate it from a rate on the resource, `ReservationService.create` is the place to do it.
- **Other people's reservations return 403**, not 404. Switch to 404 if you'd rather not reveal that an ID exists.
- **Before deploying anywhere real:** change the demo passwords (or set `SEED_ENABLED=false`), set your own `JWT_SECRET`, and switch `DDL_AUTO` to `validate`.

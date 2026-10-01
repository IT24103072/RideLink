# RideLink 🚗

A production-grade, microservices-based ride-hailing platform built with **Spring Boot 3.5** and **Java 17**.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT (Postman / App)                 │
└──────────────┬───────────┬──────────────┬───────────────┘
               │           │              │
       ┌───────▼──┐  ┌─────▼──────┐  ┌───▼──────────┐
       │ Account   │  │  Driver &  │  │ Fare &       │
       │ Service   │  │  Vehicle   │  │ Payment      │
       │ :8081     │  │ Service    │  │ Service      │
       └───────────┘  │ :8082      │  │ :8084        │
                      └─────▲──────┘  └───▲──────────┘
                            │ REST         │ REST + RabbitMQ
                      ┌─────┴──────────────┴─────────┐
                      │   Ride Management Service     │
                      │          :8083                │
                      └───────────────────────────────┘
                                    │
                      ┌─────────────▼──────────────┐
                      │     MongoDB + RabbitMQ      │
                      │   :27017       :5672        │
                      └────────────────────────────┘
```

## Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| `account-service` | 8081 | `account_db` | User registration, login, JWT auth |
| `driver-vehicle-service` | 8082 | `driver_db` | Driver profiles, vehicles, availability, location |
| `ride-management-service` | 8083 | `ride_db` | Ride lifecycle, driver dispatch, state machine |
| `fare-payment-service` | 8084 | `fare_db` | Fare estimation, calculation, payment records |

## Quick Start (Local Development)

### Prerequisites
- Docker Desktop
- Java 17+
- Maven 3.9+

### 1. Start Infrastructure

```bash
cd infra
docker compose up -d mongodb rabbitmq
```

> MongoDB on port `27017`, RabbitMQ Management UI at http://localhost:15672 (guest/guest)

### 2. Run Each Service

Open 4 terminal windows:

```bash
# Terminal 1 – Account Service
cd account-service
mvn spring-boot:run

# Terminal 2 – Driver & Vehicle Service
cd driver-vehicle-service
mvn spring-boot:run

# Terminal 3 – Fare & Payment Service
cd fare-payment-service
mvn spring-boot:run

# Terminal 4 – Ride Management Service
cd ride-management-service
mvn spring-boot:run
```

### 3. Swagger UIs

| Service | URL |
|---------|-----|
| Account | http://localhost:8081/swagger-ui.html |
| Driver & Vehicle | http://localhost:8082/swagger-ui.html |
| Ride Management | http://localhost:8083/swagger-ui.html |
| Fare & Payment | http://localhost:8084/swagger-ui.html |

## Running with Docker Compose (All Services)

```bash
# Build and start all services
cd infra
docker compose up --build
```

## API Workflow

### Complete End-to-End Flow

```
1.  POST /api/auth/register          → Register passenger account
2.  POST /api/auth/register          → Register driver account
3.  POST /api/auth/login             → Get JWT token (passenger)
4.  POST /api/drivers/profile        → Driver registers vehicle (driver JWT)
5.  GET  /api/drivers/available      → Verify driver is available
6.  POST /api/fares/estimate         → Estimate fare before booking
7.  POST /api/rides                  → Passenger requests ride (auto-dispatches driver)
8.  POST /api/rides/{id}/accept      → Driver accepts ride (driver JWT)
9.  POST /api/rides/{id}/start       → Driver picks up passenger
10. POST /api/rides/{id}/complete    → Driver completes trip (final fare calculated)
11. GET  /api/fares/ride/{rideId}    → Retrieve fare breakdown
```

## Authentication

All protected endpoints require a **Bearer JWT** in the `Authorization` header:

```
Authorization: Bearer <token>
```

JWTs are issued by `account-service` on registration and login. The **same `JWT_SECRET`** must be configured across all services.

## Environment Variables

Copy `.env.example` in each service directory and configure:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | **Shared secret** – must be identical across all services |
| `MONGODB_URI` | MongoDB connection string |
| `RABBITMQ_HOST` | RabbitMQ host |
| `SERVER_PORT` | HTTP port override |

## Fare Model (LKR)

| Vehicle | Base Fare | Per Km | Per Minute | Tax |
|---------|-----------|--------|------------|-----|
| SEDAN | 100 | 45 | 3 | 18% |
| SUV | 130 | 60 | 4 | 18% |
| VAN | 150 | 70 | 5 | 18% |
| MOTORCYCLE | 60 | 30 | 2 | 18% |

## Running Tests

```bash
# From any service directory
mvn test

# Or test all services
for service in account-service driver-vehicle-service ride-management-service fare-payment-service; do
  echo "Testing $service..."
  cd $service && mvn test && cd ..
done
```

## Postman Collection

Import `postman/RideLink_API_Collection.json` into Postman.

The collection includes:
- Pre-configured base URLs for all 4 services
- Auto-save of `JWT_TOKEN` and `RIDE_ID` via test scripts
- All endpoints in the correct execution order
- Both passenger and driver workflow examples

## Project Structure

```
RideLink/
├── account-service/          # User accounts & JWT auth
│   ├── src/main/java/com/ridelink/account/
│   │   ├── controller/       # AuthController, UserController
│   │   ├── service/          # AuthService, UserService
│   │   ├── model/            # User, Role, AccountStatus
│   │   ├── security/         # JwtUtil, JwtAuthFilter
│   │   └── config/           # SecurityConfig, OpenApiConfig
│   └── Dockerfile
│
├── driver-vehicle-service/   # Driver profiles & dispatch
│   ├── src/main/java/com/ridelink/driver/
│   │   ├── controller/       # DriverController
│   │   ├── service/          # DriverService
│   │   ├── model/            # Driver, Vehicle, Location, DriverStatus, VehicleType
│   │   └── repository/       # DriverRepository
│   └── Dockerfile
│
├── ride-management-service/  # Ride lifecycle & state machine
│   ├── src/main/java/com/ridelink/ride/
│   │   ├── controller/       # RideController
│   │   ├── service/          # RideService
│   │   ├── model/            # Ride, RideStatus, LocationPoint
│   │   ├── config/           # RabbitConfig, SecurityConfig, AppConfig
│   │   └── repository/       # RideRepository
│   └── Dockerfile
│
├── fare-payment-service/     # Fare calculation & payment records
│   ├── src/main/java/com/ridelink/fare/
│   │   ├── controller/       # FareController
│   │   ├── service/          # FareService
│   │   ├── messaging/        # RideEventListener
│   │   ├── model/            # FareRecord
│   │   └── config/           # RabbitConfig, SecurityConfig
│   └── Dockerfile
│
├── infra/
│   └── docker-compose.yml    # Full stack: MongoDB, RabbitMQ, all 4 services
│
└── postman/
    └── RideLink_API_Collection.json
```

## Roles & Authorization

| Role | Capabilities |
|------|-------------|
| `PASSENGER` | Register, login, request rides, view own rides, cancel |
| `DRIVER` | Register driver profile, accept/start/complete rides, update location/availability |
| `ADMIN` | View all rides, all fare records, manage users |

## Asynchronous Events (RabbitMQ)

| Exchange | Routing Key | Publisher | Consumer |
|----------|-------------|-----------|---------|
| `ride.exchange` | `ride.requested` | ride-management-service | (extensible) |
| `ride.exchange` | `ride.completed` | ride-management-service | fare-payment-service |

The fare-payment-service listens on `fare.ride.completed.queue` and marks fare records as `COLLECTED` when a ride completes.

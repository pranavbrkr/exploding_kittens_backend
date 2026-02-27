# Exploding Kittens Backend

A microservices-based backend for the Exploding Kittens multiplayer card game built with Spring Boot.

## Features

- **Player Management**: Player registration and authentication
- **Lobby System**: Create and manage game lobbies
- **Game Engine**: Complete game logic implementation
- **Real-time Communication**: WebSocket-based live updates
- **Action Notifications**: Real-time game event broadcasting
- **Feral Cat Combinations**: Advanced stealing mechanics

## Technologies Used

- Spring Boot 3
- Spring WebSocket (STOMP)
- Spring Web (REST APIs)
- Maven
- Java 17

## Architecture

Two microservices:
- **Session Service** (Port 8080): Player management (auth, registration) and lobby management (create/join lobby, start game)
- **Game Service** (Port 8082): Core game logic

## How to Run

### Prerequisites: Postgres

Both **session-service** (users) and **game-service** (games, game_participants) use PostgreSQL. Start Postgres first (from the **backend repo root**):

```bash
cd exploding_kittens_backend
docker-compose up -d postgres
```

This creates database `explodingkittens` with user `kitten` / password `kitten`. If you use a different Postgres, set env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.

**Nuke the DB (fresh start, no stale data):** From `exploding_kittens_backend` run `docker-compose down -v` then `docker-compose up -d postgres`. Or run `./scripts/nuke-db.ps1` (Windows) / `./scripts/nuke-db.sh` (Mac/Linux). Then restart session-service and game-service so tables are recreated.

**If you already have a `users` table** (e.g. from before email verification was added) and session-service fails on startup with "email_verified contains null values" or at register with "email_verified does not exist", run this migration once:

```bash
# From backend root, with Postgres running:
psql -U kitten -d explodingkittens -f session-service/src/main/resources/db/migration/V1__add_email_verification_columns.sql
```

(On Windows you may use `psql -U kitten -d explodingkittens` and paste the contents of that file, or run it from your DB client.)

### Start all services

1. **Build the project**
   ```bash
   mvn clean install
   ```

2. **Start all services**
   ```bash
   # Start Session Service (requires Postgres running; see above)
   cd session-service
   mvn spring-boot:run
   
   # Start Game Service (new terminal)
   cd game-service
   mvn spring-boot:run
   ```
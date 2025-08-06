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

Three microservices:
- **Player Service** (Port 8080): Player management
- **Lobby Service** (Port 8081): Lobby management
- **Game Service** (Port 8082): Core game logic

## How to Run

1. **Build the project**
   ```bash
   mvn clean install
   ```

2. **Start all services**
   ```bash
   # Start Player Service
   cd player-service
   mvn spring-boot:run
   
   # Start Lobby Service (new terminal)
   cd lobby-service
   mvn spring-boot:run
   
   # Start Game Service (new terminal)
   cd game-service
   mvn spring-boot:run
   ```
# Multi-stage build for better performance and smaller image size
FROM openjdk:24-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy pom files first for better layer caching
COPY pom.xml .
COPY common/pom.xml common/
COPY player-service/pom.xml player-service/
COPY lobby-service/pom.xml lobby-service/
COPY game-service/pom.xml game-service/

# Download dependencies (this layer will be cached if pom files don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY . .

# Build the project
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:24-jre-slim

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy built JAR files from builder stage
COPY --from=builder /app/player-service/target/*.jar /app/player-service.jar
COPY --from=builder /app/lobby-service/target/*.jar /app/lobby-service.jar
COPY --from=builder /app/game-service/target/*.jar /app/game-service.jar

# Create a startup script that runs all three services
RUN echo '#!/bin/bash\n\
echo "Starting Exploding Kittens Backend Services..."\n\
echo ""\n\
echo "Starting Player Service on port 8080..."\n\
java -jar /app/player-service.jar &\n\
PLAYER_PID=$!\n\
\n\
# Wait a bit for Player Service to start\n\
sleep 5\n\
\n\
echo "Starting Lobby Service on port 8081..."\n\
java -jar /app/lobby-service.jar &\n\
LOBBY_PID=$!\n\
\n\
# Wait a bit for Lobby Service to start\n\
sleep 5\n\
\n\
echo "Starting Game Service on port 8082..."\n\
java -jar /app/game-service.jar &\n\
GAME_PID=$!\n\
\n\
echo ""\n\
echo "All services started!"\n\
echo "Player Service PID: $PLAYER_PID"\n\
echo "Lobby Service PID: $LOBBY_PID"\n\
echo "Game Service PID: $GAME_PID"\n\
echo ""\n\
echo "Services are running on:"\n\
echo "- Player Service: http://localhost:8080"\n\
echo "- Lobby Service: http://localhost:8081"\n\
echo "- Game Service: http://localhost:8082"\n\
echo ""\n\
echo "WebSocket endpoints:"\n\
echo "- Lobby WebSocket: ws://localhost:8081/ws/lobby"\n\
echo "- Game WebSocket: ws://localhost:8082/ws-game"\n\
echo ""\n\
echo "Press Ctrl+C to stop all services"\n\
\n\
# Function to handle shutdown\n\
cleanup() {\n\
    echo "Shutting down services..."\n\
    kill $PLAYER_PID $LOBBY_PID $GAME_PID 2>/dev/null\n\
    wait $PLAYER_PID $LOBBY_PID $GAME_PID 2>/dev/null\n\
    echo "All services stopped."\n\
    exit 0\n\
}\n\
\n\
# Set up signal handlers\n\
trap cleanup SIGINT SIGTERM\n\
\n\
# Wait for all background processes\n\
wait' > /app/start-services.sh && chmod +x /app/start-services.sh

# Expose all three ports
EXPOSE 8080 8081 8082

# Set the default command to run the startup script
CMD ["/app/start-services.sh"]

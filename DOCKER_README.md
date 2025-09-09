# 🐳 Docker Setup for Exploding Kittens Backend

This document provides instructions for running the Exploding Kittens backend using Docker.

## 📋 Prerequisites

- Docker installed on your system
- Docker Compose (optional, for easier management)

## 🚀 Quick Start

### Option 1: Using Docker directly

1. **Build the Docker image:**
   ```bash
   docker build -t exploding-kittens-backend .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8080:8080 -p 8081:8081 -p 8082:8082 exploding-kittens-backend
   ```

### Option 2: Using Docker Compose (Recommended)

1. **Build and run all services:**
   ```bash
   docker-compose up --build
   ```

2. **Run in detached mode:**
   ```bash
   docker-compose up -d --build
   ```

3. **Stop the services:**
   ```bash
   docker-compose down
   ```

## 🌐 Service Endpoints

Once the container is running, the following services will be available:

> **✅ Frontend Ready**: All services are configured with CORS to accept requests from any frontend origin

### REST APIs
- **Player Service**: http://localhost:8080
  - `POST /api/player/register` - Register new player
  - `GET /api/player/{id}` - Get player details
  - `GET /api/player/health` - Health check

- **Lobby Service**: http://localhost:8081
  - `GET /api/lobby/{lobbyId}` - Get lobby details
  - `POST /api/lobby/create` - Create new lobby
  - `POST /api/lobby/join` - Join existing lobby
  - `POST /api/lobby/start/{lobbyId}` - Start game
  - `GET /api/lobby/health` - Health check

- **Game Service**: http://localhost:8082
  - `GET /api/game/{lobbyId}` - Get game state
  - `POST /api/game/play/{lobbyId}` - Play a card
  - `POST /api/game/draw/{lobbyId}` - Draw a card
  - `GET /api/game/health` - Health check
  - And many more game endpoints...

### WebSocket Endpoints
- **Lobby WebSocket**: ws://localhost:8081/ws/lobby
- **Game WebSocket**: ws://localhost:8082/ws-game

## 🎯 Frontend Integration

### CORS Configuration
All services are configured to accept requests from any frontend origin:
- **Player Service**: CORS enabled for all origins
- **Lobby Service**: CORS enabled for all origins  
- **Game Service**: CORS enabled for all origins

### Frontend Connection Examples

#### JavaScript/React Frontend
```javascript
// REST API calls
const playerResponse = await fetch('http://localhost:8080/api/player/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ name: 'PlayerName' })
});

// WebSocket connections
const gameSocket = new WebSocket('ws://localhost:8082/ws-game');
const lobbySocket = new WebSocket('ws://localhost:8081/ws/lobby');
```

#### Environment Variables for Frontend
```env
REACT_APP_PLAYER_SERVICE_URL=http://localhost:8080
REACT_APP_LOBBY_SERVICE_URL=http://localhost:8081
REACT_APP_GAME_SERVICE_URL=http://localhost:8082
REACT_APP_LOBBY_WS_URL=ws://localhost:8081/ws/lobby
REACT_APP_GAME_WS_URL=ws://localhost:8082/ws-game
```

## 🔧 Docker Commands

### Build Commands
```bash
# Build the image
docker build -t exploding-kittens-backend .

# Build without cache
docker build --no-cache -t exploding-kittens-backend .
```

### Run Commands
```bash
# Run with port mapping
docker run -p 8080:8080 -p 8081:8081 -p 8082:8082 exploding-kittens-backend

# Run in detached mode
docker run -d -p 8080:8080 -p 8081:8081 -p 8082:8082 --name exploding-kittens exploding-kittens-backend

# Run with environment variables
docker run -p 8080:8080 -p 8081:8081 -p 8082:8082 -e SPRING_PROFILES_ACTIVE=docker exploding-kittens-backend
```

### Management Commands
```bash
# View running containers
docker ps

# View logs
docker logs exploding-kittens

# Follow logs in real-time
docker logs -f exploding-kittens

# Stop container
docker stop exploding-kittens

# Remove container
docker rm exploding-kittens

# Remove image
docker rmi exploding-kittens-backend
```

## 🏗️ Docker Compose Commands

```bash
# Start services
docker-compose up

# Start in background
docker-compose up -d

# Rebuild and start
docker-compose up --build

# Stop services
docker-compose down

# View logs
docker-compose logs

# Follow logs
docker-compose logs -f

# Scale services (if needed)
docker-compose up --scale exploding-kittens-backend=2
```

## 🐛 Troubleshooting

### Common Issues

1. **Port already in use:**
   ```bash
   # Check what's using the ports
   netstat -tulpn | grep :8080
   netstat -tulpn | grep :8081
   netstat -tulpn | grep :8082
   
   # Kill processes using the ports
   sudo kill -9 <PID>
   ```

2. **Container won't start:**
   ```bash
   # Check container logs
   docker logs <container_name>
   
   # Check if image was built correctly
   docker images | grep exploding-kittens
   ```

3. **Services not responding:**
   ```bash
   # Check if services are running inside container
   docker exec -it <container_name> ps aux
   
   # Test connectivity
   curl http://localhost:8080/api/player/health
   ```

### Health Checks

The Docker Compose setup includes health checks. You can monitor the health status:

```bash
# Check health status
docker-compose ps

# View health check logs
docker inspect <container_name> | grep -A 10 Health
```

## 📊 Monitoring

### View Resource Usage
```bash
# View container stats
docker stats

# View specific container stats
docker stats exploding-kittens-backend
```

### Access Container Shell
```bash
# Access running container
docker exec -it <container_name> /bin/bash

# Access container with sh
docker exec -it <container_name> /bin/sh
```

## 🔒 Security Notes

- The current setup is for development purposes
- For production, consider:
  - Using specific user instead of root
  - Adding security scanning
  - Using secrets management
  - Implementing proper network isolation

## 📝 Development

### Rebuilding After Code Changes
```bash
# Rebuild and restart
docker-compose up --build

# Or rebuild specific service
docker-compose build exploding-kittens-backend
docker-compose up exploding-kittens-backend
```

### Debugging
```bash
# Run with debug mode
docker run -p 8080:8080 -p 8081:8081 -p 8082:8082 -e DEBUG=true exploding-kittens-backend

# Access container logs
docker logs -f <container_name>
```

## 🎯 Next Steps

1. Test the API endpoints using curl or Postman
2. Connect your frontend to the WebSocket endpoints
3. Monitor the logs for any issues
4. Scale the services if needed

## 📞 Support

If you encounter any issues:
1. Check the container logs
2. Verify all ports are available
3. Ensure Docker is running properly
4. Check the service health status

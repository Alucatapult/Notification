# Notification System

A scalable, resilient notification service with WebSocket support for real-time notifications.

## Features

- **Real-time Notifications**: Using WebSockets for instant delivery
- **Persistent Storage**: H2/PostgreSQL for notification history
- **Security**: JWT-based authentication
- **Resilience**: Circuit breakers for handling downstream failures
- **Rate Limiting**: Protection against abuse
- **Multiple Clients**: Java CLI client and Web client for testing
- **Docker Support**: Containerized deployment for easy scaling

## System Architecture

The notification system is built on a modern microservice architecture with several components working together to provide reliable, scalable notification delivery.

```
┌─────────────────┐     ┌────────────────────────────────────┐     ┌─────────────────┐
│                 │     │                                    │     │                 │
│   Web Client    │◄────┤                                    │────►│   Database      │
│   (Browser)     │     │                                    │     │   (H2/Postgres) │
│                 │     │                                    │     │                 │
└─────────────────┘     │                                    │     └─────────────────┘
                        │       Notification Service         │
┌─────────────────┐     │                                    │     ┌─────────────────┐
│                 │     │         ┌──────────────┐           │     │                 │
│   Java Client   │◄────┤         │ WebSocket    │           │────►│   Cache         │
│   (CLI)         │     │         │ Subsystem    │           │     │   (Optional)    │
│                 │     │         └──────────────┘           │     │                 │
└─────────────────┘     └────────────────────────────────────┘     └─────────────────┘
```

### Core Components

1. **Notification Service**: Spring Boot application serving as the central hub
   - REST API for notification management
   - WebSocket endpoint for real-time delivery
   - JWT authentication for security
   - Circuit breakers for resilience

2. **Database Layer**:
   - H2 in-memory database for development/testing
   - PostgreSQL support for production environments
   - JPA repositories for persistence operations

3. **WebSocket Subsystem**:
   - STOMP over WebSocket for pub/sub messaging
   - User-specific queues for private notifications
   - Authentication integration with JWT

4. **Client Applications**:
   - Web Client: JavaScript/HTML application for browser-based testing
   - Java Client: Command-line application for automated testing

### Internal Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐    ┌────────────┐   │
│  │            │    │            │    │            │    │            │   │
│  │ Controller │───►│  Service   │───►│ Repository │───►│  Database  │   │
│  │   Layer    │    │   Layer    │    │   Layer    │    │            │   │
│  │            │    │            │    │            │    │            │   │
│  └────────────┘    └────────────┘    └────────────┘    └────────────┘   │
│         ▲                 │                                             │
│         │                 ▼                                             │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐                     │
│  │            │    │            │    │            │                     │
│  │  Security  │    │ WebSocket  │    │   Metrics  │                     │
│  │  Filters   │    │  Broker    │    │  Collector │                     │
│  │            │    │            │    │            │                     │
│  └────────────┘    └────────────┘    └────────────┘                     │
│                          │                                              │
│                          ▼                                              │
│                    ┌────────────┐                                       │
│                    │            │                                       │
│                    │   Client   │                                       │
│                    │ Connection │                                       │
│                    │            │                                       │
│                    └────────────┘                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Key Flows

1. **Notification Creation Flow**:
   - Client authenticates with the system to obtain JWT token
   - Client creates a notification through REST API with recipient information
   - Service validates and persists the notification
   - WebSocket subsystem delivers the notification to connected recipient
   - Status updates are stored in the database

2. **Real-time Delivery Flow**:
   - User connects to WebSocket endpoint with authentication
   - User subscribes to their personal notification queue
   - When a notification targets the user, it's immediately pushed
   - Client receives and displays the notification without polling

3. **Resilience Patterns**:
   - Circuit breakers prevent cascading failures
   - Retry mechanisms for failed notification delivery
   - Fallback strategies when components are unavailable

## Prerequisites

- Java 17+
- Maven 3.6+
- Modern web browser
- PostgreSQL (optional, for production)

## Getting Started

### 1. Running the Notification Server

```bash
# Navigate to the root directory of the project
cd /path/to/Notification

# Build the application
mvn clean install -DskipTests

# Start the server
mvn spring-boot:run
```

#### Expected Output:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.3)

...
2025-04-24T18:XX:XX.XXX+05:30  INFO XXXX --- [notification-service] [main] o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port 8080 (http) with context path ''
2025-04-24T18:XX:XX.XXX+05:30  INFO XXXX --- [notification-service] [main] c.n.NotificationServiceApplication : Started NotificationServiceApplication in X.XXX seconds
```

### 2. Verifying Server Status

To verify the server is running properly, you can make a simple API request:

```bash
# Test authentication endpoint
curl -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

If successful, you should receive a JWT token:
```json
{"token":"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0Ijo..."}
```

### 3. Running the Web Client

The web client provides a user-friendly interface for testing the notification system.

```bash
# Navigate to the web client directory
cd web-client

# Option 1: Using Python's built-in HTTP server
python -m http.server 8082

# Option 2: Using PHP's built-in server
php -S localhost:8082

# Option 3: Using Node.js http-server (install if needed: npm install -g http-server)
http-server -p 8082
```

Open your browser and navigate to:
```
http://localhost:8082
```

#### Testing with the Web Client:

1. **Authentication**:
   - Enter username: `user`
   - Enter password: `password`
   - Click "Login"
   - Verify: You should see "Authenticated" status and the JWT token should be displayed

2. **Create a Notification**:
   - Fill in the form:
     - Recipient: `user` (sending to yourself for testing)
     - Type: Select `INFO` or `ALERT`
     - Message: Enter any test message
   - Click "Send Notification"
   - Verify: A success message should appear and the notification should be listed in the notifications panel

3. **Real-time Notification Reception**:
   - When you receive a notification while connected, it will appear with a highlight animation
   - The notification will be automatically added to your list without refreshing

### 4. Running the Java Client

The Java client provides a command-line interface for testing notifications.

```bash
# Navigate to the Java client directory
cd java-client

# Build the client
mvn clean package

# Run the client
java -jar target/notification-client-0.0.1-SNAPSHOT.jar
```

#### Testing with the Java Client:

The Java client presents a menu-driven interface:

1. **Login** (Option 1):
   - Enter username: `user`
   - Enter password: `password`
   - Verify: You should see "Login successful!"

2. **Create Notification** (Option 2):
   - Enter recipient: `user` (or any valid username)
   - Enter notification type: `INFO` or `ALERT`
   - Enter message: Any test message
   - Verify: You should see "Notification created successfully with ID: X"

3. **View Notifications** (Option 3):
   - Verify: You should see a list of notifications for your user

4. **Get Notification by ID** (Option 4):
   - Enter the ID from a previously created notification
   - Verify: You should see the details of that specific notification

5. **Real-time Notifications**:
   - When you receive a notification while the client is running, it will automatically display it
   - You should see: "[REAL-TIME NOTIFICATION RECEIVED]: Your message here"

### 5. Testing Real-time Communication Between Clients

To test real-time communication, you should have both clients running simultaneously:

1. Ensure the server is running
2. Launch the web client in a browser
3. Start the Java client in a terminal
4. Log in to both clients with the same or different users
5. Send a notification from one client to the other
6. Verify that the recipient client receives the notification in real-time

#### Example Scenario:

1. Login to Web Client as "user1"
2. Login to Java Client as "user2"
3. From Web Client, send a notification to "user2"
4. Observe the notification appearing immediately in the Java Client
5. From Java Client, send a notification to "user1"
6. Observe the notification appearing with animation in the Web Client

### 6. Testing with cURL

For automated testing or scripting, you can use cURL commands:

```bash
# 1. Get a JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 2. Create a notification
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"recipient":"user","type":"INFO","payload":"Test notification from cURL","status":"PENDING"}'

# 3. Get notifications for a user
curl -X GET http://localhost:8080/api/notifications/recipient/user \
  -H "Authorization: Bearer $TOKEN"

# 4. Get a specific notification
curl -X GET http://localhost:8080/api/notifications/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 7. Docker Deployment

The notification system can be deployed using Docker for improved portability and isolation.

#### Prerequisites
- Docker Engine (20.10.x or higher)
- Docker Compose (2.x or higher)

#### Creating a Dockerfile

Create a `Dockerfile` in the project root:

```dockerfile
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose Setup

Create a `docker-compose.yml` file for running the complete stack:

```yaml
version: '3.8'
services:
  # Notification Service
  notification-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/notification
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=password
      - SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
      - SPRING_JPA_HIBERNATE_DDL_AUTO=update
      - JWT_SECRET=dockerSecretKey
      - SPRING_MAIN_ALLOW_CIRCULAR_REFERENCES=true
    depends_on:
      - postgres
    restart: unless-stopped
    networks:
      - notification-network

  # PostgreSQL Database
  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=notification
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - notification-network

  # Web Client (Optional)
  web-client:
    image: nginx:alpine
    ports:
      - "8082:80"
    volumes:
      - ./web-client:/usr/share/nginx/html
    networks:
      - notification-network

networks:
  notification-network:
    driver: bridge

volumes:
  postgres-data:
```

#### Running with Docker Compose

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f notification-service

# Scale services if needed
docker-compose up -d --scale notification-service=2

# Stop all services
docker-compose down
```

#### Accessing Services

- **Notification Service API**: http://localhost:8080
- **Web Client**: http://localhost:8082
- **PostgreSQL**: localhost:5432

#### Production Considerations

For production deployments, consider the following enhancements:

1. **Security**:
   - Use secret management (Docker secrets or environment management)
   - Configure HTTPS with proper certificates
   - Modify JWT secret and expiration settings

2. **Persistence**:
   - Use named volumes or external storage for PostgreSQL data
   - Configure proper backup strategies

3. **Monitoring**:
   - Add Prometheus and Grafana containers for monitoring
   - Expose actuator endpoints for health checks

4. **Load Balancing**:
   - Add a load balancer for multiple notification service instances
   - Configure sticky sessions for WebSocket connections

#### Docker Deployment Example

```bash
# Clone repository
git clone https://github.com/yourusername/notification-service.git
cd notification-service

# Build images and start services
docker-compose up -d

# Test with curl (wait for service to start)
curl -X POST http://localhost:8080/api/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'

# Access web client
# Open browser to http://localhost:8082
```

## Troubleshooting

### Common Issues:

1. **Server won't start due to circular dependencies**:
   - Ensure `spring.main.allow-circular-references=true` is set in application.properties
   - Check if all required dependencies are in pom.xml

2. **Database connection errors**:
   - For development, use H2 in-memory database
   - Verify database credentials and connection settings in application.properties

3. **WebSocket connection issues**:
   - Ensure the JWT token is valid
   - Check that you're subscribing to the correct destination: `/user/{username}/queue/notifications`
   - Verify that WebSocket connections are being authorized properly

4. **Authentication failures**:
   - Verify you're using the correct credentials (default: user/password)
   - Check if the token has expired (default expiration is 24 hours)

5. **Java Client compilation errors**:
   - If using Lombok, ensure annotation processing is enabled in your IDE
   - Alternatively, use explicit getters/setters in model classes

6. **Web Client not connecting**:
   - Ensure CORS is properly configured on the server
   - Check browser console for any connection errors
   - Verify you're using the correct server URL

### Docker-Specific Issues:

1. **Container fails to start**:
   - Check logs with `docker-compose logs notification-service`
   - Verify database connection settings are correct
   - Ensure ports are not already in use

2. **Network connectivity issues**:
   - Verify that services are on the same network
   - Check container naming and service discovery

3. **Database persistence issues**:
   - Verify volume mounts are configured correctly
   - Check database container logs

4. **Performance in containers**:
   - Configure appropriate memory limits in compose file
   - Monitor resource usage with `docker stats`

## Technology Stack

- **Backend**: Spring Boot 3.2.3
- **Security**: Spring Security, JWT
- **Persistence**: JPA/Hibernate, H2/PostgreSQL
- **Real-time Communication**: WebSocket, STOMP
- **Resilience**: Resilience4j
- **Frontend**: HTML, JavaScript, Bootstrap
- **Testing Clients**: Java, JavaScript

## API Reference

### Authentication

```
POST /api/authenticate
Request: {"username":"user","password":"password"}
Response: {"token":"eyJhbGciOiJIUzI1NiJ9..."}
```

### Notifications

```
POST /api/notifications
Headers: Authorization: Bearer <token>
Request: {"recipient":"username","type":"INFO","payload":"message","status":"PENDING"}

GET /api/notifications/{id}
Headers: Authorization: Bearer <token>

GET /api/notifications/recipient/{username}
Headers: Authorization: Bearer <token>
```

### WebSocket

```
Connect: ws://localhost:8080/ws
Headers: Authorization: Bearer <token>
Subscribe: /user/{username}/queue/notifications
```

## Configuration

Key configuration options in `application.properties`:

```properties
# Server
server.port=8080

# Database (H2 for development)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=password
spring.h2.console.enabled=true

# Security
jwt.secret=VerySecureSecretKeyForJWTSigningAndVerificationPurposesOnly
jwt.expiration=86400000  # 24 hours

# WebSocket
spring.websocket.path=/ws
```

### Docker Environment Variables

When running in Docker, you can configure the application using environment variables:

```
SPRING_DATASOURCE_URL            - Database connection URL
SPRING_DATASOURCE_USERNAME       - Database username
SPRING_DATASOURCE_PASSWORD       - Database password
JWT_SECRET                       - Secret key for JWT tokens
SPRING_MAIN_ALLOW_CIRCULAR_REFERENCES - Set to true to handle circular dependencies
SERVER_PORT                      - Customize the server port (default: 8080)
``` 
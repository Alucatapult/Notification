# Notification Service Server

This guide covers the server-side setup, configuration, and implementation details of the Notification System.

## Server Architecture

The Notification Service is built as a Spring Boot application with the following key components:

1. **REST Controllers**
   - `NotificationController`: Manages notification CRUD operations
   - `AuthController`: Handles user authentication and JWT token generation

2. **WebSocket Infrastructure**
   - STOMP-based messaging for real-time delivery
   - Authentication integration via JWT
   - User-specific notification queues

3. **Service Layer**
   - `NotificationService`: Core business logic for notification handling
   - Retry mechanisms for failed notifications
   - Circuit breakers for resilience

4. **Persistence Layer**
   - JPA/Hibernate for ORM
   - H2 in-memory database for development/testing
   - PostgreSQL support for production

5. **Security Infrastructure**
   - JWT-based authentication
   - Role-based access control
   - WebSocket security integration

## Setup and Configuration

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- H2 or PostgreSQL database

### Configuration Options

The application can be configured via `application.properties`:

```properties
# Server configuration
server.port=8080

# Spring configuration
spring.main.allow-circular-references=true

# Database configuration (H2 in-memory by default)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JPA configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Security configuration
jwt.secret=VerySecureSecretKeyForJWTSigningAndVerificationPurposesOnly
jwt.expiration=86400000  # 24 hours in milliseconds

# WebSocket configuration
spring.websocket.path=/ws

# Logging
logging.level.root=INFO
logging.level.com.notification=DEBUG
logging.level.org.springframework.security=INFO
```

For PostgreSQL in production, update these properties:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/notification
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### Running the Server

#### Development Mode

```bash
# Clone the repository
git clone https://github.com/yourusername/notification-service.git
cd notification-service

# Build the application
mvn clean package

# Run with development profile
mvn spring-boot:run
```

#### Production Deployment

For production, build an executable JAR:

```bash
mvn clean package
java -jar target/notification-service-0.0.1-SNAPSHOT.jar
```

For Docker deployment:

```bash
docker build -t notification-service .
docker run -p 8080:8080 notification-service
```

## API Documentation

### Authentication

```
POST /api/authenticate

Request:
{
  "username": "user",
  "password": "password"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "user"
}
```

### Notification Endpoints

#### Create Notification

```
POST /api/notifications

Headers:
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Request:
{
  "recipient": "username",
  "type": "INFO",
  "payload": "Notification message content",
  "status": "PENDING"
}

Response:
{
  "id": 1,
  "recipient": "username",
  "type": "INFO",
  "payload": "Notification message content",
  "status": "PENDING",
  "createdAt": "2025-04-24T12:34:56",
  "processedAt": null,
  "errorMessage": null,
  "retryCount": 0
}
```

#### Get Notification by ID

```
GET /api/notifications/{id}

Headers:
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Response:
{
  "id": 1,
  "recipient": "username",
  "type": "INFO",
  "payload": "Notification message content",
  "status": "DELIVERED",
  "createdAt": "2025-04-24T12:34:56",
  "processedAt": "2025-04-24T12:34:57",
  "errorMessage": null,
  "retryCount": 0
}
```

#### Get User Notifications

```
GET /api/notifications/recipient/{username}

Headers:
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Response:
[
  {
    "id": 1,
    "recipient": "username",
    "type": "INFO",
    "payload": "Notification message content",
    "status": "DELIVERED",
    "createdAt": "2025-04-24T12:34:56",
    "processedAt": "2025-04-24T12:34:57",
    "errorMessage": null,
    "retryCount": 0
  },
  ...
]
```

## WebSocket Connectivity

### Connecting

1. Connect to the WebSocket endpoint: `ws://localhost:8080/ws`
2. Include JWT token as part of STOMP connection headers:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```

### Subscribing to notifications

Subscribe to your personal notification queue:
```
/user/{username}/queue/notifications
```

### Receiving Notifications

Notifications will be pushed as JSON messages to your subscribed queue.

## Troubleshooting

### Common Issues

1. **Circular Dependency Error**
   - Solution: Set `spring.main.allow-circular-references=true` in application.properties

2. **Database Connection Failure**
   - Solution: Verify database credentials and connection settings
   - For development, use H2 in-memory database by setting appropriate properties

3. **WebSocket Connection Issues**
   - Check if the JWT token is valid and included in connection headers
   - Verify the subscription destination matches `/user/{username}/queue/notifications`

## Security Considerations

1. **JWT Secret**
   - Change the default JWT secret in production
   - Consider using environment variables instead of hardcoding

2. **HTTPS in Production**
   - Always use HTTPS in production environments
   - Configure appropriate TLS/SSL settings

3. **WebSocket Security**
   - Ensure all WebSocket connections are authenticated
   - Use secure WebSocket (wss://) in production

## Monitoring and Logging

The application includes extensive logging:

- `DEBUG` level for `com.notification` package
- `INFO` level for general application logs
- `INFO` level for Spring Security

Access logs via:
- Console output during development
- Log files in production
- Integration with centralized logging systems (ELK, Graylog, etc.)

## Credits and References

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- WebSocket: https://docs.spring.io/spring-framework/reference/web/websocket.html 
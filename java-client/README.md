# Notification System Java Client

This is a command-line Java client for the Notification System. It allows users to:

1. Log in with their credentials
2. View their notifications
3. Create new notifications
4. Receive real-time notifications via WebSocket

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- The Notification Server must be running on `localhost:8080`

## Building the Client

1. Navigate to the `java-client` directory:
   ```bash
   cd java-client
   ```

2. Build the application using Maven:
   ```bash
   mvn clean package
   ```

## Running the Client

After building, you can run the client using:

```bash
java -jar target/notification-client-0.0.1-SNAPSHOT.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

## Using the Client

The client provides a simple command-line interface with the following options:

1. **Login**: Authenticate with the server using your username and password
2. **Create Notification**: Send a new notification to a specified recipient
3. **View All Notifications**: List all notifications for the logged-in user
4. **Get Notification by ID**: Retrieve details for a specific notification
5. **Exit**: Close the application

### Authentication

The default credentials are:
- Username: `user`
- Password: `password`

### Real-time Notifications

Once logged in, the client establishes a WebSocket connection to the server and subscribes to your personal notification queue. When you receive a new notification, it will be displayed immediately in the console.

## Configuration

The client configuration is defined in `src/main/resources/application.properties`. You can modify this file to change:

- The server URL
- The server WebSocket URL
- The client port
- Logging settings

```properties
# Default configuration
notification.server.url=http://localhost:8080
notification.server.ws-url=ws://localhost:8080/ws
```

## Notes

- The JWT token is stored in memory during the application lifetime
- When creating notifications, you can specify different notification types (e.g., INFO, ALERT)
- The client automatically connects to the WebSocket after successful login 
# Notification System Web Client

This is a simple web client for the Notification System. It allows users to:

1. Log in with their credentials
2. View their notifications
3. Create new notifications
4. Receive real-time notifications via WebSocket

## Setup and Usage

### Prerequisites

- The Notification Server must be running on `localhost:8080`
- Modern web browser with JavaScript enabled

### Running the Web Client

Since this is a simple HTML/JavaScript application, you can open the `index.html` file directly in your browser. Alternatively, you can serve it using a simple HTTP server:

#### Using Python HTTP Server

```bash
# Navigate to the web-client directory
cd web-client

# Start a simple HTTP server (Python 3)
python -m http.server 8082
```

Then open your browser and navigate to `http://localhost:8082`

#### Using Node.js HTTP Server

First, install `http-server` globally:

```bash
npm install -g http-server
```

Then run:

```bash
# Navigate to the web-client directory
cd web-client

# Start the HTTP server
http-server -p 8082
```

Then open your browser and navigate to `http://localhost:8082`

### How to Use

1. **Log in**: Use the credentials provided by the notification system (default: username=`user`, password=`password`)
2. **View Notifications**: After logging in, your notifications will be displayed in the right panel
3. **Create Notification**: Use the form on the left panel to create a new notification
   - Recipient: The username of the person who should receive the notification
   - Type: Select the type of notification (INFO, ALERT, MESSAGE)
   - Message: The content of the notification
4. **Real-time Updates**: When someone sends you a notification, it will appear in real-time at the top of your notification list

## Notes

- The JWT token is stored in memory and will be lost if you refresh the page (you'll need to log in again)
- WebSocket connection is established after successful login
- The client automatically subscribes to your personal notification queue 
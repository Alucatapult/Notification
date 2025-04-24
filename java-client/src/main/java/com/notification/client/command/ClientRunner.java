package com.notification.client.command;

import com.notification.client.model.Notification;
import com.notification.client.service.AuthService;
import com.notification.client.service.NotificationService;
import com.notification.client.service.WebSocketClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientRunner implements CommandLineRunner {

    private final AuthService authService;
    private final NotificationService notificationService;
    private final WebSocketClientService webSocketClientService;
    
    private String loggedInUser;

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Notification Client ===");
        
        webSocketClientService.addMessageHandler(this::handleNotification);
        
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter option: ");
            String option = scanner.nextLine().trim();
            
            switch (option) {
                case "1":
                    login(scanner);
                    break;
                case "2":
                    if (checkLoggedIn()) {
                        createNotification(scanner);
                    }
                    break;
                case "3":
                    if (checkLoggedIn()) {
                        viewNotifications();
                    }
                    break;
                case "4":
                    if (checkLoggedIn()) {
                        getNotificationById(scanner);
                    }
                    break;
                case "5":
                    if (checkLoggedIn()) {
                        webSocketClientService.disconnect();
                    }
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
        
        System.out.println("Exiting...");
    }
    
    private void printMenu() {
        System.out.println("\nOptions:");
        System.out.println("1. Login");
        System.out.println("2. Create Notification");
        System.out.println("3. View All Notifications");
        System.out.println("4. Get Notification by ID");
        System.out.println("5. Exit");
        
        if (loggedInUser != null) {
            System.out.println("\nLogged in as: " + loggedInUser);
        }
    }
    
    private void login(Scanner scanner) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();
        
        String token = authService.authenticate(username, password);
        
        if (token != null) {
            this.loggedInUser = username;
            System.out.println("Login successful!");
            
            // Connect to WebSocket after login
            boolean connected = webSocketClientService.connect(username);
            if (connected) {
                System.out.println("Connected to notification server via WebSocket.");
            } else {
                System.out.println("Failed to connect to notification server via WebSocket.");
            }
        } else {
            System.out.println("Login failed. Please check your credentials.");
        }
    }
    
    private void createNotification(Scanner scanner) {
        System.out.print("Enter recipient username: ");
        String recipient = scanner.nextLine().trim();
        
        System.out.print("Enter notification type (e.g., ALERT, INFO): ");
        String type = scanner.nextLine().trim();
        
        System.out.print("Enter notification message: ");
        String message = scanner.nextLine().trim();
        
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setPayload(message);
        notification.setStatus("PENDING");
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification created = notificationService.createNotification(notification);
        
        if (created != null) {
            System.out.println("Notification created successfully with ID: " + created.getId());
        } else {
            System.out.println("Failed to create notification.");
        }
    }
    
    private void viewNotifications() {
        List<Notification> notifications = notificationService.getNotificationsForUser(loggedInUser);
        
        if (notifications.isEmpty()) {
            System.out.println("No notifications found.");
            return;
        }
        
        System.out.println("\n=== Notifications for " + loggedInUser + " ===");
        for (Notification notification : notifications) {
            printNotification(notification);
        }
    }
    
    private void getNotificationById(Scanner scanner) {
        System.out.print("Enter notification ID: ");
        String idStr = scanner.nextLine().trim();
        
        try {
            Long id = Long.parseLong(idStr);
            Notification notification = notificationService.getNotification(id);
            
            if (notification != null) {
                System.out.println("\n=== Notification Details ===");
                printNotification(notification);
            } else {
                System.out.println("Notification not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format. Please enter a valid number.");
        }
    }
    
    private void printNotification(Notification notification) {
        System.out.println("ID: " + notification.getId());
        System.out.println("Type: " + notification.getType());
        System.out.println("Recipient: " + notification.getRecipient());
        System.out.println("Message: " + notification.getPayload());
        System.out.println("Status: " + notification.getStatus());
        System.out.println("Created: " + notification.getCreatedAt());
        System.out.println("Processed: " + notification.getProcessedAt());
        System.out.println("------------------------------");
    }
    
    private boolean checkLoggedIn() {
        if (loggedInUser == null) {
            System.out.println("You must login first.");
            return false;
        }
        return true;
    }
    
    private void handleNotification(String message) {
        System.out.println("\n[REAL-TIME NOTIFICATION RECEIVED]: " + message);
        System.out.print("Enter option: "); // Re-prompt the user
    }
} 
package com.notification.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String type;  // INFO, ALERT, WARNING, etc.

    @Column(nullable = false, length = 1000)
    private String payload;

    @Column(nullable = false)
    private String status;  // PENDING, DELIVERED, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "retry_count")
    private Integer retryCount;

    // Default constructor for JPA
    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
        this.retryCount = 0;
    }

    // Constructor with essential fields
    public Notification(String recipient, String type, String payload) {
        this();
        this.recipient = recipient;
        this.type = type;
        this.payload = payload;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipient='" + recipient + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
package com.notification.client.model;

import java.time.LocalDateTime;

public class Notification {

    private Long id;
    private String type;
    private String recipient;
    private String payload;
    private String status;
    private String errorMessage;
    private Integer retryCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    public Notification() {
    }
    
    public Notification(Long id, String type, String recipient, String payload, String status, 
                        String errorMessage, Integer retryCount, LocalDateTime createdAt, 
                        LocalDateTime processedAt) {
        this.id = id;
        this.type = type;
        this.recipient = recipient;
        this.payload = payload;
        this.status = status;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
} 
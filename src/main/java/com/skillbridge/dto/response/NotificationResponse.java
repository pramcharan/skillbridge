package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.NotificationType;
import lombok.Data;

import java.time.Instant;

@Data
public class NotificationResponse {
    private Long             id;
    private NotificationType type;
    private String           title;
    private String           message;
    private String           link;
    private Boolean          isRead;
    private Instant          createdAt;
}
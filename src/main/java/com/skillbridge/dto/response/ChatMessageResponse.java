package com.skillbridge.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class ChatMessageResponse {
    private Long    id;
    private Long    projectId;
    private Long    senderId;
    private String  senderName;
    private String  senderAvatar;
    private String  content;
    private Long replyToId;
    private String replyToContent;
    private String replyToSender;
    private Boolean isRead;
    private Instant createdAt;
    private boolean mine; // true if sent by current user
}
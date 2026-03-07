package com.skillbridge.dto.response;

import com.skillbridge.entity.enums.ProjectStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class ProjectSummaryResponse {
    private Long          id;
    private String        title;
    private ProjectStatus status;
    private String        otherPartyName;   // client or freelancer name
    private String        otherPartyAvatar;
    private String        lastMessage;
    private Instant       lastMessageAt;
    private int           unreadCount;
    private Instant       createdAt;
}
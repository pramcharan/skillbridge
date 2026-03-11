package com.skillbridge.dto.response;
// CommunityMessageResponse.java

import lombok.Data;
import java.time.Instant;
import java.util.*;

@Data
public class CommunityMessageResponse {
    private Long              id;
    private String            room;
    private Long              senderId;
    private String            senderName;
    private String            senderAvatar;
    private String            senderRole;
    private String            content;
    private boolean           pinned;
    private boolean           isFile;
    private String            fileUrl;
    private String            fileName;
    private String            fileType;
    private List<String>      mentionedUsers;
    private Map<String, Long> reactions;   // emoji → count
    private List<String>      reactedBy;   // emojis current user reacted with
    private Instant           createdAt;
}
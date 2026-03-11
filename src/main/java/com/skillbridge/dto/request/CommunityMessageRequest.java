package com.skillbridge.dto.request;

import lombok.Data;

@Data
public class CommunityMessageRequest {
    private String  room;
    private String  content;
    private boolean isFile   = false;
    private String  fileUrl;
    private String  fileName;
    private String  fileType;
}
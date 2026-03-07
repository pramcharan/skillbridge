package com.skillbridge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message too long")
    private String content;
}
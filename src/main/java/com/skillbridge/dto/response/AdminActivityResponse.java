package com.skillbridge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminActivityResponse {
    private String text;
    private String timeAgo;
    private String badgeColor; // 'teal', 'gold', 'rose', 'purple'
    private Instant timestamp; // Internal sort key
}

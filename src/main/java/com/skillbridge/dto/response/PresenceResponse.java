package com.skillbridge.dto.response;
import lombok.Data;
import java.util.List;

@Data
public class PresenceResponse {
    private String            room;
    private int               onlineCount;
    private List<OnlineUser>  users;

    @Data
    public static class OnlineUser {
        private Long   id;
        private String name;
        private String avatar;
        private String role;
    }
}
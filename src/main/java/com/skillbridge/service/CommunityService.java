package com.skillbridge.service;

import com.skillbridge.dto.*;
import com.skillbridge.dto.request.CommunityMessageRequest;
import com.skillbridge.dto.response.CommunityMessageResponse;
import com.skillbridge.dto.response.PresenceResponse;
import com.skillbridge.entity.*;
import com.skillbridge.exception.BadRequestException;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityMessageRepository  messageRepository;
    private final CommunityReactionRepository reactionRepository;
    private final RoomPresenceRepository      presenceRepository;
    private final UserRepository              userRepository;
    private final SimpMessagingTemplate       messagingTemplate;
    private final NotificationService         notificationService;

    private static final Set<String> VALID_ROOMS = Set.of(
            "general", "developers", "creatives", "opportunities", "help");

    private static final Set<String> VALID_EMOJIS = Set.of(
            "👍", "❤️", "😂", "🚀", "👏", "🔥", "💡", "🎉");

    private static final int  PAGE_SIZE   = 50;
    private static final long ONLINE_MINS = 5;

    // ── Get recent messages ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CommunityMessageResponse> getMessages(
            String room, String viewerEmail) {
        validateRoom(room);
        User viewer = getUserByEmail(viewerEmail);

        List<CommunityMessage> messages = messageRepository
                .findRecentByRoom(room,
                        PageRequest.of(0, PAGE_SIZE));

        // Reverse so oldest is first
        Collections.reverse(messages);
        return messages.stream()
                .map(m -> toResponse(m, viewer.getId()))
                .collect(Collectors.toList());
    }

    // ── Get pinned messages ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CommunityMessageResponse> getPinned(String room) {
        validateRoom(room);
        return messageRepository.findPinnedByRoom(room)
                .stream().map(m -> toResponse(m, null))
                .collect(Collectors.toList());
    }

    // ── Send message ──────────────────────────────────────────────────
    @Transactional
    public CommunityMessageResponse sendMessage(
            String senderEmail, CommunityMessageRequest req) {
        validateRoom(req.getRoom());

        if (!req.isFile()
                && (req.getContent() == null
                || req.getContent().isBlank())) {
            throw new BadRequestException("Message cannot be empty.");
        }
        if (!req.isFile() && req.getContent().length() > 2000) {
            throw new BadRequestException(
                    "Message too long. Max 2000 characters.");
        }

        User sender = getUserByEmail(senderEmail);

        CommunityMessage msg = new CommunityMessage();
        msg.setRoom(req.getRoom());
        msg.setSender(sender);
        msg.setContent(req.isFile() ? "[file]" : req.getContent().trim());
        msg.setFile(req.isFile());
        msg.setFileUrl(req.getFileUrl());
        msg.setFileName(req.getFileName());
        msg.setFileType(req.getFileType());

        // Extract @mentions
        List<String> mentions = extractMentions(msg.getContent());
        if (!mentions.isEmpty()) {
            msg.setMentionedUsers(String.join(",", mentions));
            notifyMentions(mentions, sender, req.getRoom(), msg.getContent());
        }

        CommunityMessage saved = messageRepository.save(msg);

        // Update presence
        updatePresence(sender, req.getRoom(), true);

        CommunityMessageResponse response =
                toResponse(saved, sender.getId());

        // Broadcast to room
        messagingTemplate.convertAndSend(
                "/topic/community." + req.getRoom(), response);

        return response;
    }

    // ── Toggle reaction ───────────────────────────────────────────────
    @Transactional
    public Map<String, Object> toggleReaction(
            Long messageId, String emoji, String userEmail) {

        if (!VALID_EMOJIS.contains(emoji)) {
            throw new BadRequestException("Invalid emoji.");
        }

        User user = getUserByEmail(userEmail);
        CommunityMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found: " + messageId));

        Optional<CommunityReaction> existing =
                reactionRepository.findByMessageIdAndUserIdAndEmoji(
                        messageId, user.getId(), emoji);

        boolean added;
        if (existing.isPresent()) {
            reactionRepository.deleteByMessageIdAndUserIdAndEmoji(
                    messageId, user.getId(), emoji);
            added = false;
        } else {
            CommunityReaction r = new CommunityReaction();
            r.setMessage(msg);
            r.setUser(user);
            r.setEmoji(emoji);
            reactionRepository.save(r);
            added = true;
        }

        long count = reactionRepository
                .countByMessageIdAndEmoji(messageId, emoji);

        Map<String, Object> result = Map.of(
                "messageId", messageId,
                "emoji",     emoji,
                "count",     count,
                "added",     added,
                "userId",    user.getId()
        );

        // Broadcast reaction update
        messagingTemplate.convertAndSend("/topic/community." + msg.getRoom() + ".reactions", (Object) result);
        return result;
    }

    // ── Pin / Unpin message (admin only) ──────────────────────────────
    @Transactional
    public CommunityMessageResponse togglePin(
            Long messageId, String adminEmail) {
        User admin = getUserByEmail(adminEmail);
        if (!admin.getRole().name().equals("ADMIN")) {
            throw new BadRequestException("Only admins can pin messages.");
        }

        CommunityMessage msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found: " + messageId));

        msg.setPinned(!msg.isPinned());
        messageRepository.save(msg);

        CommunityMessageResponse response =
                toResponse(msg, admin.getId());

        // Broadcast pin update
        messagingTemplate.convertAndSend(
                "/topic/community." + msg.getRoom() + ".pin", response);

        return response;
    }

    // ── Join room (presence) ──────────────────────────────────────────
    @Transactional
    public PresenceResponse joinRoom(String userEmail, String room) {
        validateRoom(room);
        User user = getUserByEmail(userEmail);
        updatePresence(user, room, true);
        PresenceResponse presence = getPresence(room);

        messagingTemplate.convertAndSend(
                "/topic/community." + room + ".presence", presence);

        return presence;
    }

    // ── Leave room (presence) ─────────────────────────────────────────
    @Transactional
    public void leaveRoom(String userEmail, String room) {
        validateRoom(room);
        User user = getUserByEmail(userEmail);
        updatePresence(user, room, false);

        PresenceResponse presence = getPresence(room);
        messagingTemplate.convertAndSend(
                "/topic/community." + room + ".presence", presence);
    }

    // ── Get presence ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PresenceResponse getPresence(String room) {
        Instant since = Instant.now()
                .minus(ONLINE_MINS, ChronoUnit.MINUTES);

        List<RoomPresence> online =
                presenceRepository.findOnlineInRoom(room, since);

        PresenceResponse res = new PresenceResponse();
        res.setRoom(room);
        res.setOnlineCount(online.size());
        res.setUsers(online.stream().map(rp -> {
            PresenceResponse.OnlineUser u =
                    new PresenceResponse.OnlineUser();
            u.setId(rp.getUser().getId());
            u.setName(rp.getUser().getName());
            u.setAvatar(rp.getUser().getAvatarUrl());
            u.setRole(rp.getUser().getRole().name());
            return u;
        }).collect(Collectors.toList()));

        return res;
    }

    // ── Freelancer Spotlight ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSpotlight() {
        // Top 5 freelancers by avg rating + recent activity
        return userRepository
                .findTopFreelancersForSpotlight(
                        PageRequest.of(0, 5))
                .stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",          u.getId());
                    m.put("name",        u.getName());
                    m.put("avatar",      u.getAvatarUrl());
                    m.put("skills",      u.getSkills());
                    m.put("avgRating",   u.getAvgRating());
                    m.put("reviewCount", u.getReviewCount());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void validateRoom(String room) {
        if (!VALID_ROOMS.contains(room)) {
            throw new BadRequestException(
                    "Invalid room. Valid rooms: " +
                            String.join(", ", VALID_ROOMS));
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    private void updatePresence(User user, String room, boolean online) {
        RoomPresence rp = presenceRepository
                .findByUserIdAndRoom(user.getId(), room)
                .orElseGet(() -> {
                    RoomPresence p = new RoomPresence();
                    p.setUser(user);
                    p.setRoom(room);
                    return p;
                });
        rp.setOnline(online);
        rp.setLastSeen(Instant.now());
        presenceRepository.save(rp);
    }

    private List<String> extractMentions(String content) {
        if (content == null) return List.of();
        Pattern p = Pattern.compile("@(\\w+)");
        Matcher m = p.matcher(content);
        List<String> mentions = new ArrayList<>();
        while (m.find()) mentions.add(m.group(1));
        return mentions;
    }

    private void notifyMentions(List<String> usernames,
                                User sender,
                                String room,
                                String content) {
        for (String username : usernames) {
            userRepository.findByNameIgnoreCase(username)
                    .ifPresent(mentioned -> {
                        try {
                            notificationService.send(
                                    mentioned,
                                    com.skillbridge.entity.enums.NotificationType.SYSTEM,
                                    sender.getName() + " mentioned you",
                                    "In #" + room + ": " +
                                            content.substring(0, Math.min(80, content.length())),
                                    "/community.html?room=" + room
                            );
                        } catch (Exception e) {
                            log.warn("Failed to notify mention: {}",
                                    e.getMessage());
                        }
                    });
        }
    }

    private CommunityMessageResponse toResponse(
            CommunityMessage msg, Long viewerUserId) {
        CommunityMessageResponse r = new CommunityMessageResponse();
        r.setId(msg.getId());
        r.setRoom(msg.getRoom());
        r.setSenderId(msg.getSender().getId());
        r.setSenderName(msg.getSender().getName());
        r.setSenderAvatar(msg.getSender().getAvatarUrl());
        r.setSenderRole(msg.getSender().getRole().name());
        r.setContent(msg.getContent());
        r.setPinned(msg.isPinned());
        r.setFile(msg.isFile());
        r.setFileUrl(msg.getFileUrl());
        r.setFileName(msg.getFileName());
        r.setFileType(msg.getFileType());
        r.setCreatedAt(msg.getCreatedAt());

        // Mentioned users
        r.setMentionedUsers(msg.getMentionedUsers() != null
                ? Arrays.asList(msg.getMentionedUsers().split(","))
                : List.of());

        // Aggregate reactions: emoji → count
        Map<String, Long> reactionMap = new LinkedHashMap<>();
        List<String> reactedBy = new ArrayList<>();
        for (CommunityReaction rx : msg.getReactions()) {
            reactionMap.merge(rx.getEmoji(), 1L, Long::sum);
            if (viewerUserId != null
                    && rx.getUser().getId().equals(viewerUserId)) {
                reactedBy.add(rx.getEmoji());
            }
        }
        r.setReactions(reactionMap);
        r.setReactedBy(reactedBy);

        return r;
    }
}
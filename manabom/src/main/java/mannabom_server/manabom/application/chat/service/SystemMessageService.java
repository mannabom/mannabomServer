package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.notification.enums.NotificationType;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMessageService {

    private static final String USER_LOCATION_PREFIX = "user:location:";

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MeetingMatchRepository meetingMatchRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RecordedSystemMessage recordForRoom(ChatSystemMessageEvent event) {
        ChatRoom room = chatRoomRepository.findById(event.roomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        return save(room, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<RecordedSystemMessage> recordMatchFound(Long matchId, Instant deadline) {
        MeetingMatch match = findMatch(matchId);
        Map<String, Object> data = Map.of(
                "matchId", matchId,
                "decisionDeadline", deadline.toString()
        );
        return List.of(
                saveForMeeting(match.getMeeting1(), ChatSystemMessageEvent.of(
                        roomIdForMeeting(match.getMeeting1()),
                        SystemMessageType.MATCH_FOUND,
                        null,
                        null,
                        data
                )),
                saveForMeeting(match.getMeeting2(), ChatSystemMessageEvent.of(
                        roomIdForMeeting(match.getMeeting2()),
                        SystemMessageType.MATCH_FOUND,
                        null,
                        null,
                        data
                ))
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<RecordedSystemMessage> recordMatchFailure(
            Long matchId,
            Long actorUserId,
            boolean isByTimeout
    ) {
        MeetingMatch match = findMatch(matchId);
        Meeting failedMeeting = findFailedMeeting(match, isByTimeout);
        Meeting opponentMeeting = failedMeeting.getId().equals(match.getMeeting1().getId())
                ? match.getMeeting2()
                : match.getMeeting1();

        SystemMessageType failedTeamType = isByTimeout
                ? SystemMessageType.MATCH_TIMED_OUT
                : SystemMessageType.MATCH_REJECTED_BY_LEADER;
        boolean bothTeamsTimedOut = isByTimeout
                && match.getMeeting1Decision() == MeetingDecision.AUTO_REJECTED
                && match.getMeeting2Decision() == MeetingDecision.AUTO_REJECTED;
        SystemMessageType opponentType = isByTimeout
                ? bothTeamsTimedOut
                        ? SystemMessageType.MATCH_TIMED_OUT
                        : SystemMessageType.OPPONENT_MATCH_TIMED_OUT
                : SystemMessageType.MATCH_REJECTED_BY_OPPONENT;
        Map<String, Object> data = Map.of("matchId", matchId, "timeout", isByTimeout);

        return List.of(
                saveForMeeting(failedMeeting, ChatSystemMessageEvent.of(
                        roomIdForMeeting(failedMeeting),
                        failedTeamType,
                        actorUserId,
                        null,
                        data
                )),
                saveForMeeting(opponentMeeting, ChatSystemMessageEvent.of(
                        roomIdForMeeting(opponentMeeting),
                        opponentType,
                        actorUserId,
                        null,
                        data
                ))
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<RecordedSystemMessage> recordMatchSuccess(Long matchId, Long actorUserId, Long chatRoomId) {
        MeetingMatch match = findMatch(matchId);
        Map<String, Object> data = Map.of(
                "matchId", matchId,
                "chatRoomId", chatRoomId
        );

        return List.of(
                saveForMeeting(match.getMeeting1(), ChatSystemMessageEvent.withoutPush(
                        roomIdForMeeting(match.getMeeting1()),
                        SystemMessageType.MATCH_COMPLETED,
                        actorUserId,
                        null,
                        data
                )),
                saveForMeeting(match.getMeeting2(), ChatSystemMessageEvent.withoutPush(
                        roomIdForMeeting(match.getMeeting2()),
                        SystemMessageType.MATCH_COMPLETED,
                        actorUserId,
                        null,
                        data
                ))
        );
    }

    public void dispatch(RecordedSystemMessage recorded) {
        boolean broadcasted = broadcast(recorded.message());
        if (recorded.pushEnabled()) {
            sendPushFallback(recorded, !broadcasted);
        }
    }

    public void dispatch(List<RecordedSystemMessage> messages) {
        messages.forEach(this::dispatch);
    }

    private boolean broadcast(ChatMessageEvent event) {
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + event.getRoomId(), event);
            return true;
        } catch (RuntimeException e) {
            log.error("시스템 메시지 WebSocket 전송 실패: roomId={}, messageId={}",
                    event.getRoomId(), event.getMessageId(), e);
            return false;
        }
    }

    private void sendPushFallback(RecordedSystemMessage recorded, boolean forcePush) {
        ChatMessageEvent message = recorded.message();
        for (Long recipientUserId : message.getRecipientUserIds()) {
            if (!forcePush && isViewingRoom(recipientUserId, message.getRoomId())) {
                continue;
            }

            Map<String, Object> data = new HashMap<>(message.getData());
            data.put("roomId", message.getRoomId());
            data.put("messageId", message.getMessageId());
            data.put("systemEventType", recorded.type().name());
            if (message.getActorUserId() != null) {
                data.put("actorUserId", message.getActorUserId());
            }
            if (message.getActorNickname() != null) {
                data.put("actorNickname", message.getActorNickname());
            }

            try {
                notificationService.sendNotification(
                        recipientUserId,
                        NotificationType.SYSTEM_MESSAGE,
                        message.getSystemTitle(),
                        null,
                        data
                );
            } catch (RuntimeException e) {
                log.error("시스템 메시지 Push 알림 처리 실패: userId={}, roomId={}, type={}",
                        recipientUserId, message.getRoomId(), recorded.type(), e);
            }
        }
    }

    private boolean isViewingRoom(Long userId, Long roomId) {
        String location = stringRedisTemplate.opsForValue().get(USER_LOCATION_PREFIX + userId);
        return String.valueOf(roomId).equals(location);
    }

    private RecordedSystemMessage saveForMeeting(Meeting meeting, ChatSystemMessageEvent event) {
        ChatRoom room = chatRoomRepository.findByMeeting_Id(meeting.getId())
                .orElseThrow(() -> new IllegalArgumentException("미팅 채팅방이 존재하지 않습니다."));
        return save(room, event);
    }

    private RecordedSystemMessage save(ChatRoom room, ChatSystemMessageEvent event) {
        String actorNickname = resolveActorNickname(event.actorUserId());
        SystemMessageType.RenderedSystemMessage rendered = event.type().render(actorNickname, event.data());
        ChatMessage message = chatMessageRepository.saveAndFlush(
                ChatMessage.system(
                        room,
                        rendered.body(),
                        event.type().name(),
                        rendered.title(),
                        event.actorUserId(),
                        actorNickname,
                        event.data()
                )
        );
        List<Long> recipients = resolveRecipients(room.getId(), event.actorUserId(), event.recipientUserIds());

        ChatMessageEvent response = ChatMessageEvent.builder()
                .roomId(room.getId())
                .senderUserId(null)
                .actorUserId(event.actorUserId())
                .recipientUserIds(recipients)
                .messageType(message.getType().name())
                .systemEventType(message.getSystemEventType())
                .systemTitle(message.getSystemTitle())
                .actorNickname(message.getActorNickname())
                .content(message.getContent())
                .data(message.getSystemData())
                .messageId(message.getId())
                .clientMessageId(null)
                .sendAt(message.getCreatedAt())
                .build();
        return new RecordedSystemMessage(response, event.type(), event.pushEnabled());
    }

    private String resolveActorNickname(Long actorUserId) {
        if (actorUserId == null) {
            return null;
        }
        return profileRepository.findByUser_UserId(actorUserId)
                .map(profile -> profile.getNickName())
                .filter(org.springframework.util.StringUtils::hasText)
                .orElse(null);
    }

    private List<Long> resolveRecipients(Long roomId, Long actorUserId, List<Long> explicitRecipients) {
        List<Long> recipients = explicitRecipients == null
                ? chatMemberRepository.findAllByRoomIdAndStatus(roomId, ChatMemberStatus.ACTIVATE)
                        .stream()
                        .map(ChatMember::getUser)
                        .map(user -> user.getUserId())
                        .toList()
                : new ArrayList<>(explicitRecipients);

        return recipients.stream()
                .filter(userId -> actorUserId == null || !actorUserId.equals(userId))
                .distinct()
                .toList();
    }

    private Long roomIdForMeeting(Meeting meeting) {
        return chatRoomRepository.findByMeeting_Id(meeting.getId())
                .map(ChatRoom::getId)
                .orElseThrow(() -> new IllegalArgumentException("미팅 채팅방이 존재하지 않습니다."));
    }

    private MeetingMatch findMatch(Long matchId) {
        return meetingMatchRepository.findByIdWithMeeting(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅 매칭입니다."));
    }

    private Meeting findFailedMeeting(MeetingMatch match, boolean isByTimeout) {
        MeetingDecision failedDecision = isByTimeout
                ? MeetingDecision.AUTO_REJECTED
                : MeetingDecision.REJECTED;

        if (match.getMeeting1Decision() == failedDecision) {
            return match.getMeeting1();
        }
        if (match.getMeeting2Decision() == failedDecision) {
            return match.getMeeting2();
        }
        throw new IllegalStateException("거절한 미팅 팀을 확인할 수 없습니다.");
    }

    public record RecordedSystemMessage(
            ChatMessageEvent message,
            SystemMessageType type,
            boolean pushEnabled
    ) {
    }
}

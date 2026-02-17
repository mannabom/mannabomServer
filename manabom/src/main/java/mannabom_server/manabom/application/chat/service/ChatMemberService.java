package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.response.ChatReadEvent;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMemberService {
    private static final String UNREAD_PREFIX = "unread:count:";
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public void updateReadStatus(Long roomId, Long userId,Long lastSeenMessageId) {
        ChatMember chatMember = chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방 멤버가 아닙니다."));

        long lastReadId = chatMember.getLastReadMessageId() == null ? 0L : chatMember.getLastReadMessageId();

        if (lastSeenMessageId <= lastReadId) return;

        List<ChatMessage> unreadMessages = chatMessageRepository.findByRoomIdAndIdBetween(roomId, lastReadId + 1, lastSeenMessageId);
        for (ChatMessage m : unreadMessages) {
            String key = UNREAD_PREFIX + m.getId();
            Long count = stringRedisTemplate.opsForValue().decrement(key);
            if (count != null && count < 0) {
                stringRedisTemplate.opsForValue().set(key, "0");
            }
        }


        ChatReadEvent readEvent = ChatReadEvent.builder()
                .roomId(roomId)
                .userId(userId)
                .lastReadMessageId(lastSeenMessageId)
                .build();
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/read", readEvent);


    }
}

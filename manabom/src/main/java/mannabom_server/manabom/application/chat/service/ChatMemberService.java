package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.response.ChatReadEvent;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMemberService {
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void updateReadStatus(Long roomId, Long userId,Long lastSeenMessageId) {
        ChatMember chatMember = chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(roomId, userId, ChatMemberStatus.ACTIVATE)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방 멤버가 아닙니다."));

        if (lastSeenMessageId == null || !chatMessageRepository.existsByIdAndRoomId(lastSeenMessageId, roomId)) {
            throw new IllegalArgumentException("해당 채팅방에 존재하지 않는 메시지입니다.");
        }

        long lastReadId = chatMember.getLastReadMessageId() == null ? 0L : chatMember.getLastReadMessageId();

        if (lastSeenMessageId <= lastReadId) return;

        chatMember.updateLastReadMessageId(lastSeenMessageId);


        ChatReadEvent readEvent = ChatReadEvent.builder()
                .roomId(roomId)
                .userId(userId)
                .lastReadMessageId(lastSeenMessageId)
                .build();
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/read", readEvent);


    }

    @Transactional
    public void markRoomAsSeen(ChatMember chatMember) {
        if (chatMember.getLastReadMessageId() == null) {
            chatMember.updateLastReadMessageId(0L);
        }
    }
}

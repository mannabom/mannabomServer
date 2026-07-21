package mannabom_server.manabom.application.chat.service;

import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ChatMemberService chatMemberService;
    @Mock private FileStoragePort fileStoragePort;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void rejectsClientAuthoredSystemMessage() {
        ChatSendRequest request = new ChatSendRequest(
                77L,
                ChatMessageType.SYSTEM,
                "가짜 시스템 메시지",
                "client-message-id"
        );

        assertThatThrownBy(() -> chatService.sendMessage(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("클라이언트가 전송할 수 없습니다");

        verifyNoInteractions(chatRoomRepository, chatMessageRepository);
    }
}

package mannabom_server.manabom.application.chat.service;

import mannabom_server.manabom.application.chat.dto.request.ChatSendRequest;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageResponse;
import mannabom_server.manabom.application.chat.dto.response.ChatSyncResponse;
import mannabom_server.manabom.application.common.port.FileStoragePort;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

    @Test
    void syncReturnsStructuredSystemMessage() {
        ChatRoom room = ChatRoom.builder().id(77L).build();
        ChatMember member = ChatMember.builder()
                .room(room)
                .status(ChatMemberStatus.ACTIVATE)
                .build();
        ChatMessage systemMessage = ChatMessage.system(
                room,
                "수락할 시 서로의 프로필이 공개됩니다.",
                "PHOTO_REQUESTED",
                "봄이님이 프로필 공개를 요청했어요",
                1L,
                "봄이",
                Map.of(
                        "actorStatus", "PENDING",
                        "recipientStatus", "RECEIVED"
                )
        );

        when(chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(
                77L,
                2L,
                ChatMemberStatus.ACTIVATE
        )).thenReturn(Optional.of(member));
        when(chatMemberRepository.findAllActiveMembersWithMainImage(77L, ChatMemberStatus.ACTIVATE))
                .thenReturn(List.of());
        when(chatMessageRepository.findChatMessagesAfter(eq(77L), eq(0L), any()))
                .thenReturn(List.of(systemMessage));

        ChatSyncResponse response = chatService.getLatestChatMessageListSync(77L, 2L, 0L);

        assertThat(response.getMessages()).hasSize(1);
        ChatMessageResponse message = response.getMessages().get(0);
        assertThat(message.getMessageType()).isEqualTo("SYSTEM");
        assertThat(message.getSystemEventType()).isEqualTo("PHOTO_REQUESTED");
        assertThat(message.getSystemTitle()).isEqualTo("봄이님이 프로필 공개를 요청했어요");
        assertThat(message.getContent()).isEqualTo("수락할 시 서로의 프로필이 공개됩니다.");
        assertThat(message.getActorUserId()).isEqualTo(1L);
        assertThat(message.getActorNickname()).isEqualTo("봄이");
        assertThat(message.getData())
                .containsEntry("actorStatus", "PENDING")
                .containsEntry("recipientStatus", "RECEIVED");
    }
}

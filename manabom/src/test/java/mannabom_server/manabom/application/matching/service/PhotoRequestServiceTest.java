package mannabom_server.manabom.application.matching.service;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageContent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.enums.ChatRoomType;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.matching.entity.LoveViewPhotoRequest;
import mannabom_server.manabom.domain.matching.entity.LoveViewRecommendHistory;
import mannabom_server.manabom.domain.matching.enums.PhotoRequestStatus;
import mannabom_server.manabom.domain.matching.repository.LoveViewPhotoRequestRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoRequestServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private LoveViewPhotoRequestRepository photoRequestRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PhotoRequestService photoRequestService;

    private final User user1 = user(1L);
    private final User user2 = user(2L);
    private ChatRoom room;
    private LoveViewRecommendHistory history;

    @BeforeEach
    void setUpRoom() {
        history = org.mockito.Mockito.mock(LoveViewRecommendHistory.class);
        when(history.getId()).thenReturn(88L);
        room = ChatRoom.builder()
                .id(77L)
                .type(ChatRoomType.LOVEVIEW_MATCH)
                .loveView(history)
                .build();
        when(chatRoomRepository.findById(77L)).thenReturn(Optional.of(room));
    }

    @Test
    void publishesSystemMessageWhenPhotoIsRequested() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        allowActiveMember(user1);
        when(photoRequestRepository.findTopByHistoryIdOrderByIdDesc(88L))
                .thenReturn(Optional.empty());
        when(chatMessageRepository.countChatMessagesByRoom_IdAndUserIsNotNull(77L))
                .thenReturn(10);
        when(chatMemberRepository.findByRoomId(77L))
                .thenReturn(List.of(ChatMember.create(room, user1), ChatMember.create(room, user2)));

        photoRequestService.createPhotoRequest(77L, 1L);

        assertPublishedMessage(SystemMessageType.PHOTO_REQUESTED, 1L, 2L);
    }

    @Test
    void publishesSystemMessageWhenPhotoRequestIsAccepted() {
        allowActiveMember(user2);
        LoveViewPhotoRequest request = pendingRequest(user1, user2);
        when(photoRequestRepository.findTopByHistoryIdOrderByIdDesc(88L))
                .thenReturn(Optional.of(request));
        when(chatMemberRepository.findByRoomId(77L))
                .thenReturn(List.of(ChatMember.create(room, user1), ChatMember.create(room, user2)));

        photoRequestService.acceptPhotoRequest(77L, 2L);

        assertThat(request.getStatus()).isEqualTo(PhotoRequestStatus.ACCEPTED);
        assertPublishedMessage(SystemMessageType.PHOTO_REQUEST_ACCEPTED, 2L, 1L);
    }

    @Test
    void publishesSystemMessageWhenPhotoRequestIsRejected() {
        allowActiveMember(user2);
        LoveViewPhotoRequest request = pendingRequest(user1, user2);
        when(photoRequestRepository.findTopByHistoryIdOrderByIdDesc(88L))
                .thenReturn(Optional.of(request));
        when(chatMemberRepository.findByRoomId(77L))
                .thenReturn(List.of(ChatMember.create(room, user1), ChatMember.create(room, user2)));

        photoRequestService.rejectPhotoRequest(77L, 2L);

        assertThat(request.getStatus()).isEqualTo(PhotoRequestStatus.REJECTED);
        assertPublishedMessage(SystemMessageType.PHOTO_REQUEST_REJECTED, 2L, 1L);
    }

    private void allowActiveMember(User user) {
        when(chatMemberRepository.findByRoomIdAndUser_UserIdAndStatus(
                77L,
                user.getUserId(),
                ChatMemberStatus.ACTIVATE
        )).thenReturn(Optional.of(ChatMember.create(room, user)));
    }

    private LoveViewPhotoRequest pendingRequest(User sender, User receiver) {
        return LoveViewPhotoRequest.builder()
                .history(history)
                .sender(sender)
                .receiver(receiver)
                .status(PhotoRequestStatus.PENDING)
                .build();
    }

    private void assertPublishedMessage(
            SystemMessageType expectedType,
            Long actorUserId,
            Long recipientUserId
    ) {
        ArgumentCaptor<ChatSystemMessageEvent> captor =
                ArgumentCaptor.forClass(ChatSystemMessageEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().roomId()).isEqualTo(77L);
        assertThat(captor.getValue().type()).isEqualTo(expectedType);
        assertThat(captor.getValue().type().getContent()).isEqualTo(expectedType.getContent());
        assertThat(captor.getValue().actorUserId()).isEqualTo(actorUserId);
        assertThat(captor.getValue().recipientUserIds()).containsExactly(recipientUserId);
    }

    private static User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}

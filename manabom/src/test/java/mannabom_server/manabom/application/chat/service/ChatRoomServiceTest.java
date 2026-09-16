package mannabom_server.manabom.application.chat.service;

import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberQueryRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.application.meeting.service.MeetingMemberService;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMemberQueryRepository chatMemberQueryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private MeetingMemberService meetingMemberService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Test
    void allowsReentryWhenPreviousMembershipIsDeactivated() {
        MeetingMatch match = mock(MeetingMatch.class);
        User user = user(1L);
        ChatRoom room = ChatRoom.builder().id(77L).match(match).build();
        when(chatRoomRepository.findByMatch(match)).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomIdAndUser_UserIdAndStatus(
                77L,
                1L,
                ChatMemberStatus.ACTIVATE
        )).thenReturn(false);

        Long roomId = chatRoomService.joinMatchingChatRoom(match, user);

        ArgumentCaptor<ChatMember> captor = ArgumentCaptor.forClass(ChatMember.class);
        verify(chatMemberRepository).save(captor.capture());
        assertThat(roomId).isEqualTo(77L);
        assertThat(captor.getValue().getRoom()).isSameAs(room);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getStatus()).isEqualTo(ChatMemberStatus.ACTIVATE);
    }

    @Test
    void rejectsEntryWhenActiveMembershipAlreadyExists() {
        MeetingMatch match = mock(MeetingMatch.class);
        User user = user(1L);
        ChatRoom room = ChatRoom.builder().id(77L).match(match).build();
        when(chatRoomRepository.findByMatch(match)).thenReturn(Optional.of(room));
        when(chatMemberRepository.existsByRoomIdAndUser_UserIdAndStatus(
                77L,
                1L,
                ChatMemberStatus.ACTIVATE
        )).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.joinMatchingChatRoom(match, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해당 매칭 채팅방");

        verify(chatMemberRepository, never()).save(any());
    }

    private User user(Long id) {
        return User.builder()
                .userId(id)
                .kakaoId("mock_" + id)
                .userName("user" + id)
                .build();
    }
}

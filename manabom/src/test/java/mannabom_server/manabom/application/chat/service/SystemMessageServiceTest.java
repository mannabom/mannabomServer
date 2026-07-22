package mannabom_server.manabom.application.chat.service;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.application.notification.service.NotificationService;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemMessageServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private MeetingMatchRepository meetingMatchRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private SystemMessageService systemMessageService;

    @BeforeEach
    void persistPassedMessage() {
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordsSingleTypedEventAndPushesToRecipientOutsideRoom() {
        ChatRoom room = ChatRoom.builder().id(77L).build();
        when(chatRoomRepository.findById(77L)).thenReturn(Optional.of(room));
        when(profileRepository.findByUser_UserId(1L))
                .thenReturn(Optional.of(Profile.builder().nickName("봄이").build()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:location:2")).thenReturn("99");

        ChatSystemMessageEvent source = ChatSystemMessageEvent.of(
                77L,
                SystemMessageType.PHOTO_REQUESTED,
                1L,
                List.of(2L),
                Map.of("status", "RECEIVED")
        );
        SystemMessageService.RecordedSystemMessage recorded =
                systemMessageService.recordForRoom(source);
        systemMessageService.dispatch(recorded);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).saveAndFlush(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getType()).isEqualTo(ChatMessageType.SYSTEM);
        assertThat(messageCaptor.getValue().getUser()).isNull();
        assertThat(recorded.message().getActorUserId()).isEqualTo(1L);
        assertThat(recorded.message().getRecipientUserIds()).containsExactly(2L);
        assertThat(recorded.message().getSystemEventType()).isEqualTo("PHOTO_REQUESTED");
        assertThat(recorded.message().getSystemTitle()).isEqualTo("봄이님이 프로필 공개를 요청했어요");
        assertThat(recorded.message().getActorNickname()).isEqualTo("봄이");
        assertThat(recorded.message().getContent()).contains("서로의 프로필이 공개됩니다");
        verify(messagingTemplate).convertAndSend("/topic/rooms/77", recorded.message());
        verify(notificationService).sendNotification(
                org.mockito.ArgumentMatchers.eq(2L),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void recordsMatchFoundMessageForBothTeamRooms() {
        Meeting meeting1 = Meeting.builder().id(10L).build();
        Meeting meeting2 = Meeting.builder().id(11L).build();
        MeetingMatch match = org.mockito.Mockito.mock(MeetingMatch.class);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(meetingMatchRepository.findByIdWithMeeting(20L)).thenReturn(Optional.of(match));
        when(chatRoomRepository.findByMeeting_Id(10L))
                .thenReturn(Optional.of(ChatRoom.builder().id(100L).meeting(meeting1).build()));
        when(chatRoomRepository.findByMeeting_Id(11L))
                .thenReturn(Optional.of(ChatRoom.builder().id(101L).meeting(meeting2).build()));

        List<SystemMessageService.RecordedSystemMessage> events =
                systemMessageService.recordMatchFound(20L, Instant.now());

        assertThat(events).extracting(event -> event.message().getRoomId())
                .containsExactly(100L, 101L);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.message().getSystemTitle()).isEqualTo("미팅할 상대 팀을 찾았어요‼️✨");
            assertThat(event.message().getContent()).contains("수락 가능 시간 안에");
        });
    }

    @Test
    void recordsDifferentMessagesForRejectingAndOpponentTeams() {
        Meeting meeting1 = Meeting.builder().id(10L).build();
        Meeting meeting2 = Meeting.builder().id(11L).build();
        MeetingMatch match = org.mockito.Mockito.mock(MeetingMatch.class);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(match.getMeeting1Decision()).thenReturn(MeetingDecision.REJECTED);
        when(meetingMatchRepository.findByIdWithMeeting(20L)).thenReturn(Optional.of(match));
        when(chatRoomRepository.findByMeeting_Id(10L))
                .thenReturn(Optional.of(ChatRoom.builder().id(100L).meeting(meeting1).build()));
        when(chatRoomRepository.findByMeeting_Id(11L))
                .thenReturn(Optional.of(ChatRoom.builder().id(101L).meeting(meeting2).build()));

        List<SystemMessageService.RecordedSystemMessage> events =
                systemMessageService.recordMatchFailure(20L, 1L, false);

        assertThat(events.get(0).message().getContent()).contains("팀장 사용자님이 매칭을 거절했어요");
        assertThat(events.get(1).message().getContent()).contains("상대 팀과 연결되지 않았어요");
    }

    @Test
    void recordsTimeoutMessagesForBothTeams() {
        Meeting meeting1 = Meeting.builder().id(10L).build();
        Meeting meeting2 = Meeting.builder().id(11L).build();
        MeetingMatch match = org.mockito.Mockito.mock(MeetingMatch.class);
        when(match.getMeeting1()).thenReturn(meeting1);
        when(match.getMeeting2()).thenReturn(meeting2);
        when(match.getMeeting2Decision()).thenReturn(MeetingDecision.AUTO_REJECTED);
        when(meetingMatchRepository.findByIdWithMeeting(20L)).thenReturn(Optional.of(match));
        when(chatRoomRepository.findByMeeting_Id(10L))
                .thenReturn(Optional.of(ChatRoom.builder().id(100L).meeting(meeting1).build()));
        when(chatRoomRepository.findByMeeting_Id(11L))
                .thenReturn(Optional.of(ChatRoom.builder().id(101L).meeting(meeting2).build()));

        List<SystemMessageService.RecordedSystemMessage> events =
                systemMessageService.recordMatchFailure(20L, null, true);

        assertThat(events.get(0).message().getRoomId()).isEqualTo(101L);
        assertThat(events.get(0).message().getContent()).contains("자동으로 거절됐어요");
        assertThat(events.get(1).message().getRoomId()).isEqualTo(100L);
        assertThat(events.get(1).message().getContent()).contains("상대 팀의 응답 시간이 지나");
    }
}

package mannabom_server.manabom.application.chat.service;

import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageContent;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMessageType;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemMessageServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private MeetingMatchRepository meetingMatchRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private SystemMessageService systemMessageService;

    @BeforeEach
    void persistPassedMessage() {
        when(chatMessageRepository.saveAndFlush(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordsAndBroadcastsSystemMessageWithoutSender() {
        ChatRoom room = ChatRoom.builder().id(77L).build();
        when(chatRoomRepository.findById(77L)).thenReturn(Optional.of(room));

        ChatMessageEvent event = systemMessageService.recordForRoom(77L, "시스템 알림");
        systemMessageService.broadcast(event);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).saveAndFlush(messageCaptor.capture());
        ChatMessage savedMessage = messageCaptor.getValue();
        assertThat(savedMessage.getType()).isEqualTo(ChatMessageType.SYSTEM);
        assertThat(savedMessage.getUser()).isNull();
        assertThat(savedMessage.getContent()).isEqualTo("시스템 알림");
        assertThat(event.getSenderUserId()).isNull();
        assertThat(event.getMessageType()).isEqualTo("SYSTEM");
        verify(messagingTemplate).convertAndSend("/topic/rooms/77", event);
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

        List<ChatMessageEvent> events = systemMessageService.recordMatchFound(20L);

        assertThat(events).extracting(ChatMessageEvent::getRoomId).containsExactly(100L, 101L);
        assertThat(events).allSatisfy(event ->
                assertThat(event.getContent()).isEqualTo(SystemMessageContent.MATCH_FOUND));
    }

    @Test
    void recordsDifferentMessagesForRejectingAndRejectedTeams() {
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

        List<ChatMessageEvent> events = systemMessageService.recordMatchFailure(20L, false);

        assertThat(events.get(0).getContent()).isEqualTo(SystemMessageContent.MATCH_REJECTED_BY_LEADER);
        assertThat(events.get(1).getContent()).isEqualTo(SystemMessageContent.MATCH_REJECTED_BY_OPPONENT);
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

        List<ChatMessageEvent> events = systemMessageService.recordMatchFailure(20L, true);

        assertThat(events.get(0).getRoomId()).isEqualTo(101L);
        assertThat(events.get(0).getContent()).isEqualTo(SystemMessageContent.MATCH_TIMED_OUT);
        assertThat(events.get(1).getRoomId()).isEqualTo(100L);
        assertThat(events.get(1).getContent()).isEqualTo(SystemMessageContent.OPPONENT_MATCH_TIMED_OUT);
    }
}

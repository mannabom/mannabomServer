package mannabom_server.manabom.application.chat.handler;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.service.SystemMessageService;
import mannabom_server.manabom.application.meeting.dto.request.MeetingMatchingEvent;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSystemMessageEventHandlerTest {

    @Mock
    private SystemMessageService systemMessageService;

    @InjectMocks
    private ChatSystemMessageEventHandler handler;

    @Test
    void connectsMatchingStartedEventToSystemMessage() {
        MeetingMatchingEvent source = MeetingMatchingEvent.builder().meetingId(10L).build();
        ChatMessageEvent message = message(100L, "매칭 시작");
        when(systemMessageService.recordMatchingStarted(10L)).thenReturn(message);

        handler.handleMatchingStarted(source);

        verify(systemMessageService).broadcast(message);
    }

    @Test
    void connectsMatchFoundEventToBothTeamMessages() {
        MatchFoundEvent source = new MatchFoundEvent(20L, Instant.now());
        List<ChatMessageEvent> messages = List.of(
                message(100L, "상대 팀 발견"),
                message(101L, "상대 팀 발견")
        );
        when(systemMessageService.recordMatchFound(20L)).thenReturn(messages);

        handler.handleMatchFound(source);

        verify(systemMessageService).broadcast(messages);
    }

    @Test
    void connectsTimeoutFailureEventToTeamMessages() {
        MatchFailureEvent source = new MatchFailureEvent(20L, null, true);
        List<ChatMessageEvent> messages = List.of(
                message(100L, "시간 초과"),
                message(101L, "상대 팀 시간 초과")
        );
        when(systemMessageService.recordMatchFailure(20L, true)).thenReturn(messages);

        handler.handleMatchFailure(source);

        verify(systemMessageService).broadcast(messages);
    }

    @Test
    void connectsRoomEventToSystemMessage() {
        ChatSystemMessageEvent source = new ChatSystemMessageEvent(77L, "프로필 요청");
        ChatMessageEvent message = message(77L, "프로필 요청");
        when(systemMessageService.recordForRoom(77L, "프로필 요청")).thenReturn(message);

        handler.handleRoomSystemMessage(source);

        verify(systemMessageService).broadcast(message);
    }

    private ChatMessageEvent message(Long roomId, String content) {
        return ChatMessageEvent.builder()
                .roomId(roomId)
                .messageType("SYSTEM")
                .content(content)
                .build();
    }
}

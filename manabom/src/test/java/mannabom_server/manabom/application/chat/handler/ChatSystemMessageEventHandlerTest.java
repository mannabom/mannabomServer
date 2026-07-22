package mannabom_server.manabom.application.chat.handler;

import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.application.chat.service.SystemMessageService;
import mannabom_server.manabom.application.notification.dto.MatchFailureEvent;
import mannabom_server.manabom.application.notification.dto.MatchFoundEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSystemMessageEventHandlerTest {

    @Mock private SystemMessageService systemMessageService;
    @InjectMocks private ChatSystemMessageEventHandler handler;

    @Test
    void connectsMatchFoundEventToBothTeamMessages() {
        Instant deadline = Instant.now();
        MatchFoundEvent source = new MatchFoundEvent(20L, deadline);
        List<SystemMessageService.RecordedSystemMessage> messages = List.of(recorded(100L));
        when(systemMessageService.recordMatchFound(20L, deadline)).thenReturn(messages);

        handler.handleMatchFound(source);

        verify(systemMessageService).dispatch(messages);
    }

    @Test
    void connectsTimeoutFailureEventToTeamMessages() {
        MatchFailureEvent source = new MatchFailureEvent(20L, null, true);
        List<SystemMessageService.RecordedSystemMessage> messages = List.of(recorded(100L));
        when(systemMessageService.recordMatchFailure(20L, null, true)).thenReturn(messages);

        handler.handleMatchFailure(source);

        verify(systemMessageService).dispatch(messages);
    }

    @Test
    void connectsRoomEventToSingleTypedMessage() {
        ChatSystemMessageEvent source = ChatSystemMessageEvent.of(
                77L,
                SystemMessageType.PHOTO_REQUESTED,
                1L,
                List.of(2L),
                Map.of("status", "RECEIVED")
        );
        SystemMessageService.RecordedSystemMessage message = recorded(77L);
        when(systemMessageService.recordForRoom(source)).thenReturn(message);

        handler.handleRoomSystemMessage(source);

        verify(systemMessageService).dispatch(message);
    }

    private SystemMessageService.RecordedSystemMessage recorded(Long roomId) {
        ChatMessageEvent message = ChatMessageEvent.builder()
                .roomId(roomId)
                .recipientUserIds(List.of())
                .messageType("SYSTEM")
                .build();
        return new SystemMessageService.RecordedSystemMessage(
                message,
                SystemMessageType.MATCH_FOUND,
                true
        );
    }
}

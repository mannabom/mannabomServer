package mannabom_server.manabom.application.chat.handler;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.event.ChatRoomLeaveEvent;
import mannabom_server.manabom.application.meeting.service.MeetingMatchingService;
import mannabom_server.manabom.application.meeting.service.MeetingService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingChatEventHandler {
    private final MeetingService meetingService;
    private final MeetingMatchingService meetingMatchingService;

    @EventListener
    public void handleChatRoomLeave(ChatRoomLeaveEvent event){
        switch (event.roomType()){
            case MEETING_GROUP -> {
                meetingService.handleMemberLeave(event.referenceId(), event.userId());
            }
            case MEETING_MATCH -> {
                meetingMatchingService.handleMatchMemberLeave(event.referenceId(), event.userId());
            }
        }
    }
}

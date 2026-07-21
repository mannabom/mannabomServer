package mannabom_server.manabom.application.chat.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.response.ChatMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageContent;
import mannabom_server.manabom.domain.chat.entity.ChatMessage;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatMessageRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MeetingMatchRepository meetingMatchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessageEvent recordForRoom(Long roomId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));
        return save(room, content);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessageEvent recordMatchingStarted(Long meetingId) {
        return saveForMeeting(meetingId, SystemMessageContent.MATCHING_STARTED);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ChatMessageEvent> recordMatchFound(Long matchId) {
        MeetingMatch match = findMatch(matchId);
        return List.of(
                saveForMeeting(match.getMeeting1().getId(), SystemMessageContent.MATCH_FOUND),
                saveForMeeting(match.getMeeting2().getId(), SystemMessageContent.MATCH_FOUND)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ChatMessageEvent> recordMatchFailure(Long matchId, boolean isByTimeout) {
        MeetingMatch match = findMatch(matchId);
        Meeting failedMeeting = findFailedMeeting(match, isByTimeout);
        Meeting opponentMeeting = failedMeeting.getId().equals(match.getMeeting1().getId())
                ? match.getMeeting2()
                : match.getMeeting1();

        String failedTeamMessage = isByTimeout
                ? SystemMessageContent.MATCH_TIMED_OUT
                : SystemMessageContent.MATCH_REJECTED_BY_LEADER;
        String opponentMessage = isByTimeout
                ? SystemMessageContent.OPPONENT_MATCH_TIMED_OUT
                : SystemMessageContent.MATCH_REJECTED_BY_OPPONENT;

        return List.of(
                saveForMeeting(failedMeeting.getId(), failedTeamMessage),
                saveForMeeting(opponentMeeting.getId(), opponentMessage)
        );
    }

    public void broadcast(ChatMessageEvent event) {
        messagingTemplate.convertAndSend("/topic/rooms/" + event.getRoomId(), event);
    }

    public void broadcast(List<ChatMessageEvent> events) {
        events.forEach(this::broadcast);
    }

    private MeetingMatch findMatch(Long matchId) {
        return meetingMatchRepository.findByIdWithMeeting(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅 매칭입니다."));
    }

    private Meeting findFailedMeeting(MeetingMatch match, boolean isByTimeout) {
        MeetingDecision failedDecision = isByTimeout
                ? MeetingDecision.AUTO_REJECTED
                : MeetingDecision.REJECTED;

        if (match.getMeeting1Decision() == failedDecision) {
            return match.getMeeting1();
        }
        if (match.getMeeting2Decision() == failedDecision) {
            return match.getMeeting2();
        }
        throw new IllegalStateException("거절한 미팅 팀을 확인할 수 없습니다.");
    }

    private ChatMessageEvent saveForMeeting(Long meetingId, String content) {
        ChatRoom room = chatRoomRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("미팅 채팅방이 존재하지 않습니다."));
        return save(room, content);
    }

    private ChatMessageEvent save(ChatRoom room, String content) {
        ChatMessage message = chatMessageRepository.saveAndFlush(ChatMessage.system(room, content));
        return ChatMessageEvent.builder()
                .roomId(room.getId())
                .senderUserId(null)
                .messageType(message.getType().name())
                .content(message.getContent())
                .messageId(message.getId())
                .clientMessageId(null)
                .sendAt(message.getCreatedAt())
                .build();
    }
}

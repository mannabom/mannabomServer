package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingCancellationExpirationService {

    private final MeetingCancellationRequestRepository requestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(Long requestId, Instant now) {
        MeetingCancellationRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElse(null);

        return request != null && expire(request, now);
    }

    @Transactional
    public boolean expire(MeetingCancellationRequest request, Instant now) {
        if (!request.isExpiredAt(now)) {
            return false;
        }

        request.expire(now);
        ChatRoom room = chatRoomRepository.findByMatch(request.getMeetingMatch())
                .orElseThrow(() -> new IllegalStateException("매칭 채팅방이 존재하지 않습니다."));
        List<Long> recipients = activeMemberUserIds(request.getMeetingMatch());
        Map<String, Object> data = new HashMap<>();
        if (request.getId() != null) {
            data.put("requestId", request.getId());
        }
        data.put("status", request.getStatus().name());
        data.put("expiresAt", request.getExpiresAt().toString());
        eventPublisher.publishEvent(ChatSystemMessageEvent.of(
                room.getId(),
                SystemMessageType.MEETING_CANCELLATION_EXPIRED,
                null,
                recipients,
                data
        ));
        return true;
    }

    private List<Long> activeMemberUserIds(MeetingMatch match) {
        List<MeetingMember> members = new ArrayList<>();
        members.addAll(meetingMemberRepository.findByMeetingIdAndStatus(
                match.getMeeting1().getId(),
                ChatUserStatus.ACTIVE
        ));
        members.addAll(meetingMemberRepository.findByMeetingIdAndStatus(
                match.getMeeting2().getId(),
                ChatUserStatus.ACTIVE
        ));

        return members.stream()
                .map(MeetingMember::getUser)
                .map(user -> user.getUserId())
                .distinct()
                .toList();
    }
}

package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.domain.chat.entity.ChatMember;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingCancellationExpirationService {

    private final MeetingCancellationRequestRepository requestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(Long requestId, Instant now) {
        MeetingCancellationRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElse(null);

        if (request == null || !request.isExpiredAt(now)) {
            return false;
        }

        request.expire(now);
        ChatRoom room = chatRoomRepository.findByMatch(request.getMeetingMatch())
                .orElseThrow(() -> new IllegalStateException("매칭 채팅방이 존재하지 않습니다."));
        List<Long> recipients = chatMemberRepository
                .findAllByRoomIdAndStatus(room.getId(), ChatMemberStatus.ACTIVATE)
                .stream()
                .map(ChatMember::getUser)
                .map(user -> user.getUserId())
                .toList();
        Map<String, Object> data = new HashMap<>();
        if (request.getId() != null) {
            data.put("requestId", request.getId());
        }
        data.put("status", request.getStatus().name());
        eventPublisher.publishEvent(ChatSystemMessageEvent.of(
                room.getId(),
                SystemMessageType.MEETING_CANCELLATION_EXPIRED,
                null,
                recipients,
                data
        ));
        return true;
    }
}

package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MeetingCancellationExpirationService {

    private final MeetingCancellationRequestRepository requestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(Long requestId, Instant now) {
        MeetingCancellationRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElse(null);

        if (request == null || !request.isExpiredAt(now)) {
            return false;
        }

        request.expire(now);
        return true;
    }
}

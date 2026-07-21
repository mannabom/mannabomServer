package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.meeting.dto.response.MeetingCancellationResponse;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationVoteRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingCancellationService {

    private static final Duration CANCELLATION_DEADLINE = Duration.ofHours(24);

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final MeetingCancellationRequestRepository requestRepository;
    private final MeetingCancellationVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional(readOnly = true)
    public void validateNoPendingCancellation(Long meetingId) {
        if (requestRepository.existsByMeeting_IdAndStatus(
                meetingId,
                MeetingCancellationStatus.PENDING
        )) {
            throw new IllegalStateException(
                    "미팅 전체 취소 투표가 진행 중이므로 입장하거나 나갈 수 없습니다."
            );
        }
    }

    @Transactional
    public MeetingCancellationResponse create(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findByIdWithLock(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅방입니다."));
        validateActiveMember(meetingId, userId);

        if (meeting.getMeetingStatus() == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 미팅입니다.");
        }
        if (requestRepository.existsByMeeting_IdAndStatus(
                meetingId,
                MeetingCancellationStatus.PENDING
        )) {
            throw new IllegalStateException("이미 진행 중인 미팅 취소 요청이 있습니다.");
        }

        List<MeetingMember> members = activeMembers(meetingId);
        if (members.size() < 2) {
            throw new IllegalStateException("전체 취소 투표를 진행할 팀원이 없습니다.");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Instant now = Instant.now();
        MeetingCancellationRequest request = requestRepository.save(
                MeetingCancellationRequest.create(
                        meeting,
                        initiator,
                        now,
                        now.plus(CANCELLATION_DEADLINE)
                )
        );

        List<MeetingCancellationVote> votes = members.stream()
                .map(member -> member.getUser().getUserId().equals(userId)
                        ? MeetingCancellationVote.agreedByInitiator(request, member.getUser(), now)
                        : MeetingCancellationVote.pending(request, member.getUser()))
                .toList();
        voteRepository.saveAll(votes);

        return MeetingCancellationResponse.of(request, votes);
    }

    @Transactional
    public MeetingCancellationResponse vote(
            Long requestId,
            Long userId,
            CancellationVoteDecision decision
    ) {
        if (decision == null || decision == CancellationVoteDecision.PENDING) {
            throw new IllegalArgumentException("투표 결과는 AGREE 또는 REJECT여야 합니다.");
        }

        MeetingCancellationRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅 취소 요청입니다."));
        Instant now = Instant.now();
        expireIfNecessary(request, now);

        if (request.getStatus() != MeetingCancellationStatus.PENDING) {
            throw new IllegalStateException("이미 종료된 미팅 취소 요청입니다.");
        }

        MeetingCancellationVote vote = voteRepository
                .findByRequest_IdAndUser_UserId(requestId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 미팅의 투표 대상자가 아닙니다."));
        vote.decide(decision, now);

        if (decision == CancellationVoteDecision.REJECT) {
            request.reject(now);
        } else if (allMembersAgreed(requestId)) {
            approveCancellation(request, now);
        }

        return response(request);
    }

    @Transactional
    public MeetingCancellationResponse getCurrent(Long meetingId, Long userId) {
        validateActiveMember(meetingId, userId);
        MeetingCancellationRequest request = requestRepository
                .findByMeeting_IdAndStatus(meetingId, MeetingCancellationStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 미팅 취소 요청이 없습니다."));
        expireIfNecessary(request, Instant.now());
        return response(request);
    }

    @Transactional
    public int expirePendingRequests() {
        Instant now = Instant.now();
        List<MeetingCancellationRequest> expiredRequests = requestRepository
                .findAllByStatusAndExpiresAtLessThanEqual(
                        MeetingCancellationStatus.PENDING,
                        now
                );
        expiredRequests.forEach(request -> request.expire(now));
        return expiredRequests.size();
    }

    private void approveCancellation(
            MeetingCancellationRequest request,
            Instant now
    ) {
        request.approve(now);
        Meeting meeting = request.getMeeting();

        chatRoomRepository.findByMeeting(meeting)
                .ifPresent(chatRoom -> chatRoom.deactivate());
        activeMembers(meeting.getId()).forEach(MeetingMember::deactivate);
        meeting.cancelByAgreement();
    }

    private void expireIfNecessary(
            MeetingCancellationRequest request,
            Instant now
    ) {
        if (request.isExpiredAt(now)) {
            request.expire(now);
        }
    }

    private boolean allMembersAgreed(Long requestId) {
        return voteRepository.countByRequest_IdAndDecision(
                requestId,
                CancellationVoteDecision.PENDING
        ) == 0 && voteRepository.countByRequest_IdAndDecision(
                requestId,
                CancellationVoteDecision.REJECT
        ) == 0;
    }

    private MeetingCancellationResponse response(MeetingCancellationRequest request) {
        return MeetingCancellationResponse.of(
                request,
                voteRepository.findAllByRequest_IdOrderById(request.getId())
        );
    }

    private List<MeetingMember> activeMembers(Long meetingId) {
        return meetingMemberRepository.findByMeetingIdAndStatus(
                meetingId,
                ChatUserStatus.ACTIVE
        );
    }

    private void validateActiveMember(Long meetingId, Long userId) {
        if (!meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(
                meetingId,
                userId,
                ChatUserStatus.ACTIVE
        )) {
            throw new IllegalArgumentException("해당 미팅방의 참여자가 아닙니다.");
        }
    }
}

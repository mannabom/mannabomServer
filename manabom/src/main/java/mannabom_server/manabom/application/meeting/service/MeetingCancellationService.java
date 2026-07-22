package mannabom_server.manabom.application.meeting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mannabom_server.manabom.application.chat.dto.event.ChatSystemMessageEvent;
import mannabom_server.manabom.application.chat.message.SystemMessageType;
import mannabom_server.manabom.application.meeting.dto.response.MeetingCancellationResponse;
import mannabom_server.manabom.domain.chat.entity.ChatRoom;
import mannabom_server.manabom.domain.chat.enums.ChatMemberStatus;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.meeting.entity.Meeting;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationRequest;
import mannabom_server.manabom.domain.meeting.entity.MeetingCancellationVote;
import mannabom_server.manabom.domain.meeting.entity.MeetingMatch;
import mannabom_server.manabom.domain.meeting.entity.MeetingMember;
import mannabom_server.manabom.domain.meeting.enums.CancellationVoteDecision;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingCancellationStatus;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationRequestRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingCancellationVoteRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMatchRepository;
import mannabom_server.manabom.domain.meeting.repository.MeetingMemberRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingCancellationService {

    private static final Duration CANCELLATION_DEADLINE = Duration.ofHours(24);

    private final MeetingMatchRepository meetingMatchRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final MeetingCancellationRequestRepository requestRepository;
    private final MeetingCancellationVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MeetingCancellationExpirationService expirationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void validateNoPendingCancellation(Long meetingId) {
        var match = meetingMatchRepository.findByMeetingIdAndStatus(
                meetingId,
                MatchingStatus.SUCCEEDED
        );
        if (match.isEmpty()) {
            return;
        }

        if (requestRepository.existsByMeetingMatch_IdAndStatus(
                match.get().getId(),
                MeetingCancellationStatus.PENDING
        )) {
            throw new IllegalStateException(
                    "미팅 전체 취소 투표가 진행 중이므로 입장하거나 나갈 수 없습니다."
            );
        }
    }

    @Transactional
    public MeetingCancellationResponse create(Long matchId, Long userId) {
        MeetingMatch match = meetingMatchRepository.findByIdWithLockAndMeeting(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅 매칭입니다."));

        if (match.getMatchingStatus() != MatchingStatus.SUCCEEDED) {
            throw new IllegalStateException("성사된 미팅만 전체 취소를 요청할 수 있습니다.");
        }
        validateActiveParticipant(match, userId);

        if (requestRepository.existsByMeetingMatch_IdAndStatus(
                matchId,
                MeetingCancellationStatus.PENDING
        )) {
            throw new IllegalStateException("이미 진행 중인 미팅 취소 요청이 있습니다.");
        }

        List<MeetingMember> members = activeMembers(match);
        if (members.size() < 2) {
            throw new IllegalStateException("전체 취소 투표를 진행할 참여자가 부족합니다.");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Instant now = Instant.now();
        MeetingCancellationRequest request = requestRepository.save(
                MeetingCancellationRequest.create(
                        match,
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
        publishCancellationEvent(
                request,
                SystemMessageType.MEETING_CANCELLATION_VOTE_STARTED,
                userId,
                memberUserIds(members)
        );

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
        List<Long> recipientUserIds = memberUserIds(activeMembers(request.getMeetingMatch()));
        vote.decide(decision, now);

        if (decision == CancellationVoteDecision.REJECT) {
            request.reject(now);
            publishCancellationEvent(
                    request,
                    SystemMessageType.MEETING_CANCELLATION_REJECTED,
                    userId,
                    recipientUserIds
            );
        } else if (allMembersAgreed(requestId)) {
            approveCancellation(request, now);
            publishCancellationEvent(
                    request,
                    SystemMessageType.MEETING_CANCELLATION_APPROVED,
                    userId,
                    recipientUserIds
            );
        }

        return response(request);
    }

    @Transactional
    public MeetingCancellationResponse getCurrent(Long matchId, Long userId) {
        MeetingMatch match = meetingMatchRepository.findByIdWithMeeting(matchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 미팅 매칭입니다."));
        validateActiveParticipant(match, userId);
        MeetingCancellationRequest request = requestRepository
                .findByMeetingMatch_IdAndStatus(matchId, MeetingCancellationStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 미팅 취소 요청이 없습니다."));
        expireIfNecessary(request, Instant.now());
        return response(request);
    }

    public int expirePendingRequests() {
        Instant now = Instant.now();
        List<Long> expiredRequestIds = requestRepository
                .findExpiredRequestIds(
                        MeetingCancellationStatus.PENDING,
                        now
                );

        int expiredCount = 0;
        for (Long requestId : expiredRequestIds) {
            try {
                if (expirationService.expire(requestId, now)) {
                    expiredCount++;
                }
            } catch (RuntimeException e) {
                log.warn("미팅 취소 요청 만료 처리 실패: requestId={}", requestId, e);
            }
        }
        return expiredCount;
    }

    private void approveCancellation(
            MeetingCancellationRequest request,
            Instant now
    ) {
        request.approve(now);
        MeetingMatch match = request.getMeetingMatch();
        Meeting meeting1 = match.getMeeting1();
        Meeting meeting2 = match.getMeeting2();

        chatRoomRepository.findByMatch(match)
                .ifPresent(this::deactivateChatRoomAndMembers);
        chatRoomRepository.findByMeeting(meeting1)
                .ifPresent(this::deactivateChatRoomAndMembers);
        chatRoomRepository.findByMeeting(meeting2)
                .ifPresent(this::deactivateChatRoomAndMembers);

        activeMembers(match).forEach(MeetingMember::deactivate);
        meeting1.cancelByAgreement();
        meeting2.cancelByAgreement();
    }

    private void expireIfNecessary(
            MeetingCancellationRequest request,
            Instant now
    ) {
        if (request.isExpiredAt(now)) {
            List<Long> recipients = memberUserIds(activeMembers(request.getMeetingMatch()));
            request.expire(now);
            publishCancellationEvent(
                    request,
                    SystemMessageType.MEETING_CANCELLATION_EXPIRED,
                    null,
                    recipients
            );
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

    private List<MeetingMember> activeMembers(MeetingMatch match) {
        List<MeetingMember> members = new ArrayList<>();
        members.addAll(activeMembers(match.getMeeting1().getId()));
        members.addAll(activeMembers(match.getMeeting2().getId()));
        return members;
    }

    private List<MeetingMember> activeMembers(Long meetingId) {
        return meetingMemberRepository.findByMeetingIdAndStatus(
                meetingId,
                ChatUserStatus.ACTIVE
        );
    }

    private List<Long> memberUserIds(List<MeetingMember> members) {
        return members.stream()
                .map(member -> member.getUser().getUserId())
                .distinct()
                .toList();
    }

    private void publishCancellationEvent(
            MeetingCancellationRequest request,
            SystemMessageType type,
            Long actorUserId,
            List<Long> recipientUserIds
    ) {
        Long roomId = chatRoomRepository.findByMatch(request.getMeetingMatch())
                .map(ChatRoom::getId)
                .orElseThrow(() -> new IllegalStateException("매칭 채팅방이 존재하지 않습니다."));
        Map<String, Object> data = new HashMap<>();
        if (request.getId() != null) {
            data.put("requestId", request.getId());
        }
        data.put("status", request.getStatus().name());
        data.put("expiresAt", request.getExpiresAt().toString());

        eventPublisher.publishEvent(ChatSystemMessageEvent.of(
                roomId,
                type,
                actorUserId,
                recipientUserIds,
                data
        ));
    }

    private void validateActiveParticipant(MeetingMatch match, Long userId) {
        boolean belongsToMeeting1 = isActiveMember(match.getMeeting1().getId(), userId);
        boolean belongsToMeeting2 = isActiveMember(match.getMeeting2().getId(), userId);
        if (!belongsToMeeting1 && !belongsToMeeting2) {
            throw new IllegalArgumentException("해당 미팅 매칭의 참여자가 아닙니다.");
        }
    }

    private boolean isActiveMember(Long meetingId, Long userId) {
        return meetingMemberRepository.existsByMeeting_IdAndUser_UserIdAndStatus(
                meetingId,
                userId,
                ChatUserStatus.ACTIVE
        );
    }

    private void deactivateChatRoomAndMembers(ChatRoom room) {
        room.deactivate();
        chatMemberRepository.findAllByRoomIdAndStatus(
                room.getId(),
                ChatMemberStatus.ACTIVATE
        ).forEach(chatMember -> chatMember.deactivate());
    }
}

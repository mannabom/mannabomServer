package mannabom_server.manabom.application.report.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.report.dto.request.CreateChatReportRequest;
import mannabom_server.manabom.application.report.dto.request.CreateProfileReportRequest;
import mannabom_server.manabom.domain.chat.repository.ChatMemberRepository;
import mannabom_server.manabom.domain.chat.repository.ChatRoomRepository;
import mannabom_server.manabom.domain.report.entity.Report;
import mannabom_server.manabom.domain.report.entity.ReportReason;
import mannabom_server.manabom.domain.report.entity.ReportStatus;
import mannabom_server.manabom.domain.report.entity.ReportType;
import mannabom_server.manabom.domain.report.repository.ReportRepository;
import mannabom_server.manabom.domain.user.entity.Profile;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.ProfileRepository;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final List<ReportStatus> ACTIVE_REPORT_STATUSES =
            List.of(ReportStatus.RECEIVED, ReportStatus.UNDER_REVIEW);

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Transactional
    public Long createChatReport(Long userId, CreateChatReportRequest request) {
        User reporter = findReporterWithLock(userId);
        User target = userRepository.findById(request.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타겟 유저 입니다."));
        validateNotSelfReport(reporter, target);
        validateChatReport(userId, request.getTargetId(), request.getContextId());

        return findOrCreateReport(
                reporter,
                target,
                ReportType.CHAT,
                request.getContextId(),
                request.getReason(),
                request.getAdditionalDetail()
        );
    }

    @Transactional
    public Long createProfileReport(Long userId, CreateProfileReportRequest request) {
        User reporter = findReporterWithLock(userId);
        Profile targetProfile = profileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타겟 프로필입니다."));
        User target = targetProfile.getUser();
        validateNotSelfReport(reporter, target);

        return findOrCreateReport(
                reporter,
                target,
                ReportType.PROFILE,
                request.getProfileId(),
                request.getReason(),
                request.getAdditionalDetail()
        );
    }

    private User findReporterWithLock(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 입니다."));
    }

    private void validateNotSelfReport(User reporter, User target) {
        if (reporter.getUserId().equals(target.getUserId())) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        }
    }

    private Long findOrCreateReport(
            User reporter,
            User target,
            ReportType type,
            Long contextId,
            ReportReason reason,
            String additionalDetail
    ) {
        return reportRepository
                .findFirstByReporter_UserIdAndTarget_UserIdAndTypeAndContextIdAndStatusInOrderByIdAsc(
                        reporter.getUserId(),
                        target.getUserId(),
                        type,
                        contextId,
                        ACTIVE_REPORT_STATUSES
                )
                .map(Report::getId)
                .orElseGet(() -> reportRepository.save(Report.createReport(
                        reporter,
                        target,
                        type,
                        contextId,
                        reason,
                        additionalDetail
                )).getId());
    }

    private void validateChatReport(Long reporterId, Long targetId, Long roomId) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("존재하지 않는 채팅방입니다.");
        }
        if (!chatMemberRepository.existsByRoomIdAndUser_UserId(roomId, reporterId)) {
            throw new IllegalArgumentException("신고자가 해당 채팅방의 참여자가 아닙니다.");
        }
        if (!chatMemberRepository.existsByRoomIdAndUser_UserId(roomId, targetId)) {
            throw new IllegalArgumentException("신고 대상자가 해당 채팅방의 참여자가 아닙니다.");
        }
    }
}

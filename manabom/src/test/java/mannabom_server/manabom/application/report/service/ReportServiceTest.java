package mannabom_server.manabom.application.report.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository reportRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @InjectMocks private ReportService reportService;

    @Test
    void createsProfileReportUsingProfileIdAsContextAndReturnsId() {
        User reporter = user(1L);
        User target = user(2L);
        Profile targetProfile = Profile.builder().user(target).build();
        targetProfile.setProfileId(200L);
        CreateProfileReportRequest request = CreateProfileReportRequest.builder()
                .profileId(200L)
                .reason(ReportReason.INAPPROPRIATE_PROFILE)
                .additionalDetail("부적절한 소개입니다.")
                .build();

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reporter));
        when(profileRepository.findById(200L)).thenReturn(Optional.of(targetProfile));
        when(reportRepository
                .findFirstByReporter_UserIdAndTarget_UserIdAndTypeAndContextIdAndStatusInOrderByIdAsc(
                        eq(1L),
                        eq(2L),
                        eq(ReportType.PROFILE),
                        eq(200L),
                        anyCollection()
                )).thenReturn(Optional.empty());
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 300L);
            return report;
        });

        Long reportId = reportService.createProfileReport(1L, request);

        assertThat(reportId).isEqualTo(300L);
        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getTarget()).isEqualTo(target);
        assertThat(reportCaptor.getValue().getType()).isEqualTo(ReportType.PROFILE);
        assertThat(reportCaptor.getValue().getContextId()).isEqualTo(200L);
    }

    @Test
    void returnsExistingActiveReportIdForDuplicateChatReport() {
        User reporter = user(1L);
        User target = user(2L);
        Report existing = Report.createReport(
                reporter,
                target,
                ReportType.CHAT,
                100L,
                ReportReason.ABUSIVE_LANGUAGE,
                null
        );
        ReflectionTestUtils.setField(existing, "id", 300L);
        CreateChatReportRequest request = CreateChatReportRequest.builder()
                .contextId(100L)
                .targetId(2L)
                .reason(ReportReason.ABUSIVE_LANGUAGE)
                .build();

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(chatRoomRepository.existsById(100L)).thenReturn(true);
        when(chatMemberRepository.existsByRoomIdAndUser_UserId(100L, 1L)).thenReturn(true);
        when(chatMemberRepository.existsByRoomIdAndUser_UserId(100L, 2L)).thenReturn(true);
        when(reportRepository
                .findFirstByReporter_UserIdAndTarget_UserIdAndTypeAndContextIdAndStatusInOrderByIdAsc(
                        eq(1L),
                        eq(2L),
                        eq(ReportType.CHAT),
                        eq(100L),
                        anyCollection()
                )).thenReturn(Optional.of(existing));

        Long reportId = reportService.createChatReport(1L, request);

        assertThat(reportId).isEqualTo(300L);
        verify(reportRepository, never()).save(any(Report.class));
        verify(reportRepository)
                .findFirstByReporter_UserIdAndTarget_UserIdAndTypeAndContextIdAndStatusInOrderByIdAsc(
                        1L,
                        2L,
                        ReportType.CHAT,
                        100L,
                        java.util.List.of(ReportStatus.RECEIVED, ReportStatus.UNDER_REVIEW)
                );
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .kakaoId("kakao_" + userId)
                .userName("user" + userId)
                .build();
    }
}

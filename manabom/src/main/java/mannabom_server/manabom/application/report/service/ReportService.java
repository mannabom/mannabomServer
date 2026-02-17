package mannabom_server.manabom.application.report.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.application.report.dto.request.CreateReportRequest;
import mannabom_server.manabom.domain.report.entity.Report;
import mannabom_server.manabom.domain.report.entity.ReportType;
import mannabom_server.manabom.domain.report.repository.ReportRepository;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReport(Long userId, CreateReportRequest request, ReportType type) {
        if (userId.equals(request.getTargetId()))
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저 입니다."));
        User target = userRepository.findById(request.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 타겟 유저 입니다."));

        Report report = Report.createReport(
                user,
                target,
                type,
                request.getContextId(),
                request.getReason(),
                request.getAdditionalDetail()
        );
        reportRepository.save(report);
    }

    //특정 유저 신고 내역 조회
    //처리해야할 신고 리스트 조회
    //처리된 신고 리스트 조회
    //전체 신고 리스트 조회?
    //신고 처리하기



}

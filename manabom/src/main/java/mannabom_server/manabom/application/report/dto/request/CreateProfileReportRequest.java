package mannabom_server.manabom.application.report.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.report.entity.ReportReason;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileReportRequest {

    @NotNull(message = "신고 대상 프로필 ID는 필수입니다.")
    private Long profileId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 1000, message = "신고 상세 내용은 최대 1000자까지 입력할 수 있습니다.")
    private String additionalDetail;
}

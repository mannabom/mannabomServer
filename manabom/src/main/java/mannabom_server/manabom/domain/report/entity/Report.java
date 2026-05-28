package mannabom_server.manabom.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;
import mannabom_server.manabom.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;


@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    private Long contextId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private User target;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    private String additionalDetail;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.RECEIVED;



    private String adminComment;

    private Instant processedAt;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;


    public static Report createReport(User reporter, User target,ReportType type, Long contextId, ReportReason reason, String detail){
        return Report.builder()
                .reason(reason)
                .reporter(reporter)
                .target(target)
                .additionalDetail(detail)
                .contextId(contextId)
                .type(type)
                .build();
    }

    public void processReport(String adminComment, ReportStatus status){
        this.adminComment = adminComment;
        this.processedAt = Instant.now();
        this.status = status;
    }
}

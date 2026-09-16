package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatusConverter;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecision;
import mannabom_server.manabom.domain.meeting.enums.MeetingDecisionConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "meeting_matches")
public class MeetingMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "meeting1_id", nullable = false)
    private Meeting meeting1;
    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "meeting2_id", nullable = false)
    private Meeting meeting2;

    @Convert(converter = MeetingDecisionConverter.class)
    private MeetingDecision meeting1Decision;
    @Convert(converter = MeetingDecisionConverter.class)
    private MeetingDecision meeting2Decision;

    @Convert(converter = MatchingStatusConverter.class)
    private MatchingStatus matchingStatus;
    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    private Instant decisionDeadLine;

    @Builder
    public MeetingMatch(Meeting meeting1, Meeting meeting2) {
        this.meeting1 = meeting1;
        this.meeting2 = meeting2;

        this.meeting1Decision = MeetingDecision.WAITING;
        this.meeting2Decision = MeetingDecision.WAITING;

        this.matchingStatus = MatchingStatus.PENDING;
        this.decisionDeadLine = Instant.now().plus(1, ChronoUnit.DAYS);
    }

    public void reject(Long meetingId) {
        if (meeting1.getId().equals(meetingId)) {
            this.meeting1Decision = processRejection(meeting1, meeting1Decision);
        } else if (meeting2.getId().equals(meetingId)) {
            this.meeting2Decision = processRejection(meeting2, meeting2Decision);
        } else throw new IllegalArgumentException("해당 매칭에 속하지 않은 팀입니다.");
        updateStatus();
    }
    private MeetingDecision processRejection(Meeting meeting, MeetingDecision decision){
        if(!decision.equals(MeetingDecision.WAITING))
            throw new IllegalStateException("미팅 거절: 거절할 수 없는 상태입니다.");
        if (meeting.getRemainingRejectCount() > 0) {
            meeting.decreaseRejectCount();
            return MeetingDecision.REJECTED;
        } else return MeetingDecision.ACCEPTED;
    }


    public void accept(Long meetingId) {
        if (meeting1.getId().equals(meetingId)) {
            this.meeting1Decision = MeetingDecision.ACCEPTED;
        } else if (meeting2.getId().equals(meetingId)) {
            this.meeting2Decision = MeetingDecision.ACCEPTED;
        } else throw new IllegalArgumentException("해당 매칭에 속하지 않은 팀입니다.");
        updateStatus();
    }

    public void autoAccept(Long meetingId){
        if (meeting1.getId().equals(meetingId)) {
            this.meeting1Decision = MeetingDecision.AUTO_ACCEPTED;
        } else if (meeting2.getId().equals(meetingId)) {
            this.meeting2Decision = MeetingDecision.AUTO_ACCEPTED;
        } else throw new IllegalArgumentException("해당 매칭에 속하지 않은 팀입니다.");
        updateStatus();
    }

    private void updateStatus() {
        if ((meeting1Decision.equals(MeetingDecision.ACCEPTED)|| meeting1Decision.equals(MeetingDecision.AUTO_ACCEPTED)) && (meeting2Decision.equals(MeetingDecision.ACCEPTED)|| meeting2Decision.equals(MeetingDecision.AUTO_ACCEPTED))) {
            {
                this.matchingStatus = MatchingStatus.SUCCEEDED;
                this.meeting1.changeToMatchedStatus();
                this.meeting2.changeToMatchedStatus();
            }
        } else if (meeting2Decision.equals(MeetingDecision.REJECTED)||meeting2Decision.equals(MeetingDecision.AUTO_REJECTED) || meeting1Decision.equals(MeetingDecision.REJECTED)|| meeting1Decision.equals(MeetingDecision.AUTO_REJECTED)) {
            this.matchingStatus = MatchingStatus.FAILED;
        }
    }

    public void processExpiration(){
        if(meeting1Decision.equals(MeetingDecision.WAITING)){
            this.meeting1Decision = determineAutoDecision(meeting1);

        }
        if(meeting2Decision.equals(MeetingDecision.WAITING)){
            this.meeting2Decision = determineAutoDecision(meeting2);
        }
        updateStatus();
    }
    private MeetingDecision determineAutoDecision(Meeting meeting){
        if(meeting.getRemainingRejectCount()>0){
            meeting.decreaseRejectCount();
            return MeetingDecision.AUTO_REJECTED;
        }
        return MeetingDecision.AUTO_ACCEPTED;
    }


}

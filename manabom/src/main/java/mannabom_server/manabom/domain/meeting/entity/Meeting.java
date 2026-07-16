package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatus;
import mannabom_server.manabom.domain.meeting.enums.MeetingStatusConverter;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.enums.GenderConverter;
import org.hibernate.annotations.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;



@Entity
@NoArgsConstructor
@Builder//(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DynamicUpdate
@SQLDelete(sql = "UPDATE meeting SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Meeting extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomName;

    @Convert(converter = GenderConverter.class)
    private Gender gender;

    @ManyToOne
    @JoinColumn(name = "sigungu_code")
    private Region region;

    private Integer minAge;
    private Integer maxAge;

    private Integer maxMembers;
    private Integer currentMembers;

    @Convert(converter = MeetingStatusConverter.class)
    private MeetingStatus meetingStatus;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;


    @Column(name = "occupancy_score")
    @Builder.Default
    private Integer occupancyScore=0;

    private Instant matchingStartAt;

    @Builder.Default
    private Integer remainingRejectCount = 3;
    @Builder.Default
    private double avgAge=0.0;

    private Instant deletedAt;

    public static Meeting create(
            User createdBy,
            String roomName,
            Gender gender,
            Region region,
            int minAge,
            int maxAge,
            int maxMembers,
            String code,
            int userAge
    ){
        if (minAge > maxAge) throw new IllegalArgumentException("나이 범위가 올바르지 않습니다.");
        if (maxMembers < 2 || maxMembers > 4) throw new IllegalArgumentException("인원 수는 2~4명만 가능합니다.");

        Meeting meeting = Meeting.builder()
                .createdBy(createdBy)
                .roomName(roomName)
                .gender(gender)
                .region(region)
                .minAge(minAge)
                .maxAge(maxAge)
                .maxMembers(maxMembers)
                .meetingStatus(MeetingStatus.RECRUITING)
                .code(code)
                .currentMembers(1)
                .avgAge(userAge)
                .build();
        meeting.updateOccupancyScore();
        return meeting;
    }


    public void addMember(int userAge){
        double tmp = avgAge*currentMembers+userAge;
        currentMembers++;
        avgAge = tmp/currentMembers;
        if(currentMembers.equals(maxMembers)){
            meetingStatus = MeetingStatus.FULL;
        }
        updateOccupancyScore();
    }

    public void deleteMember(int userAge){
        if(currentMembers!=1){
            double tmp = avgAge*currentMembers-userAge;
            currentMembers--;
            avgAge = tmp/currentMembers;
            updateOccupancyScore();
        } else {
            this.currentMembers = 0;
            this.avgAge = 0.0;
        }
    }


    private void updateOccupancyScore(){
        if(this.maxMembers==0){
            this.occupancyScore =0;
            return;
        }
        this.occupancyScore = (int)(((double)this.currentMembers*10000)/this.maxMembers);
    }


    public void startMatching(){
        if(this.meetingStatus != MeetingStatus.FULL){
            throw new IllegalArgumentException("매칭 시작: 아직 인원이 충분히 모집되지 않았습니다.");
        }
        this.matchingStartAt = Instant.now();
        this.meetingStatus = MeetingStatus.MATCHING_WAITING;
    }

    public void cancelMatching(){
        if (this.matchingStartAt == null) {
            throw new IllegalStateException("매칭 취소: 매칭이 시작되지 않았습니다.");
        }
        if (this.meetingStatus != MeetingStatus.MATCHING_WAITING) {
            throw new IllegalStateException("매칭 취소: 현재 상태에서는 취소할 수 없습니다. status=" + this.meetingStatus);
        }

        Instant now = Instant.now();
        Instant cancelableAt = this.matchingStartAt.plus(24,ChronoUnit.HOURS);
        if(now.isBefore(cancelableAt)){
            Duration remaining = Duration.between(now, cancelableAt);
            throw new IllegalStateException("매칭 취소: 아직 24시간이 지나지 않아 취소할 수 없습니다. 남은 시간:" + formatRemaining(remaining));

        }
        this.matchingStartAt= null;
        this.meetingStatus = MeetingStatus.FULL;
    }

    private String formatRemaining(Duration d){
        long totalSeconds = d.getSeconds();

        long hours = totalSeconds /3600;
        long minutes = (totalSeconds % 3600) /60;
        long seconds = totalSeconds % 60;

        if(hours >0)
            return String.format("%d시간 %d분 %d초",hours,minutes,seconds);
        if(minutes>0)
            return String.format("%d분 %d초", minutes, seconds);
        return seconds+"초";

    }

    public void decreaseRejectCount(){
        if(this.remainingRejectCount>0)
            this.remainingRejectCount--;
        else throw new IllegalStateException("더이상 거절 할 수 없습니다.");
    }
    public void restoreStatus(){
        if (this.meetingStatus != MeetingStatus.MATCHING_PENDING) {
            throw new IllegalStateException("매칭 대기: 현재 상태에서는 대기할 수 없습니다. status=" + this.meetingStatus);
        }
        this.meetingStatus= MeetingStatus.MATCHING_WAITING;
    }
    public void changeToPendingStatus(){
        if (this.meetingStatus != MeetingStatus.MATCHING_WAITING) {
            throw new IllegalStateException("매칭 답변 대기: 현재 상태에서는 대기할 수 없습니다. status=" + this.meetingStatus);
        }
        this.meetingStatus= MeetingStatus.MATCHING_PENDING;
    }
    public void changeToMatchedStatus(){
        if (this.meetingStatus != MeetingStatus.MATCHING_PENDING) {
            throw new IllegalStateException("매칭 성공: 현재 상태에서는 매칭 완료할 수 없습니다. status=" + this.meetingStatus);
        }
        this.meetingStatus= MeetingStatus.MATCHED;
    }

    public void delete(){
        this.deletedAt = Instant.now();
    }

    public void cancelByAgreement() {
        if (meetingStatus == MeetingStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 미팅입니다.");
        }

        this.meetingStatus = MeetingStatus.CANCELLED;
        this.matchingStartAt = null;
        delete();
    }
}

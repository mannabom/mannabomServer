package mannabom_server.manabom.domain.meeting.entity;

import jakarta.persistence.*;
import lombok.*;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.meeting.enums.ChatUserStatus;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatus;
import mannabom_server.manabom.domain.meeting.enums.MatchingStatusConverter;
import mannabom_server.manabom.domain.user.entity.User;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.enums.GenderConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@ToString(exclude = "meetingMembers")
public class Meeting extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomName;

    @Convert(converter = GenderConverter.class)
    private Gender gender;

    private String regionSido;
    private String regionSigungu;

    private Integer minAge;
    private Integer maxAge;

    private Integer maxMembers;
    private Integer currentMembers;

    @Convert(converter = MatchingStatusConverter.class)
    private MatchingStatus matchingStatus;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "meeting")
    private Set<MeetingMember> meetingMembers = new HashSet<>();

    @Column(name = "occupancy_score", insertable = false, updatable = false)
    private Integer occupancyScore;

    public static Meeting create(
            User createdBy,
            String roomName,
            Gender gender,
            String regionSido,
            String regionSigungu,
            int minAge,
            int maxAge,
            int maxMembers,
            String code
    ){
        if (minAge > maxAge) throw new IllegalArgumentException("나이 범위가 올바르지 않습니다.");
        if (maxMembers < 2 || maxMembers > 4) throw new IllegalArgumentException("인원 수는 2~4명만 가능합니다.");

        return Meeting.builder()
                .createdBy(createdBy)
                .roomName(roomName)
                .gender(gender)
                .regionSido(regionSido)
                .regionSigungu(regionSigungu)
                .minAge(minAge)
                .maxAge(maxAge)
                .maxMembers(maxMembers)
                .matchingStatus(MatchingStatus.RECRUITING)
                .code(code)
                .currentMembers(1)
                .build();
    }


    public void addMember(){
        currentMembers++;
        if(currentMembers.equals(maxMembers)){
            matchingStatus= MatchingStatus.FULL;
        }
    }

    public List<MeetingMember> getActiveMembers(){
        return this.meetingMembers.stream()
                .filter(mm-> mm.getStatus()== ChatUserStatus.ACTIVE).toList();
    }

}

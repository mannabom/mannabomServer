package mannabom_server.manabom.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import mannabom_server.manabom.domain.common.BaseTimeEntity;
import mannabom_server.manabom.domain.user.enums.BodyType;
import mannabom_server.manabom.domain.user.enums.DrinkingHabit;
import mannabom_server.manabom.domain.user.enums.Gender;
import mannabom_server.manabom.domain.user.enums.SmokingHabit;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 상세 프로필 엔터티
 * 회원가입 시 추가로 입력하는 상세 정보들
 */
@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "profileImages")
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    // 연관관계 설정
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "height")
    private Integer height;

    @Enumerated(EnumType.STRING)                        // ERD랑 달라진 부분: 화면설계서에 추가되었음
    @Column(name = "body_type")
    private BodyType bodyType;

    @Column(name = "region_sido")                       // ERD랑 달라진 부분: 시/도 + 구 분리
    private String regionSido;

    @Column(name = "region_sigungu")
    private String regionSigungu;

    @Column(name = "nick_name", unique = true)          // 앱에서 사용할 닉네임 (중복X)
    private String nickName;

    @Column(name = "grade")                             // 별점 (1.0~5.0)
    private Double grade;

    @Column(name = "birth_date")                        // 생년월일
    private LocalDate birthDate;

    @Column(name = "mbti", length = 4)
    private String mbti;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcohol")
    private DrinkingHabit alcohol;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking")
    private SmokingHabit smoking;

    @Column(name = "university")                        // 대학명
    private String university;

    @Column(name = "email")                             // 대학 인증용 이메일
    private String email;

    @BatchSize(size = 8)
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProfileImage> profileImages = new ArrayList<>();

    // 질문들은 question으로 통합 Type으로 구분.
//    private String intro;
//    private String attractivePartnerTrait;
//    private String desiredPartnerTrait;

    @Builder
    public Profile(User user, Gender gender, Integer height, BodyType bodyType,
                   String regionSido, String regionSigungu, String nickName, Double grade,
                   LocalDate birthDate, String mbti, DrinkingHabit alcohol,
                   SmokingHabit smoking, String university, String email) {
        this.user = user;
        this.gender = gender;
        this.height = height;
        this.bodyType = bodyType;
        this.regionSido = regionSido;
        this.regionSigungu = regionSigungu;
        this.nickName = nickName;
        this.grade = grade != null ? grade : 0.0;
        this.birthDate = birthDate;
        this.mbti = mbti;
        this.alcohol = alcohol;
        this.smoking = smoking;
        this.university = university;
        this.email = email;
    }

    /**
     * 닉네임 설정
     */
    public void updateNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * 이메일 설정
     */
    public void updateEmail(String email) {
        this.email = email;
    }



    /**
    * 대표 사진
    */
    public String extractMainImageUrl(){
        if(this.profileImages== null || this.profileImages.isEmpty()){
            return null;
        }
        return this.profileImages.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .map(ProfileImage::getUrl)
                .orElseGet(()-> this.profileImages.get(0).getUrl());
    }



    /**
     * 나이 반환
     * */
    public int computeAge(){
        return LocalDate.now().getYear() - this.getBirthDate().getYear()+1;
    }

}

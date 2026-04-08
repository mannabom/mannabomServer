package mannabom_server.manabom.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import mannabom_server.manabom.domain.common.BaseTimeEntity;

/**
 * 프로필 이미지 엔터티
 */
@Entity
@Table(name = "profile_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    @JsonIgnore
    private Profile profile;

    @Column(name = "url", nullable = false)
    private String url;                         // 접근 가능한 URL

    @Column(name = "file_name", nullable = false)
    private String fileName;                    // 실제 저장 파일명

    @Column(name = "original_name")
    private String originalName;                // 사용자 업로드 원본명

    @Setter
    @Column(name = "image_index", nullable = false)
    private Integer imageIndex;                 // 표시 순서

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;             // 대표사진 여부

    @Builder
    public ProfileImage(Profile profile, String url, String fileName, String originalName,
                        Integer imageIndex, Boolean isMain) {
        this.profile = profile;
        this.url = url;
        this.fileName = fileName;
        this.originalName = originalName;
        this.imageIndex = imageIndex;
        this.isMain = isMain != null ? isMain : false;
    }

    public void setAsMain() {
        this.isMain = true;
    }

    public void unsetAsMain() {
        this.isMain = false;
    }

    public void updateImageIndex(Integer imageIndex) {
        this.imageIndex = imageIndex;
    }
}

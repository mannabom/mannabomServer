package mannabom_server.manabom.application.signup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@NoArgsConstructor
public class ProfilePhotosRequestDto {

    @NotBlank(message = "프로필 ID는 필수입니다.")
    private String profileId;

    @NotEmpty(message = "최소 1장의 사진을 업로드해야 합니다.")
    @Size(max = 10, message = "최대 10장까지 업로드 가능합니다.")
    private List<MultipartFile> photos;

    public ProfilePhotosRequestDto(String profileId, List<MultipartFile> photos) {
        this.profileId = profileId;
        this.photos = photos;
    }
}
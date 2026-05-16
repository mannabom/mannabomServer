package mannabom_server.manabom.application.userInfo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeleteUserPhotoRequest {
    @NotNull(message = "photoId는 필수입니다.")
    private Long photoId;
}

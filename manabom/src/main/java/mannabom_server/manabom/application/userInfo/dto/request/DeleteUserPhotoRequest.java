package mannabom_server.manabom.application.userInfo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DeleteUserPhotoRequest {
    private Long photoId;
}

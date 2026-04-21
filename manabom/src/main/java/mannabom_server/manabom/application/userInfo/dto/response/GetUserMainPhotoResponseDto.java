package mannabom_server.manabom.application.userInfo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetUserMainPhotoResponseDto {
    private final String photoURL;
}

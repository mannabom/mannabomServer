package mannabom_server.manabom.application.userInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetUserMainPhotoResponseDto {
    private final String photoURL;
}

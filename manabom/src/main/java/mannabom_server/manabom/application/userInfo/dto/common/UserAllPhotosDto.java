package mannabom_server.manabom.application.userInfo.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class UserAllPhotosDto {
    private List<Photo> photos;

    @Getter
    @AllArgsConstructor
    public static class Photo{
        private Long id;
        private Integer index;
        private String url;
    }
}

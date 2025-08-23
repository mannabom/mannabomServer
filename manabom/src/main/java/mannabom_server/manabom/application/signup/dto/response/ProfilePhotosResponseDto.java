package mannabom_server.manabom.application.signup.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProfilePhotosResponseDto {
    private boolean success;
    private ProfilePhotosDataDto data;
    private String message;

    @Getter
    @Builder
    public static class ProfilePhotosDataDto {
        private List<UploadedPhotoDto> uploadedPhotos;
    }

    @Getter
    @Builder
    public static class UploadedPhotoDto {
        private String photoId;
        private String url;
    }
}
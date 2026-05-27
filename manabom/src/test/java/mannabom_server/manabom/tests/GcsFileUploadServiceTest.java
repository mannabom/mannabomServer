package mannabom_server.manabom.tests;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import mannabom_server.manabom.infrastructure.storage.GcsFileUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GcsFileUploadServiceTest {

    private final Storage storage = mock(Storage.class);
    private final GcsFileUploadService service = new GcsFileUploadService(
            storage,
            "mannabom-prod-storage",
            "https://storage.googleapis.com/mannabom-prod-storage/"
    );

    @Test
    void uploadFileStoresImageInGcsAndReturnsPublicUrl() throws Exception {
        MockMultipartFile file = imageFile("profile.png", 120, 120);
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);

        String url = service.uploadFile(file, "profiles");

        verify(storage).create(blobInfoCaptor.capture(), any(byte[].class));
        BlobInfo blobInfo = blobInfoCaptor.getValue();

        assertThat(blobInfo.getBucket()).isEqualTo("mannabom-prod-storage");
        assertThat(blobInfo.getName()).startsWith("profiles/");
        assertThat(blobInfo.getName()).endsWith(".png");
        assertThat(blobInfo.getContentType()).isEqualTo("image/png");
        assertThat(url).isEqualTo("https://storage.googleapis.com/mannabom-prod-storage/" + blobInfo.getName());
    }

    @Test
    void deleteFileDeletesObjectExtractedFromPublicUrl() {
        when(storage.delete(BlobId.of("mannabom-prod-storage", "profiles/a.png"))).thenReturn(true);

        boolean deleted = service.deleteFile("https://storage.googleapis.com/mannabom-prod-storage/profiles/a.png");

        assertThat(deleted).isTrue();
        verify(storage).delete(BlobId.of("mannabom-prod-storage", "profiles/a.png"));
    }

    @Test
    void presignedGetUrlUsesGcsSignedUrl() throws Exception {
        URL signedUrl = new URL("https://signed.example.com/profiles/a.png");
        when(storage.signUrl(
                any(BlobInfo.class),
                anyLong(),
                any(TimeUnit.class),
                any(Storage.SignUrlOption.class)
        )).thenReturn(signedUrl);

        String url = service.presignedGetUrl("profiles/a.png", Duration.ofMinutes(10));

        assertThat(url).isEqualTo("https://signed.example.com/profiles/a.png");
    }

    @Test
    void presignedGetUrlFailsWhenGcsSigningFails() {
        when(storage.signUrl(
                any(BlobInfo.class),
                anyLong(),
                any(TimeUnit.class),
                any(Storage.SignUrlOption.class)
        )).thenThrow(new IllegalStateException("signing unavailable"));

        assertThatThrownBy(() -> service.presignedGetUrl("profiles/a.png", Duration.ofMinutes(10)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("파일 접근 URL 생성에 실패했습니다.");
    }

    @Test
    void uploadFileRejectsSmallImage() throws Exception {
        MockMultipartFile file = imageFile("small.png", 80, 80);

        assertThatThrownBy(() -> service.uploadFile(file, "profiles"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("파일 업로드에 실패했습니다.");
    }

    @Test
    @EnabledIfSystemProperty(named = "gcsUploadTest", matches = "true")
    void uploadFileStoresImageInRealGcsBucket() throws Exception {
        String bucketName = System.getProperty("gcsUploadTest.bucket", "mannabom-prod-storage");
        String baseUrl = System.getProperty(
                "gcsUploadTest.baseUrl",
                "https://storage.googleapis.com/" + bucketName + "/"
        );
        Storage realStorage = StorageOptions.getDefaultInstance().getService();
        GcsFileUploadService realService = new GcsFileUploadService(realStorage, bucketName, baseUrl);
        MockMultipartFile file = imageFile("codex-app-code-upload-test.png", 120, 120);

        String url = realService.uploadFile(file, "diagnostics/app-code");
        String objectName = realService.extractKeyFromUrl(url);
        BlobInfo uploaded = realStorage.get(BlobId.of(bucketName, objectName));

        assertThat(url).startsWith(baseUrl + "diagnostics/app-code/");
        assertThat(url).endsWith(".png");
        assertThat(uploaded).isNotNull();
        assertThat(uploaded.getContentType()).isEqualTo("image/png");
        assertThat(uploaded.getSize()).isGreaterThan(0);

        System.out.println("Real GCS upload test object: " + url);
    }

    private MockMultipartFile imageFile(String fileName, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);

        return new MockMultipartFile(
                "file",
                fileName,
                "image/png",
                out.toByteArray()
        );
    }
}

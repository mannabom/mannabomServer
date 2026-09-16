package mannabom_server.manabom.domain.gifticon.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class GifticonStatusDescriptionTest {

    @Test
    void everyGifticonStatusHasDescription() {
        assertThat(Arrays.stream(GifticonPaymentStatus.values())
                .map(GifticonPaymentStatus::getDescription))
                .allMatch(description -> description != null && !description.isBlank());
        assertThat(Arrays.stream(GifticonOrderStatus.values())
                .map(GifticonOrderStatus::getDescription))
                .allMatch(description -> description != null && !description.isBlank());
        assertThat(Arrays.stream(GifticonMessageCreationStatus.values())
                .map(GifticonMessageCreationStatus::getDescription))
                .allMatch(description -> description != null && !description.isBlank());
    }
}

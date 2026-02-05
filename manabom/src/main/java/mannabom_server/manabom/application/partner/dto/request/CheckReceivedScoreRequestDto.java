package mannabom_server.manabom.application.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckReceivedScoreRequestDto {
    @NotNull
    private Long targetProfileId;
}

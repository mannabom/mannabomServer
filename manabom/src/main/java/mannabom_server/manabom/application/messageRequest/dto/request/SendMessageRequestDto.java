package mannabom_server.manabom.application.messageRequest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SendMessageRequestDto {
    @NotNull
    private Long targetProfileId;

    private String message;
}

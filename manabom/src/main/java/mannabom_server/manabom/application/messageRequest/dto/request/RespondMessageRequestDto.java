package mannabom_server.manabom.application.messageRequest.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RespondMessageRequestDto {
    @NotNull(message = "messageRequestId가 비어있습니다.")
    private Long messageRequestId;
    @NotNull
    private Boolean accepted;

    private String rejectReason;
}

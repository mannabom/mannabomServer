package mannabom_server.manabom.application.messageRequest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.messageRequest.enums.MessageSource;

@Getter
@NoArgsConstructor
public class SendMessageRequestDto {
    @NotNull(message = "targetProfileId는 필수입니다.")
    private Long targetProfileId;

    @NotNull(message = "source는 필수입니다.")
    private MessageSource source;

    @Size(max = 200, message = "message는 200자를 초과할 수 없습니다.")
    private String message;
}

package mannabom_server.manabom.application.like.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RespondLikeRequestDto {
    @NotNull(message = "likeRequestId가 비어있습니다.")
    private Long likeRequestId;

    @NotNull(message = "accepted가 비어있습니다.")
    private Boolean accepted;

    private String rejectReason;
}

package mannabom_server.manabom.application.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetTargetProfileDetailRequestDto {
    @NotNull(message = "상대방의 프로필 아이디가 비어있습니다.")
    private Long targetProfileId;
}

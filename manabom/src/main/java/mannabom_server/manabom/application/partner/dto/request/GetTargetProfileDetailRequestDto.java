package mannabom_server.manabom.application.partner.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetTargetProfileDetailRequestDto {
    @NotNull
    private Long targetProfileId;
}

package mannabom_server.manabom.application.pushService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 디바이스 토큰 요청 Dto
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenUpsertRequest{

    @NotBlank(message = "디바이스 토큰이 비어있습니다.")
    private String deviceToken;
}


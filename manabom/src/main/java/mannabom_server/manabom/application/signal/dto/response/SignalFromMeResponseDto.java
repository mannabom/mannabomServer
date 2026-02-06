package mannabom_server.manabom.application.signal.dto.response;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class SignalFromMeResponseDto {
    List<SignalFromMeProfileDto> profiles;
}

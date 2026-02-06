package mannabom_server.manabom.application.signal.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SignalToMeResponseDto {
    private List<SignalToMeProfileDto> profiles;
}

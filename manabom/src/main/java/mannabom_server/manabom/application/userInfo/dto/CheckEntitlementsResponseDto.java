package mannabom_server.manabom.application.userInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class CheckEntitlementsResponseDto {
    private final Boolean isMembership;
    private final LocalDateTime membershipActiveUntil;
    private final Boolean isVip;
}

package mannabom_server.manabom.application.partner.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mannabom_server.manabom.domain.messageRequest.enums.MessageRequestStatus;

@Getter
@AllArgsConstructor
public class MessagedDto {
    boolean sent;
    MessageRequestStatus messageStatus;
}

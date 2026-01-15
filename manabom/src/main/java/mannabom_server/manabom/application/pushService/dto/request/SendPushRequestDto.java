package mannabom_server.manabom.application.pushService.dto.request;

import jakarta.mail.event.MailEvent;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;

/**
 * push 알람 요청 Dto
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendPushRequestDto {
    @NotBlank(message = "제목(title)이 비어있습니다")
    String title;
    @NotBlank(message = "본문(body)가 비어있습니다")
    String body;
}

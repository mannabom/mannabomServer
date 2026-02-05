package mannabom_server.manabom.application.messageRequest.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
public class SendMessageResponseDto {
    private int freeLikeNum;
    private int freeMessageNum;
    private int eventTingNum;
    private int tingNum;
    private int freeProfileNum;
    private int freeLoveViewNum;
    private int additionalProfileNum;
}

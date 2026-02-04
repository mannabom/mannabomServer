package mannabom_server.manabom.application.like.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SendLikeResponseDto {
    private int freeLikeNum;
    private int freeMessageNum;
    private int eventTingNum;
    private int tingNum;
    private int freeProfileNum;
    private int freeLoveViewNum;
    private int additionalProfileNum;
}

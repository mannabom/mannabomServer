package mannabom_server.manabom.application.currency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckTingWalletResponseDto {
    private int freeLikeNum;
    private int freeMessageNum;
    private int eventTingNum;
    private int tingNum;
    private int freeProfileNum;
    private int freeLoveViewNum;
    private int additionalProfileNum;
}

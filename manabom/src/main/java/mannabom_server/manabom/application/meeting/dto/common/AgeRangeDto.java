package mannabom_server.manabom.application.meeting.dto.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgeRangeDto{
    @Min(value = 20, message = "최소 나이는 20세 이상이어야 합니다.")
    @Max(value = 29,message = "나이 입력값이 너무 큽니다.")
    private Integer min;

    @Min(value = 20, message = "최소 나이는 20세 이상이어야 합니다.")
    @Max(value = 29,message = "나이 입력값이 너무 큽니다.")
    private Integer max;
}

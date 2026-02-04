package mannabom_server.manabom.application.meeting.dto.common;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mannabom_server.manabom.domain.region.entity.Region;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RegionDto {
    @NotBlank(message = "시/도는 필수입니다.")
    private String regionSido;

    @NotBlank(message = "구는 필수입니다.")
    private String regionSigungu;

    public static RegionDto from(Region region){
        return new RegionDto(region.getSidoName(), region.getSigunguName());
    }
}

package mannabom_server.manabom.application.region.service;

import lombok.RequiredArgsConstructor;
import mannabom_server.manabom.domain.region.entity.Region;
import mannabom_server.manabom.domain.region.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {
    private final RegionRepository regionRepository;

    public Region resolveRegion(String sido, String sigungu){
        String finalsigungu = "상관없음".equals(sigungu) ? "전체" : sigungu;

        return regionRepository
                .findBySidoNameAndSigunguName(sido, finalsigungu)
                .orElseThrow(()-> new IllegalArgumentException(
                        String.format("유효하지 않은 지역입니다. (시도: %s, 시군구: %s)", sido, finalsigungu)
                ));
    }
}

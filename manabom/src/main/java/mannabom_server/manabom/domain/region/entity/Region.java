package mannabom_server.manabom.domain.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "regions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {
    @Id
    @Column(length = 5)
    private String sigunguCode;

    @Column(length = 2, nullable = false)
    private String sidoCode;
    private String sidoName;
    private String sigunguName;




}

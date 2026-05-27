package mannabom_server.manabom.domain.admin.repository;

import mannabom_server.manabom.domain.admin.entity.UserAccountRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRestrictionRepository extends JpaRepository<UserAccountRestriction, Long> {
}

package mannabom_server.manabom.policy.repository;

import mannabom_server.manabom.policy.entity.PolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyConfigRepository extends JpaRepository<PolicyConfig, Long> {
}

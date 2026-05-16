package mannabom_server.manabom.domain.inquiry.repository;

import mannabom_server.manabom.domain.inquiry.entity.UserInquiry;
import mannabom_server.manabom.domain.inquiry.enums.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserInquiryRepository extends JpaRepository<UserInquiry, Long> {
    List<UserInquiry> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<UserInquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status);
    List<UserInquiry> findAllByOrderByCreatedAtDesc();
}

package sparta.paymentsystemserver.domain.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.payment.entity.PaymentWebhookEvent;

import java.util.Optional;

// 웹훅 이벤트 저장소. 같은 webhookId가 여러 번 들어오더라도 한 번만 처리하기 위해 사용함
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {

    // webhookId 기준 단건 조회
    Optional<PaymentWebhookEvent> findByWebhookId(String webhookId);

    // webhookId 중복 존재 여부 확인
    boolean existsByWebhookId(String webhookId);
}

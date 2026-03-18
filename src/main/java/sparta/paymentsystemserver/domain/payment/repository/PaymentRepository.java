package sparta.paymentsystemserver.domain.payment.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.payment.entity.Payment;

import java.util.Optional;

// 결제 생성 단계에서 사용할 레포지토리
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // paymentId로 결제를 조회할 때 order랑 user도 같이 가져옴
    // 공개 결제 ID로 조회
    @EntityGraph(attributePaths = {"order", "user"})
    Optional<Payment> findByPaymentId(String paymentId);
}

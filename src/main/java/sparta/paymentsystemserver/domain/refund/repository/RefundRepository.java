package sparta.paymentsystemserver.domain.refund.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.refund.entity.Refund;

import java.util.Optional;

// 환불 이력을 저장합니다. 하나의 결제에 대한 환불은 최대 1건 기준 조회
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // 결제id 기준으로 환불 이력 조회
    Optional<Refund> findByPaymentId(Long paymentId);

    // 이미 환불 이력이 존재하는지 확인
    boolean existsByPaymentId(Long paymentId);

    Optional<Refund> findByRefundId(String refundId);
}

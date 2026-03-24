package sparta.paymentsystemserver.domain.payment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;

// 결제 생성 단계에서 사용할 레포지토리
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // paymentId로 결제를 조회할 때 order랑 user도 같이 가져옴
    // 공개 결제 ID로 조회
    @EntityGraph(attributePaths = {"order", "user"})
    Optional<Payment> findByPaymentId(String paymentId);

    // 주문에 연결된 READY 상태인 결제들을 조회
    List<Payment> findAllByOrder_IdAndStatus(Long orderId, PaymentStatus status);

    // 결제 확정 시 동일 payment에 대한 동시 처리 충돌을 막기 위해 비관적 락으로 조회한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order join fetch p.user where p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);
}

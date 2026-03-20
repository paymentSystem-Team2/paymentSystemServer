package sparta.paymentsystemserver.domain.point.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.point.entity.PointTransaction;
import sparta.paymentsystemserver.domain.point.entity.PointTransactionType;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {

//    환불 시 해당 주문의 특정 타입 조회
    Optional<PointTransaction> findByOrderAndType(Order order, PointTransactionType pointTransactionType);

//    거래 내역 최신순 조회
    List<PointTransaction> findByUserOrderByCreatedAtDesc(User user);

//    특정 유저의 유효한 EARNED를 만료일 순으로 조회
    @Query("""
           SELECT pt
           FROM PointTransaction pt
           WHERE pt.user = :user
           AND pt.type = 'EARNED'
           AND pt.expirationProcessedAt IS NULL
           ORDER BY pt.expiresAt ASC
           """)
    List<PointTransaction> findEarnedByUserOrderByExpiresAt(@Param("user") User user);

//    만료 스케쥴링
    @Query("""
           SELECT pt
           FROM PointTransaction pt
           WHERE pt.type = 'EARNED'
           AND pt.expiresAt < :now
           AND pt.expirationProcessedAt IS NULL
           """)
    List<PointTransaction> findExpiredEarnedPoints(@Param("now") LocalDateTime now);
}

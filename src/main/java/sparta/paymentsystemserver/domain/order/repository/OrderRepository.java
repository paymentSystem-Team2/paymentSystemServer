package sparta.paymentsystemserver.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 외부용 주문 ID로 조회
    Optional<Order> findByOrderId(String orderId);

    // 주문번호로 조회
    Optional<Order> findByOrderNumber(String orderNumber);

    // 로그인한 사용자의 주문 목록 조회 (최신순)
    List<Order> findByUserIdOrderByOrderedAtDesc(Long userId);

    List<Order> findByStatusAndCompletedAtBefore(OrderStatus orderStatus, LocalDateTime threshold);
}

package sparta.paymentsystemserver.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.order.entity.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 외부용 주문 ID로 조회
    Optional<Order> findByOrderId(String orderId);

    // 주문번호로 조회
    Optional<Order> findByOrderNumber(String orderNumber);

    // 내 주문 목록 조회 (마이페이지)
    List<Order> findByUserId(Long userId);

}

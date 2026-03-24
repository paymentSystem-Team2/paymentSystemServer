package sparta.paymentsystemserver.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    @Query("""
        select oi
        from OrderItem oi
        join fetch oi.product
        where oi.order = :order
    """)
    List<OrderItem> findWithProductByOrder(@Param("order") Order order);
}

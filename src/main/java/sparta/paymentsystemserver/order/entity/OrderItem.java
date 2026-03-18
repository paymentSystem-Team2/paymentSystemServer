package sparta.paymentsystemserver.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.product.entity.Product;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // 상품(참조용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 수량
    @Column(nullable = false)
    private Long quantity;

    // 주문상품명 스냅샷
    @Column(nullable = false)
    private String productName;

    // 주문상품 가격 스냅샷
    @Column(nullable = false)
    private Long productPrice;

    // 생성일시
    @Column(nullable = false)
    private LocalDateTime orderedAt;

}

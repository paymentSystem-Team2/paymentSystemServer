package sparta.paymentsystemserver.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import sparta.paymentsystemserver.domain.product.entity.Product;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "order_items")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
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
    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime orderedAt;

    public OrderItem(Order order, Product product, Long quantity, String productName, Long productPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.productName = productName;
        this.productPrice = productPrice;
    }

}

package sparta.paymentsystemserver.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sparta.paymentsystemserver.domain.product.exception.ProductStockException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

@Entity
@Getter
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Long stock;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(nullable = false)
    private String category;

//    @ElementCollection
//    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
//    @Column(name = "image_url")
//    private List<String> productImages = new ArrayList<>();

    public Product(
            String productId,
            String name,
            Long price,
            Long stock,
            String description,
            ProductStatus status,
            String category
    ) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.status = status;
        this.category = category;
    }

    // 재고 감소
    public void decreaseStock(Long quantity){
        if (quantity == null || quantity <= 0) {
            throw new ProductStockException(ErrorCode.INVALID_QUANTITY);
        }

        if (this.stock < quantity) {
            throw new ProductStockException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        this.stock -= quantity;

        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    // 재고 복구
    public void increaseStock(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new ProductStockException(ErrorCode.INVALID_QUANTITY);
        }

        this.stock += quantity;

        if (this.status == ProductStatus.SOLD_OUT && this.stock > 0) {
            this.status = ProductStatus.ON_SALE;
        }
    }

}


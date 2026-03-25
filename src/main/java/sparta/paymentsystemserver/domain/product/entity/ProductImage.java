package sparta.paymentsystemserver.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Product product;

    @Column(nullable = false)
    private String fileKey;

    @Column
    private Integer sortOrder;

    @Column
    private Boolean thumbnail;

    @CreatedDate
    private LocalDateTime createdAt;

    public ProductImage(Product product, String fileKey, Integer sortOrder, boolean thumbnail) {
        this.product = product;
        this.fileKey = fileKey;
        this.sortOrder = sortOrder;
        this.thumbnail = thumbnail;
    }
}

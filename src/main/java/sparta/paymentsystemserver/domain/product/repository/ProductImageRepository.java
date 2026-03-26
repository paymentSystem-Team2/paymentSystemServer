package sparta.paymentsystemserver.domain.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.entity.ProductImage;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findAllByProduct(Product product);

    Optional<ProductImage> findByProductAndThumbnail(Product product, boolean thumbnail);
}

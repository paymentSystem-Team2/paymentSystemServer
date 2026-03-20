package sparta.paymentsystemserver.domain.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.entity.ProductStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(String productId);

    // 주문 목록 조회
    List<Product> findByStatusOrderByNameAsc(ProductStatus status);

    List<Product> findAllByProductIdIn(List<String> productId);

}

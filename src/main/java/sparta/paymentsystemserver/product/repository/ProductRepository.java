package sparta.paymentsystemserver.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.product.entity.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductId(String productId);
}

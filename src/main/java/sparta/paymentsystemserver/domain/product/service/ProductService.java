package sparta.paymentsystemserver.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.entity.ProductStatus;
import sparta.paymentsystemserver.domain.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> getProducts() {
        return productRepository.findByStatusOrderByNameAsc(ProductStatus.ON_SALE)
                .stream()
                .map(product -> new ProductResponse(
                        product.getProductId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStock()
                ))
                .toList();
    }
}

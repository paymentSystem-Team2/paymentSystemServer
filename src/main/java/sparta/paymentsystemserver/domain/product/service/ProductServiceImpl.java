package sparta.paymentsystemserver.domain.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.entity.ProductStatus;
import sparta.paymentsystemserver.domain.product.exception.ProductException;
import sparta.paymentsystemserver.domain.product.repository.ProductRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // 상품 목록 조회
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

    // 상품 상세 조회(상품ID로 조회)
    public GetProductDetailResponse getProductDetail(String productId){
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(()-> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

        // 조회한 상품 반환
        return new GetProductDetailResponse(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getStatus().name()
        );
    }

    public List<Product> getValidProductList(List<String> productIds){
        List<Product> products = productRepository.findAllByProductIdIn(productIds);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        List<String> missingIds = productIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return products;
    }
}

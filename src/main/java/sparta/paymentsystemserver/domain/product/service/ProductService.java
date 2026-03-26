package sparta.paymentsystemserver.domain.product.service;

import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.entity.Product;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getProducts();

    GetProductDetailResponse getProductDetail(String productId);

    List<Product> getValidProductList(List<String> productIds);
}

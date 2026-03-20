package sparta.paymentsystemserver.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.service.ProductServiceImpl;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductServiceImpl productServiceImpl;

    @GetMapping
    public List<ProductResponse> getProducts(){
        return productServiceImpl.getProducts();
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public GetProductDetailResponse getProductDetail(
            @PathVariable String productId){
        return productServiceImpl.getProductDetail(productId);
    }

}

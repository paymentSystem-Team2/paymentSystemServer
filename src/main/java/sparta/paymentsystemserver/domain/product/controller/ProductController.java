package sparta.paymentsystemserver.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.service.ProductService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductResponse> getProducts(){
        return productService.getProducts();
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public GetProductDetailResponse getProductDetail(
            @PathVariable String productId){
        return productService.getProductDetail(productId);
    }

    // 이미지 업로드
    @PostMapping("/{productId}/images")
    public ResponseEntity<Void> uploadProductImages(
            @PathVariable String productId,
            @RequestParam("images") List<MultipartFile> images
    ) {
        productService.uploadProductImages(productId, images);
        return ResponseEntity.ok().build();
    }

}

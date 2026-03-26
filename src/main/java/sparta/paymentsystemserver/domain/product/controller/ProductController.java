package sparta.paymentsystemserver.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.dto.ProductResponse;
import sparta.paymentsystemserver.domain.product.dto.UploadProductImageRequest;
import sparta.paymentsystemserver.domain.product.service.ProductImageService;
import sparta.paymentsystemserver.domain.product.service.ProductService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;

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

    @PostMapping(value = "/admin/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProductImages(
            // MultipartFile 을 body 에 json 방식으로는 받을 수 없음 form data 로 프론트에서 요청을 해주어야 한다
            // RequestParam 보다 확장성이 높고 쉽게 바인딩 할 수 있다.
            @ModelAttribute UploadProductImageRequest uploadProductImageRequest
    ) {
        productImageService.productImageUpload(uploadProductImageRequest);
        return ResponseEntity.ok().build();
    }

}

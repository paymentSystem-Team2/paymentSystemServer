package sparta.paymentsystemserver.domain.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sparta.paymentsystemserver.domain.product.dto.GetProductDetailResponse;
import sparta.paymentsystemserver.domain.product.entity.Product;
import sparta.paymentsystemserver.domain.product.entity.ProductStatus;
import sparta.paymentsystemserver.domain.product.exception.ProductException;
import sparta.paymentsystemserver.domain.product.repository.ProductRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static sparta.paymentsystemserver.global.exception.ErrorCode.PRODUCT_NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductImageService productImageService;

    @InjectMocks
    ProductServiceImpl productService;

    private String productId;
    private Product product;

    @BeforeEach
    void init() {
        productId = "P-001";
        product = new Product(
                productId,
                "랜덤 스쿱(문구류)",
                5000L,
                50L,
                "문구류 랜덤 스쿱",
                ProductStatus.ON_SALE,
                "STATIONERY"
        );
    }

    @Test
    public void 존재하는_상품_Id로_조회하면정상적인데이터가나온다() {
        given(productRepository.findByProductId(productId)).willReturn(Optional.of(product));

        GetProductDetailResponse result = productService.getProductDetail(productId);

        assertNotNull(result);
        assertEquals(productId, result.id());
    }

    @Test
    public void 존재하지_않은_상품_Id로_조회시_PNF_에러가_발생한다() {
        given(productRepository.findByProductId("NO_PROD_ID"))
                .willThrow(new ProductException(PRODUCT_NOT_FOUND));

        ProductException exception = assertThrows(ProductException.class, () -> {
            productService.getProductDetail("NO_PROD_ID");
        });
        assertEquals(PRODUCT_NOT_FOUND, exception.getErrorCode());
    }





}
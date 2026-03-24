package sparta.paymentsystemserver.domain.product.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import sparta.paymentsystemserver.domain.product.exception.ProductStockException;
import sparta.paymentsystemserver.global.exception.ErrorCode;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(
                "POD12345678",
                "테스트 상품",
                10000L,
                5L,
                "설명",
                ProductStatus.ON_SALE,
                "카테고리"
        );
    }

    @Test
    void 정상적으로_재고가_감소한다() {
        product.decreaseStock(3L);

        assertEquals(2L, product.getStock());
    }

    @Test
    void 재고가_0이되면_SOLD_OUT_상태가_된다() {
        product.decreaseStock(5L);

        assertEquals(0L, product.getStock());
        assertEquals(ProductStatus.SOLD_OUT, product.getStatus());
    }

    @Test
    public void 재고보다_많은_수량을_차감하려고_하면_PRODUCT_OUT_OF_STOCK_예외가_발생한다(){
        ProductStockException exception = assertThrows(ProductStockException.class, () -> {
            product.decreaseStock(999L);
        });

        assertEquals(ErrorCode.PRODUCT_OUT_OF_STOCK, exception.getErrorCode());
    }

    @Test
    public void 올바르지_않은_수량감소시_INVALID_QUANTITY_에러가_발생한다(){
        ProductStockException exception = assertThrows(ProductStockException.class, () -> {
            product.decreaseStock(-111L);
        });

        assertEquals(ErrorCode.INVALID_QUANTITY, exception.getErrorCode());
    }

    @Test
    public void Null_수량감소시_INVALID_QUANTITY_에러가_발생한다(){
        ProductStockException exception = assertThrows(ProductStockException.class, () -> {
            product.decreaseStock(null);
        });

        assertEquals(ErrorCode.INVALID_QUANTITY, exception.getErrorCode());
    }
}
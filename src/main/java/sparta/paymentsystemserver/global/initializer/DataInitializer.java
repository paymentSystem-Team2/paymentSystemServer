package sparta.paymentsystemserver.global.initializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            insertProducts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertProducts() {
        Long productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products",
                Long.class
        );

        if (productCount != null && productCount > 0) {
            return;
        }

        List<Object[]> batchArgs = List.of(
                new Object[]{"P-001", "랜덤 스쿱(문구류)", 5000L, 50L, "문구류 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-002", "랜덤 스쿱(전자기기)", 129000L, 100L, "전자기기 랜덤 스쿱", "ON_SALE", "ELECTRONICS"},
                new Object[]{"P-003", "랜덤 스쿱(의류)", 59000L, 70L, "의유 랜덤 스쿱", "ON_SALE", "CLOTHES"}
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO products (product_id, name, price, stock, description, status, category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                batchArgs
        );
    }
}
package sparta.paymentsystemserver.global.initializer;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            insertProducts();
            user();
            insertMembershipPolicies();
            insertPlans();
            insertProductImages();
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
                new Object[]{"P-001", "포켓몬", 5000L, 100L, "의유 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-002", "치이카와", 5000L, 50L, "문구류 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-003", "곽철이", 5000L, 100L, "전자기기 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-004", "산리오", 5000L, 100L, "의유 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-005", "가나디", 5000L, 100L, "의유 랜덤 스쿱", "ON_SALE", "STATIONERY"},
                new Object[]{"P-006", "파워퍼프걸", 5000L, 100L, "의유 랜덤 스쿱", "ON_SALE", "STATIONERY"}
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO products (product_id, name, price, stock, description, status, category) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)", // ?는 placeholder로 JDBC에서 SQL 인젝션 공격을 방지하기 위함(?의 게수 = batchArgs의 배열 원소 개수)
                batchArgs
        );
    }

    private void insertProductImages() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_images",
                Long.class
        );

        if (count != null && count > 0) return;

        Long p1 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-001"
        );

        Long p2 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-002"
        );

        Long p3 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-003"
        );
        Long p4 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-004"
        );
        Long p5 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-005"
        );
        Long p6 = jdbcTemplate.queryForObject(
                "SELECT id FROM products WHERE product_id = ?", Long.class, "P-006"
        );

        // product PK 조회
        List<Object[]> batchArgs = List.of(

                new Object[]{p1, "products/8e33f114-c753-428e-8b51-72a65fbb9eec_ppap.avif", 1, true},
                new Object[]{p1, "products/2846037c-8c3d-435c-8bef-bd2a44db6734_poo1.jpg", 2, false},
                new Object[]{p1, "products/47cc1c8c-4f47-4c42-834c-c4279b8fc228_05e71c776234a89ff5499151c45fc610.jpg", 3, false},
                new Object[]{p1, "products/51b35a20-cd50-4a3e-9802-4c60df418500_pooo.jpg", 4, false},

                new Object[]{p2, "products/14099339-dd7f-4613-8080-96a0ac6acc05_치이4.webp", 1, true},
                new Object[]{p2, "products/b6e4bc76-9645-4e14-85f3-f87f3efe5d64_치이1.avif", 2, false},
                new Object[]{p2, "products/e4f0ac4c-92bc-4c06-875a-d84d6ba4232c_치이2.webp", 3, false},
                new Object[]{p2, "products/aeb1f434-09fe-400f-9428-ca3920443e00_치이3.jpeg", 4, false},

                new Object[]{p3, "products/df9dae4b-c4c9-4552-bf2b-217421db6544_gak1.jpg", 1, true},
                new Object[]{p3, "products/7453d974-a5e9-4154-879c-77a3d0d2b6d0_gak2.jpg", 2, false},
                new Object[]{p3, "products/6c36b328-7a38-46a7-a525-63cfd8b4e21c_gak3.jpg", 3, false},

                new Object[]{p4, "products/9dd5e92b-01b8-464d-9f70-abca02dbf0f3_image (7).png", 1, true},
                new Object[]{p4, "products/461b17b7-b014-469b-9ec5-957c95c536c4_image (4).png", 2, false},
                new Object[]{p4, "products/36290935-14ad-40dc-872a-d0662e6177c4_image (5).png", 3, false},
                new Object[]{p4, "products/78c8836b-a394-4881-9c06-931aaaef958d_image (6).png", 4, false},

                new Object[]{p5, "products/6a9aba41-1e44-498f-9b1a-2f6c84869a90_image.png", 1, true},
                new Object[]{p5, "products/f17ec730-fd59-4de7-ba97-1d7fd90e0778_image (1).png", 2, false},
                new Object[]{p5, "products/0da803e8-1a71-4fdc-b80e-dd83fdcb599e_image (2).png", 3, false},
                new Object[]{p5, "products/d75b53c4-c0dd-493e-89ce-2bdeb07c655d_image (3).png", 4, false},

                new Object[]{p6, "products/1b467a11-05a8-447c-a678-1afd6f88f817_image (8).png", 1, true},
                new Object[]{p6, "products/c0946a45-6769-4e3f-acc3-64a6e3e1283e_image (9).png", 2, false},
                new Object[]{p6, "products/e7fdfda0-369d-46d6-9328-a2a1c23b1d92_image (11).png", 3, false}

        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO product_images (product_id, file_key, sort_order, thumbnail, created_at) " +
                        "VALUES (?, ?, ?, ?, NOW())",
                batchArgs
        );
    }

    private void user() {
        Long userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );

        if (userCount != null && userCount > 0) {
            return;
        }

        List<Object[]> batchArgs = List.of(
                new Object[]{
                        "admin",
                        "admin@test.com",
                        passwordEncoder.encode("admin"),
                        "cust_001",
                        999999L,
                        "NORMAL",
                        999999L,
                        "010-1111-1111",
                        "LOCAL",
                        "ADMIN"
                },
                new Object[]{
                        "user",
                        "user@test.com",
                        passwordEncoder.encode("user"),
                        "cust_002",
                        1000L,
                        "VIP",
                        300000L,
                        "010-2222-2222",
                        "LOCAL",
                        "USER"
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO users " +
                        "(name, email, password, customer_uid, point_balance, membership_grade, total_paid_amount, phone, provider, role) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batchArgs
        );
    }

    private void insertMembershipPolicies() {
        Long policyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM membership_policies",
                Long.class
        );

        if (policyCount != null && policyCount > 0) {
            return;
        }

        List<Object[]> batchArgs = List.of(
                new Object[]{"NORMAL", 0L, new BigDecimal("0.01")},
                new Object[]{"VIP", 100000L, new BigDecimal("0.03")},
                new Object[]{"VVIP", 300000L, new BigDecimal("0.05")}
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO membership_policies (membership_code, min_total_paid_amount, earn_rate) " +
                        "VALUES (?, ?, ?)",
                batchArgs
        );
    }

    private void insertPlans() {
        Long planCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plans",
                Long.class
        );

        if (planCount != null && planCount > 0) {
            return;
        }

        List<Object[]> batchArgs = List.of(
                new Object[]{"PLAN-BASIC", "Basic", 4900L, "MONTHLY", "월간 입문 구독 플랜", true},
                new Object[]{"PLAN-PRO", "Pro", 9900L, "MONTHLY", "월간 쩌는 구독 플랜", true},
                new Object[]{"PLAN-MAX", "Max", 19900L, "MONTHLY", "월간 프리미엄 구독 플랜", true}
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO plans (plan_id, name, amount, billing_cycle, description, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                batchArgs
        );
    }
}
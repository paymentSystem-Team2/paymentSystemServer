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
                        0L,
                        "NORMAL",
                        0L,
                        "010-1111-1111"
                },
                new Object[]{
                        "admin1",
                        "admin1@test.com",
                        passwordEncoder.encode("admin"),
                        "cust_002",
                        1000L,
                        "VIP",
                        300000L,
                        "010-2222-2222"
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO users " +
                        "(name, email, password, customer_uid, point_balance, membership_grade, total_paid_amount, phone) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
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
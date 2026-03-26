package sparta.paymentsystemserver.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.redis.RedisBlackListUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentConcurrencyTest {

    @Autowired private PaymentService paymentService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;

    // 외부 의존성 Mock 처리
    @MockitoBean private PortOnePaymentClient portOnePaymentClient;
    @MockitoBean private RedisRefreshTokenUtil redisRefreshTokenUtil;
    @MockitoBean private RedisBlackListUtil redisBlackListUtil;
    @MockitoBean private WebhookSignatureService webhookSignatureService;

    @Test
    @DisplayName("동시성 - 같은 결제를 동시에 확정 요청해도 한 번만 처리된다.")
    void confirmPayment_concurrency_idempotent() throws InterruptedException {
        // given - 유저, 주문, 결제 데이터 세팅
        User user = userRepository.save(
                new User("동시성테스트", "concurrent@test.com", "password",
                        "010-0000-0000", "USR-CONC-001")
        );

        user.addPoint(10_000L);
        userRepository.save(user);

        Order order = orderRepository.save(
                new Order("ORD-CONC-001", "ORD-NUM-CONC-001", user,
                        10_000L, OrderStatus.PENDING, LocalDateTime.now())
        );

        Payment payment = paymentRepository.save(
                Payment.ready("PAY-CONC-001", order, user,
                        PaymentProvider.INTERNAL, 10_000L, 10_000L, 0L, "KRW")
        );

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 5개 스레드가 동시에 같은 결제 확정 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentService.confirmPayment("PAY-CONC-001", user.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // then - 결제 상태가 PAID로 정확히 처리됨
        Payment result = paymentRepository.findByPaymentId("PAY-CONC-001").orElseThrow();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);

        // 전체 요청 수 = 성공 + 실패
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
}
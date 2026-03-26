package sparta.paymentsystemserver.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import sparta.paymentsystemserver.domain.payment.service.WebhookSignatureService;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.redis.RedisBlackListUtil;
import sparta.paymentsystemserver.global.redis.RedisRefreshTokenUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserPointConcurrencyTest {

    @Autowired private UserRepository userRepository;

    @MockitoBean private RedisRefreshTokenUtil redisRefreshTokenUtil;
    @MockitoBean private RedisBlackListUtil redisBlackListUtil;
    @MockitoBean private WebhookSignatureService webhookSignatureService;

    @Test
    @DisplayName("동시성 - 포인트 동시 차감 시 잔액이 음수가 되지 않는다.")
    void subtractPoint_concurrency_noNegativeBalance() throws InterruptedException {
        // given - 10000 포인트 보유 유저 생성
        User user = new User("포인트테스트", "point@test.com", "password",
                "010-1111-2222", "USR-POINT-001");
        user.addPoint(10_000L);
        userRepository.save(user);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when - 10개 스레드가 동시에 2000씩 차감 시도 (총 20000, 잔액은 10000)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    User found = userRepository.findById(user.getId()).orElseThrow();
                    found.subtractPoint(2_000L);
                    userRepository.save(found);
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

        // then - 잔액이 절대 음수가 되면 안 됨
        User result = userRepository.findById(user.getId()).orElseThrow();
        assertThat(result.getPointBalance()).isGreaterThanOrEqualTo(0L);
    }
}
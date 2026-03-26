package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentStatus;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.global.client.PortOnePaymentClient;
import sparta.paymentsystemserver.global.client.dto.PortOnePaymentInfo;

import java.time.LocalDateTime;
import java.util.List;

// 결제 대기 상태로 오래 방치된 주문을 정리하는 스케줄러. 주문 생성 때 미리 차감한 재고를 오래 두지 않게 일정 시간이 지난
// PENDING 주문이랑 READY 결제를 함께 만료 처리한다
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    // 결제 대기 유효 시간 주문 생성 후 이 시간 안에 confirm 되지 않으면 만료 대상으로 본다
    private static final long PAYMENT_TIMEOUT_MINUTES = 15L;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PortOnePaymentClient portOnePaymentClient;

    // 1분마다 만료 대상 주문을 검사
    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "paymentTimeoutScheduler_expirePendingOrders", lockAtMostFor = "PT55S", lockAtLeastFor = "PT5S")
    @Transactional
    public void expirePendingOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        // 아직 결제 확정되지 않은 주문 중 일정 시간 이상 지난 주문만 조회
        List<Order> expiredOrders = orderRepository.findByStatusAndOrderedAtBefore(OrderStatus.PENDING, threshold);

        for (Order order : expiredOrders) {
            expireOrder(order);
        }
    }

    // 주문 하나를 만료 처리 먼저 연결된 READY 결제를 기준으로 포트원 실제 결제 상태를 다시 확인
    // 이미 실제 결제가 완료된 주문이면 자동 컨펌을 시도하고 실제 결제가 되지 않은 주문만 취소/재고 복구로 정리
    private void expireOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        List<Payment> readyPayments = paymentRepository.findAllByOrder_IdAndStatus(order.getId(), PaymentStatus.READY);

        // READY 결제가 없으면 주문만 남은 비정상 케이스니까 주문 취소와 재고 복구만 수행
        if (readyPayments.isEmpty()) {
            order.cancel();
            restoreOrderStock(order);
            log.info("READY 결제 없이 남아 있던 PENDING 주문을 만료 처리했습니다. orderId={}, orderNumber={}",
                    order.getOrderId(), order.getOrderNumber());
            return;
        }

        for (Payment payment : readyPayments) {
            try {
                // 실제 포트원 결제 상태를 재조회해서 결제만 되고 confirm이 누락된 케이스를 구분
                PortOnePaymentInfo paymentInfo = portOnePaymentClient.getPayment(payment.getPaymentId());

                // 실제 결제가 이미 완료되었다면 서버에서 자동 컨펌 처리
                if (paymentInfo.isPaid()) {
                    paymentService.confirmPaymentByWebhook(payment.getPaymentId());
                    log.info("시간 초과 READY 결제를 PortOne 재조회 후 자동 확정했습니다. paymentId={}", payment.getPaymentId());
                    return;
                }
            } catch (Exception exception) {
                log.warn("시간 초과 READY 결제의 PortOne 재조회에 실패했습니다. paymentId={}", payment.getPaymentId(), exception);
                return;
            }
        }

        // 실제 결제가 확인되지 않은 경우에만 주문과 결제를 만료 처리
        order.cancel();

        for (Payment payment : readyPayments) {
            payment.expireReady("결제 대기 시간 초과로 자동 취소되었습니다.");
        }

        restoreOrderStock(order);

        log.info("결제 시간 초과 주문을 만료 처리했습니다. orderId={}, orderNumber={}", order.getOrderId(), order.getOrderNumber());
    }

    // 주문 생성 시 미리 차감한 재고를 원복 주문 단위로 한 번만 실행해서 중복 복구를 방지
    private void restoreOrderStock(Order order) {
        for (OrderItem orderItem : orderItemRepository.findWithProductByOrder(order)) {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
        }
    }
}


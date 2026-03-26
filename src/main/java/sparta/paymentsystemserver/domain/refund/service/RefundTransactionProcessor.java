package sparta.paymentsystemserver.domain.refund.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sparta.paymentsystemserver.domain.order.entity.OrderItem;
import sparta.paymentsystemserver.domain.order.repository.OrderItemRepository;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.point.service.PointService;

// 환불 완료 후에 내부 처리 담당
@Component
@RequiredArgsConstructor
public class RefundTransactionProcessor {
    private final PointService pointService;
    private final OrderItemRepository orderItemRepository;

    // 환불 완료 후 포인트, 멤버십, 재고 관련 상태를 되돌림
    public void processRefund(Payment payment) {
        Long userId = payment.getUser().getId();

        // 결제 당시 사용한 포인트가 있었다면 다시 복구
        pointService.restorePoints(userId, payment.getOrder());

        // 결제 당시 적립된 포인트를 취소하고 누적 결제 금액과 멤버십 등급을 다시 계산
        pointService.cancelEarnedPoints(userId, payment.getOrder(), payment.getExternalAmount());

        // 주문 생성 시 차감했던 재고를 환불 시점에 복구
        restoreOrderStock(payment);
    }

    // 환불된 주문의 주문 상품 수량만큼 각 상품 재고를 되돌림
    private void restoreOrderStock(Payment payment) {
        for (OrderItem orderItem : orderItemRepository.findWithProductByOrder(payment.getOrder())) {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
        }
    }
}

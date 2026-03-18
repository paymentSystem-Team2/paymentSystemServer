package sparta.paymentsystemserver.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sparta.paymentsystemserver.domain.order.entity.Order;
import sparta.paymentsystemserver.domain.order.entity.OrderStatus;
import sparta.paymentsystemserver.domain.order.repository.OrderRepository;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentRequest;
import sparta.paymentsystemserver.domain.payment.dto.CreatePaymentResponse;
import sparta.paymentsystemserver.domain.payment.entity.Payment;
import sparta.paymentsystemserver.domain.payment.entity.PaymentProvider;
import sparta.paymentsystemserver.domain.payment.exception.PaymentException;
import sparta.paymentsystemserver.domain.payment.repository.PaymentRepository;
import sparta.paymentsystemserver.domain.user.entity.User;
import sparta.paymentsystemserver.domain.user.repository.UserRepository;
import sparta.paymentsystemserver.global.exception.ErrorCode;
import sparta.paymentsystemserver.global.util.PublicIdGenerator;


// 결제 시도 생성과 결제 확정을 담당하는 서비스입니다
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PublicIdGenerator publicIdGenerator;

    // 결제 시도 생성 메서드
    // 이 단계에서는 실제 결제를 확정하지 않고, 프론트가 포트원 결제창을 열기 전에 서버에 READY 상태 결제를 미리 저장하는 용도
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new PaymentException(ErrorCode.ORDER_NOT_FOUND));

        // 로그인한 사용자의 주문이 아니면 접근 불가
        if (!order.getUser().getId().equals(userId)) {
            throw new PaymentException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // 결제 대기 상태가 아닌 주문은 결제 생성 불가
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new PaymentException(ErrorCode.ORDER_NOT_PAYABLE);
        }

        // 프론트 금액과 서버 주문 금액 비교
        if (!order.getTotalAmount().equals(request.totalAmount())) {
            throw new PaymentException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }

        // 사용 포인트 기본 검증
        if (request.pointsToUse() < 0 || request.pointsToUse() > order.getTotalAmount()) {
            throw new PaymentException(ErrorCode.INVALID_POINTS_TO_USE);
        }

        // 보유 포인트보다 많이 사용하려는 경우
        if (request.pointsToUse() > user.getPointBalance()) {
            throw new PaymentException(ErrorCode.INSUFFICIENT_POINTS);
        }

        long externalAmount = order.getTotalAmount() - request.pointsToUse();

        PaymentProvider provider = externalAmount == 0 ? PaymentProvider.INTERNAL : PaymentProvider.PORTONE;

        String paymentId = publicIdGenerator.generate("PAY");

        Payment payment = Payment.ready(
                paymentId,
                order,
                user,
                provider,
                order.getTotalAmount(),
                request.pointsToUse(),
                externalAmount,
                "KRW"
        );

        paymentRepository.save(payment);

        return new CreatePaymentResponse(
                payment.getPaymentId(),
                order.getOrderId(),
                payment.getTotalAmount(),
                payment.getPointsToUse(),
                payment.getExternalAmount(),
                payment.getStatus().name()
        );
    }

}
